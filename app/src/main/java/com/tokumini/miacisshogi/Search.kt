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
    var searchNum = Array(0) { 0 }
    var sumOfSearchNum = 0
    var childIndices = Array(0) { 0 }
    var policy = Array(0) { 0.0f }
    var value = Array(0) { 0.0f }
    var age = 0
    var hash = 0.toLong()
    var turnNumber = 0
}

class HashTable {
    var rootIndex: Int = 0
    var table = Array(256) { HashEntry() }
    var usedNum = 0
    var age = 0

    fun rootEntry(): HashEntry {
        return table[rootIndex]
    }

    private fun saveUsedHash(pos: Position, index: Int) {
        //エントリの世代を合わせれば情報を持ち越すことができる
        table[index].age = age
        usedNum++

        //再帰的に子ノードを探索していく
        val currNode = table[index]

        val childIndices = currNode.childIndices
        for (i in currNode.moves.indices) {
            if (childIndices[i] != NOT_EXPANDED && table[childIndices[i]].age != age) {
                pos.doMove(currNode.moves[i])
                saveUsedHash(pos, childIndices[i])
                pos.undo()
            }
        }
    }

    fun deleteOldHash(root: Position) {
        //次のルート局面に相当するノード以下の部分木だけを残すためにインデックスを取得
        val nextRootIndex = findSameHashIndex(root)

        //置換表全体を消去
        usedNum = 0
        age++

        if (nextRootIndex == table.size) {
            //そもそも存在しないならここで終了
            return
        }

        //ルート以下を再帰的にsave
        saveUsedHash(root, nextRootIndex)
    }

    operator fun get(index: Int): HashEntry {
        return table[index]
    }

    //entryにおけるi番目の指し手についてのvalue分布を返す
    fun valueDistribution(node: HashEntry, i: Int): Array<Float> {
        //展開されていない場合は基本的にMIN_SCOREで扱うが、探索回数が0でないときは詰み探索がそこへ詰みありと言っているということなのでMAX_SCOREを返す
        if (node.childIndices[i] == NOT_EXPANDED) {
            return if (node.searchNum[i] == 0) onehotDist(MIN_SCORE) else onehotDist(MAX_SCORE)
        }
        val v = table[node.childIndices[i]].value.clone()
        v.reverse()
        return v
    }

    fun valueExpectation(node: HashEntry, i: Int): Float {
        return expOfValueDist(valueDistribution(node, i))
    }

    fun findSameHashIndex(pos: Position): Int {
        val hash = pos.hashValue
        val key = hashToIndex(hash)
        var i = key
        while (true) {
            if (table[i].age != age) {
                //根本的に世代が異なるなら同じものはないのでsize()を返す
                return table.size
            } else if (table[i].hash == hash && table[i].turnNumber == pos.turnNumber) {
                //完全に一致したのでここが記録されていたエントリ
                return i
            }

            i++
            if (i >= table.size) {
                i = 0
            }
            if (i == key) {
                return table.size
            }
        }
    }

    fun size(): Int {
        return table.size
    }

    fun searchEmptyIndex(pos: Position): Int {
        val hash = pos.hashValue
        val key = hashToIndex(hash)
        var i = key
        while (true) {
            if (table[i].age != age) {
                //世代が違うならここを上書きして良い
                table[i].hash = hash
                table[i].turnNumber = pos.turnNumber
                table[i].age = age
                usedNum++
                return i
            }

            i++

            //たぶんmodを取るより分岐の方が速かったはず
            if (i >= table.size) {
                i = 0
            }

            //一周したのなら空きがなかったということなのでsize()を返す
            if (i == key) {
                return table.size
            }
        }
    }

    //局面のハッシュ値から置換表におけるキーへ変換する。この処理が最適かは不明
    private fun hashToIndex(hash: Long): Int {
        return (hash and -0x1 xor (hash shr 32 and -0x1) and (table.size - 1).toLong()).toInt()
    }
}

class Search(context: Context, private val randomTurn: Int) {
    private val module: Module
    var hashTable = HashTable()
    private val drawTurn = 512
    private val shape = longArrayOf(1, 42, 9, 9)
    private val C_PUCT = 2.5f
    var policy: Array<Float> = Array(POLICY_DIM) { 0.0f }
    var value: Array<Float> = Array(BIN_SIZE) { 0.0f }

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

