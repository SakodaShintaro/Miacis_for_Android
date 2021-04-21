package com.tokumini.miacisshogi

import android.content.Context
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

const val NOT_EXPANDED = -1
const val MIN_SCORE = -1.0f
const val MAX_SCORE = 1.0f
const val BIN_SIZE = 51
const val VALUE_WIDTH = (MAX_SCORE - MIN_SCORE) / BIN_SIZE

fun valueToIndex(value: Float): Int {
    return min(((value - MIN_SCORE) / VALUE_WIDTH).toInt(), BIN_SIZE - 1)
}

fun onehotDist(value: Float): Array<Float> {
    val result = Array(BIN_SIZE) { 0.0f }
    result[valueToIndex(value)] = 1.0f
    return result
}

fun expOfValueDist(dist: Array<Float>): Float {
    var exp = 0.0f
    for (i in 0 until BIN_SIZE) {
        //i番目の要素が示す値はMIN_SCORE + (i + 0.5) * VALUE_WIDTH
        exp += (MIN_SCORE + (i + 0.5f) * VALUE_WIDTH) * dist[i]
    }
    return exp
}

class HashEntry {
    var moves = ArrayList<Move>()
    var N = Array(0) { 0 }
    var sum_N = 0
    var evaled = false
    var child_indices = Array(0) { 0 }
    var nn_policy = Array(0) { 0.0f }
    var value = Array(0) { 0.0f }
    var age = 0
    var hash = 0.toLong()
    var turn_number = 0
}

class HashTable {
    var root_index: Int = 0
    var table_ = Array(256) { HashEntry() }
    var used_num_ = 0
    var age_ = 0

    fun rootEntry(): HashEntry {
        return table_[root_index]
    }

    fun saveUsedHash(pos: Position, index: Int) {
        //エントリの世代を合わせれば情報を持ち越すことができる
        table_[index].age = age_
        used_num_++

        //再帰的に子ノードを探索していく
        val curr_node = table_[index]

        val child_indices = curr_node.child_indices
        for (i in curr_node.moves.indices) {
            if (child_indices[i] != NOT_EXPANDED && table_[child_indices[i]].age != age_) {
                pos.doMove(curr_node.moves[i])
                saveUsedHash(pos, child_indices[i])
                pos.undo()
            }
        }
    }

    fun deleteOldHash(root: Position, leaveRoot: Boolean) {
        //次のルート局面に相当するノード以下の部分木だけを残すためにインデックスを取得
        val next_root_index = findSameHashIndex(root)

        //置換表全体を消去
        used_num_ = 0
        age_++

        if (next_root_index == table_.size) {
            //そもそも存在しないならここで終了
            return
        }

        //ルート以下をsave
        saveUsedHash(root, next_root_index)

        //強化学習のデータ生成中ではノイズを入れる関係で次のルート局面だけは消去したいので選べるようにしてある
        if (!leaveRoot) {
            //root_indexのところは初期化
            table_[next_root_index].age = age_ - 1
            used_num_--
        }
    }

    operator fun get(index: Int): HashEntry {
        return table_[index]
    }

    fun QfromNextValue(node: HashEntry, i: Int): Array<Float> {
        //展開されていない場合は基本的にMIN_SCOREで扱うが、探索回数が0でないときは詰み探索がそこへ詰みありと言っているということなのでMAX_SCOREを返す
        if (node.child_indices[i] == NOT_EXPANDED) {
            return if (node.N[i] == 0) onehotDist(MIN_SCORE) else onehotDist(MAX_SCORE)
        }
        val v = table_[node.child_indices[i]].value.clone()
        v.reverse()
        return v
    }

    fun expQfromNext(node: HashEntry, i: Int): Float {
        return expOfValueDist(QfromNextValue(node, i))
    }