    fun search(root: Position, searchNum: Int): Move {
        // 推論
        //古いハッシュを削除
        hashTable.deleteOldHash(root)

        //ルートノードの展開
        hashTable.rootIndex = expand(root)
        val rootEntry = hashTable.rootEntry()

        //合法手が0だったら投了
        if (rootEntry.moves.isEmpty()) {
            return NULL_MOVE
        }

        //探索を実行
        repeat(searchNum) {
            oneStepSearch(root)
        }

        value = rootEntry.value
        policy = rootEntry.policy

        //行動選択
        if (searchNum == 0) {
            //Policyをもとに選択
            if (root.turnNumber <= randomTurn) {
                val index = randomChoose(policy)
                return rootEntry.moves[index]
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
                return bestMove
            }
        } else {
            //探索結果をもとに選択
            val temperature = 0
            if (root.turnNumber <= randomTurn) {
                val distribution = if (temperature == 0) {
                    //探索回数を正規化した分布に従って行動選択
                    Array(rootEntry.moves.size) { rootEntry.searchNum[it].toFloat() / rootEntry.sumOfSearchNum }
                } else {
                    //価値のソフトマックス分布に従って行動選択
                    val q = Array(rootEntry.moves.size) { hashTable.valueExpectation(rootEntry, it) }
                    softmax(q, temperature / 1000.0f)
                }

                return rootEntry.moves[randomChoose(distribution)]
            } else {
                //探索回数最大の手を選択
                var bestIndex = -1
                var bestNum = 0
                for (i in rootEntry.searchNum.indices) {
                    if (rootEntry.searchNum[i] > bestNum) {
                        bestIndex = i
                        bestNum = rootEntry.searchNum[i]
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

        var index = hashTable.rootIndex

        //未展開の局面に至るまで遷移を繰り返す
        while (index != NOT_EXPANDED) {
            if (pos.turnNumber > drawTurn) {
                //手数が制限まで達している場合,抜ける
                break
            }

            if (index != hashTable.rootIndex && pos.getFinishStatus() != Position.NOT_FINISHED) {
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
            index = hashTable[index].childIndices[action]
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
            hashTable[index].childIndices[action] = leafIndex
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
            node.searchNum[currAction]++
            node.sumOfSearchNum++

            //価値の更新
            val currV = node.value
            val alpha = 1.0f / (node.sumOfSearchNum + 1)
            for (i in 0 until BIN_SIZE) {
                node.value[i] += alpha * (value[i] - currV[i])
            }
        }
    }

    private fun selectMaxUcbChild(node: HashEntry): Int {
        var bestNum = -1
        var bestIndex = -1
        for (i in 0 until node.moves.size) {
            if (node.searchNum[i] > bestNum) {
                bestNum = node.searchNum[i]
                bestIndex = i
            }
        }
        val bestValue = expOfValueDist(hashTable.valueDistribution(node, bestIndex))
        val bestValueIndex = min(valueToIndex(bestValue) + 1, BIN_SIZE - 1)
        val reversedBestValueIndex = BIN_SIZE - bestValueIndex
        var maxIndex = -1
        var maxValue = -100000f

        val sum = node.sumOfSearchNum

        for (i in node.moves.indices) {
            val u = sqrt((sum + 1).toFloat()) / (node.searchNum[i] + 1)
            var p = 0.0f
            if (node.childIndices[i] == NOT_EXPANDED) {
                p = 0.0f
            } else {
                for (j in 0 until reversedBestValueIndex) {
                    p += hashTable[node.childIndices[i]].value[j]
                }
            }
            val ucb = C_PUCT * node.policy[i] * u + p
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
        currNode.childIndices = Array(currNode.moves.size) { NOT_EXPANDED }
        currNode.searchNum = Array(currNode.moves.size) { 0 }
        currNode.sumOfSearchNum = 0

        //ノードを評価
        if (pos.getFinishStatus() != Position.NOT_FINISHED || pos.turnNumber > drawTurn) {
            currNode.value = when (pos.getFinishStatus()) {
                Position.WIN -> onehotDist(MAX_SCORE)
                Position.DRAW -> onehotDist((MAX_SCORE + MIN_SCORE) / 2)
                Position.LOSE -> onehotDist(MIN_SCORE)
                else -> onehotDist(-1.0f)
            }
        } else {
            //計算
            val thisFeature = pos.makeFeature()
            val tensor = Tensor.fromBlob(thisFeature.toFloatArray(), shape)
            val output = module.forward(IValue.from(tensor))
            val tuple = output.toTuple()
            val policyLogit = tuple[0].toTensor().dataAsFloatArray
            val valueLogit = tuple[1].toTensor().dataAsFloatArray

            //ルートノードへ書き込み
            currNode.policy = Array(currNode.moves.size) { policyLogit[currNode.moves[it].toLabel()] }
            currNode.policy = softmax(currNode.policy, 1.0f)

            currNode.value = Array(BIN_SIZE) { valueLogit[it] }
            currNode.value = softmax(currNode.value, 1.0f)
        }

        return index
    }
}