    fun findSameHashIndex(pos: Position): Int {
        val hash = pos.hashValue
        val key = hashToIndex(hash)
        var i = key
        while (true) {
            if (table_[i].age != age_) {
                //根本的に世代が異なるなら同じものはないのでsize()を返す
                return table_.size
            } else if (table_[i].hash == hash && table_[i].turn_number == pos.turnNumber) {
                //完全に一致したのでここが記録されていたエントリ
                return i
            }

            i++
            if (i >= table_.size) {
                i = 0
            }
            if (i == key) {
                return table_.size
            }
        }
    }

    fun size(): Int {
        return table_.size
    }

    fun searchEmptyIndex(pos: Position): Int {
        val hash = pos.hashValue
        val key = hashToIndex(hash)
        var i = key
        while (true) {
            if (table_[i].age != age_) {
                //世代が違うならここを上書きして良い
                table_[i].hash = hash
                table_[i].turn_number = pos.turnNumber
                table_[i].age = age_
                used_num_++
                return i
            }

            i++

            //たぶんmodを取るより分岐の方が速かったはず
            if (i >= table_.size) {
                i = 0
            }

            //一周したのなら空きがなかったということなのでsize()を返す
            if (i == key) {
                return table_.size
            }
        }
    }

    //局面のハッシュ値から置換表におけるキーへ変換する。この処理が最適かは不明
    private fun hashToIndex(hash: Long): Int {
        return (hash and -0x1 xor (hash shr 32 and -0x1) and (table_.size - 1).toLong()).toInt()
    }
}

class Search(context: Context, val randomTurn: Int) {
    private val module: Module
    var hashTable = HashTable()
    private val drawTurn = 512
    private val shape = longArrayOf(1, 42, 9, 9)
    private val C_PUCT = 2.5f
    var policy: Array<Float> = Array(POLICY_DIM) { 0.0f }
    var value: Array<Float> = Array(BIN_SIZE) { 0.0f }
    var cacheMove: Move = NULL_MOVE
    var preTurn: Int = -1
    var preHash: Long = -1
    var preSearchNum: Int = -1

    init {
        // assetファイルからパスを取得する関数
        fun assetFilePath(context: Context, assetName: String): String {
            val file = File(context.filesDir, assetName)
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                return file.absolutePath
            }
        }

        // モデルをロード
        module = Module.load(assetFilePath(context, "shogi_cat_bl10_ch256_cpu.model"))
    }

    fun search(pos: Position, searchNum: Int): Move {
        if (pos.turnNumber == preTurn && pos.hashValue == preHash && preSearchNum == searchNum) {
            return cacheMove
        }

        // キャッシュのための情報更新
        preTurn = pos.turnNumber
        preHash = pos.hashValue
        preSearchNum = searchNum

        // 合法手だけマスクしたいので取得
        val moveList = pos.generateAllMoves()

        // 合法手がなかったら投了
        if (moveList.isEmpty()) {
            value = Array(BIN_SIZE) { 0.0f }
            value[0] = 1.0f
            return NULL_MOVE
        }

        // 推論
        cacheMove = think(pos, searchNum)
        return cacheMove
    }

    private fun think(root: Position, nodeLimit: Int): Move {
        //古いハッシュを削除
        hashTable.deleteOldHash(root, true)

        //ルートノードの展開
        hashTable.root_index = expand(root)
        val rootEntry = hashTable.rootEntry()

        //合法手が0だったら投了
        if (rootEntry.moves.isEmpty()) {
            return NULL_MOVE
        }

        //探索を実行
        repeat(nodeLimit) {
            oneStepSearch(root)
        }

        value = rootEntry.value
        policy = rootEntry.nn_policy

        //行動選択
        if (nodeLimit == 0) {
            //Policyをもとに選択
            return if (root.turnNumber <= randomTurn) {
                val index = randomChoose(policy)
                rootEntry.moves[index]
            } else {
                // 最も確率が高いものを取得する
                var maxScore = -10000.0f
                var bestMove = NULL_MOVE
                for (i in rootEntry.moves.indices) {
                    if (policy[i] > maxScore) {
                        maxScore = policy[i]
                        bestMove = rootEntry.moves[i]
                    }
                }
                bestMove
            }
        } else {
            //探索結果をもとに選択
            val temperature = 0
            if (root.turnNumber <= randomTurn) {
                var distribution = Array(rootEntry.moves.size) { 0.0f }
                if (temperature == 0) {
                    //探索回数を正規化した分布に従って行動選択
                    for (i in rootEntry.moves.indices) {
                        distribution[i] = rootEntry.N[i].toFloat() / rootEntry.sum_N
                    }
                } else {
                    //価値のソフトマックス分布に従って行動選択
                    val Q = Array(rootEntry.moves.size) { 0.0f }
                    for (i in rootEntry.moves.indices) {
                        Q[i] = hashTable.expQfromNext(rootEntry, i)
                    }
                    distribution = softmax(Q, temperature / 1000.0f)
                }

                return rootEntry.moves[randomChoose(distribution)]
            } else {
                //探索回数最大の手を選択
                var bestIndex = -1
                var bestNum = 0
                for (i in rootEntry.N.indices) {
                    if (rootEntry.N[i] > bestNum) {
                        bestIndex = i
                        bestNum = rootEntry.N[i]
                    }
                }
                return rootEntry.moves[bestIndex]
            }
        }
    }

    private fun randomChoose(x: Array<Float>): Int {
        var prob = Random.nextFloat()
        for (i in x.indices) {
            prob -= x[i]
            if (prob < 0) {
                return i
            }
        }
        return x.size - 1
    }

    private fun softmax(x: Array<Float>, temperature: Float): Array<Float> {
        val maxOrNull = x.maxOrNull()
        val maxValue = maxOrNull ?: 0.0f

        var expSum = 0.0f
        for (i in x.indices) {
            x[i] = exp((x[i] - maxValue) / temperature)
            expSum += x[i]
        }
        for (i in x.indices) {
            x[i] /= expSum
        }

        return x
    }

    private fun oneStepSearch(pos: Position) {
        //選択
        val currIndices = Stack<Int>()
        val currActions = Stack<Int>()

        var index = hashTable.root_index

        //未展開の局面に至るまで遷移を繰り返す
        while (index != NOT_EXPANDED) {
            if (pos.turnNumber > drawTurn) {
                //手数が制限まで達している場合,抜ける
                break
            }

            if (index != hashTable.root_index && pos.getFinishStatus() != Position.NOT_FINISHED) {
                //繰り返しが発生している場合も抜ける
                break
            }

            //状態を記録
            currIndices.push(index)

            //選択
            val action = selectMaxUcbChild(hashTable[index])

            //取った行動を記録
            currActions.push(action)

            //遷移
            pos.doMove(hashTable[index].moves[action])

            //index更新
            index = hashTable[index].child_indices[action]
        }

        if (currIndices.empty()) {
            assert(false)
        }

        //expandNode内でこれらの情報は壊れる可能性があるので保存しておく
        index = currIndices.peek()
        val action = currActions.peek()
        val moveSize = currActions.size

        //今の局面を展開
        val leafIndex = expand(pos)
        if (leafIndex == -1) {
            //置換表に空きがなかった場合こうなる
            //ここには来ないように制御しているはずだが、現状ときどき来ているっぽい
            //別に止める必要はないので進行
        } else {
            //葉の直前ノードを更新
            hashTable[index].child_indices[action] = leafIndex
        }

        //局面を戻す
        repeat(moveSize) {
            pos.undo()
        }

        //バックアップ
        val value = hashTable[leafIndex].value.clone()

        while (!currActions.empty()) {
            val currIndex = currIndices.peek()
            currIndices.pop()

            val currAction = currActions.peek()
            currActions.pop()

            //手番が変わるので反転
            value.reverse()

            // 探索結果の反映
            val node = hashTable[currIndex]
            //探索回数の更新
            node.N[currAction]++
            node.sum_N++

            //価値の更新
            val currV = node.value
            val alpha = 1.0f / (node.sum_N + 1)
            for (i in 0 until BIN_SIZE) {
                node.value[i] += alpha * (value[i] - currV[i])
            }
        }
    }

    private fun selectMaxUcbChild(node: HashEntry): Int {
        var bestNum = -1
        var bestIndex = -1
        for (i in 0 until node.moves.size) {
            if (node.N[i] > bestNum) {
                bestNum = node.N[i]
                bestIndex = i
            }
        }
        val bestValue = expOfValueDist(hashTable.QfromNextValue(node, bestIndex))
        val bestValueIndex = min(valueToIndex(bestValue) + 1, BIN_SIZE - 1)
        val reversedBestValueIndex = BIN_SIZE - bestValueIndex
        var maxIndex = -1
        var maxValue = -100000f

        val sum = node.sum_N

        for (i in node.moves.indices) {
            val U = sqrt((sum + 1).toFloat()) / (node.N[i] + 1)
            var P = 0.0f
            if (node.child_indices[i] == NOT_EXPANDED) {
                P = 0.0f
            } else {
                for (j in 0 until reversedBestValueIndex) {
                    P += hashTable[node.child_indices[i]].value[j]
                }
            }
            val ucb = C_PUCT * node.nn_policy[i] * U + P
            if (ucb > maxValue) {
                maxValue = ucb
                maxIndex = i
            }
        }
        return maxIndex
    }

    private fun expand(pos: Position): Int {
        var index = hashTable.findSameHashIndex(pos)

        //合流先が検知できればそれを返す
        if (index != hashTable.size()) {
            return index
        }

        //空のインデックスを探す
        index = hashTable.searchEmptyIndex(pos)

        //空のインデックスが見つからなかった
        if (index == hashTable.size()) {
            return -1
        }

        //ノードを取得
        val currNode = hashTable[index]

        // 候補手の展開
        currNode.moves = pos.generateAllMoves()
        currNode.child_indices = Array(currNode.moves.size) { NOT_EXPANDED }
        currNode.N = Array(currNode.moves.size) { 0 }
        currNode.sum_N = 0
        currNode.evaled = false
        currNode.nn_policy
        currNode.value

        //ノードを評価
        if (pos.getFinishStatus() != Position.NOT_FINISHED || pos.turnNumber > drawTurn) {
            currNode.value = when (pos.getFinishStatus()) {
                Position.WIN -> onehotDist(MAX_SCORE)
                Position.DRAW -> onehotDist((MAX_SCORE + MIN_SCORE) / 2)
                Position.LOSE -> onehotDist(MIN_SCORE)
                else -> onehotDist(-1.0f)
            }
            currNode.evaled = true
        } else {
            //計算
            val thisFeature = pos.makeFeature()
            val tensor = Tensor.fromBlob(thisFeature.toFloatArray(), shape)
            val output = module.forward(IValue.from(tensor))
            val tuple = output.toTuple()
            val policy = tuple[0].toTensor()
            val value = tuple[1].toTensor()
            val scores = policy.dataAsFloatArray

            //ルートノードへ書き込み
            currNode.nn_policy = Array(currNode.moves.size) { 0.0f }
            for (i in currNode.nn_policy.indices) {
                currNode.nn_policy[i] = scores[currNode.moves[i].toLabel()]
            }
            currNode.nn_policy = softmax(currNode.nn_policy, 1.0f)

            currNode.value = Array(BIN_SIZE) { 0.0f }
            for (i in currNode.value.indices) {
                currNode.value[i] = value.dataAsFloatArray[i]
            }

            currNode.value = softmax(currNode.value, 1.0f)
            currNode.evaled = true
        }

        return index
    }
}