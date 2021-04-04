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
    var result = Array(BIN_SIZE) { 0.0f }
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
    var table_ = Array(0) { HashEntry() }
    var used_num_ = 0
    var age_ = 0

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
    var hash_table_ = HashTable()
    val draw_turn = 10
    val shape = longArrayOf(1, 42, 9, 9)
    val C_PUCT = 2.5f
    lateinit var policy: Array<Float>
    lateinit var value: Array<Float>

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

        // Policy, Valueを仮に初期化
        policy = Array(POLICY_DIM) { 0.0f }
        value = Array(BIN_SIZE) { 0.0f }
    }

    fun search(pos: Position): Move {
        // 入力のshape
        val feature = pos.makeFeature()
        val tensor = Tensor.fromBlob(feature.toFloatArray(), shape)

        // 結果
        val output = module.forward(IValue.from(tensor))
        val tuple = output.toTuple()
        val rawPolicy = tuple[0].toTensor().dataAsFloatArray
        val rawValue = tuple[1].toTensor().dataAsFloatArray

        // 合法手だけマスク
        val moveList = pos.generateAllMoves()
        policy = Array(moveList.size) { it -> rawPolicy[moveList[it].toLabel()] }
        policy = softmax(policy, 1.0f)

        // valueを取得
        value = Array(BIN_SIZE) { 0.0f }
        for (i in 0 until BIN_SIZE) {
            value[i] = rawValue[i]
        }
        value = softmax(value, 1.0f)

        if (pos.turnNumber < randomTurn) {
            val index = randomChoose(policy)
            return moveList[index]
        } else {
            // 最も確率が高いものを取得する
            var maxScore = -10000.0f
            var bestMove = NULL_MOVE
            for (move in moveList) {
                if (rawPolicy[move.toLabel()] > maxScore) {
                    maxScore = rawPolicy[move.toLabel()]
                    bestMove = move
                }
            }

            return bestMove
        }
    }

    fun think(root: Position): Move {
        //制限の設定
        val node_limit_ = 10

        //古いハッシュを削除
        hash_table_.deleteOldHash(root, true)

        //キューの初期化:TODO

        //ルートノードの展開
        hash_table_.root_index = expand(root)
        val curr_node = hash_table_[hash_table_.root_index]

        //合法手が0だったら投了
        if (curr_node.moves.isEmpty()) {
            return NULL_MOVE
        }

        //探索を実行: TODO

        //行動選択
        val temperature = 0
        if (root.turnNumber <= randomTurn) {
            var distribution = Array(curr_node.moves.size) { 0.0f }
            if (temperature == 0) {
                //探索回数を正規化した分布に従って行動選択
                for (i in curr_node.moves.indices) {
                    distribution[i] = curr_node.N[i].toFloat() / curr_node.sum_N
                }
            } else {
                //価値のソフトマックス分布に従って行動選択
                val Q = Array(curr_node.moves.size) { 0.0f }
                for (i in curr_node.moves.indices) {
                    Q[i] = hash_table_.expQfromNext(curr_node, i)
                }
                distribution = softmax(Q, temperature / 1000.0f)
            }

            return curr_node.moves[randomChoose(distribution)]
        } else {
            //探索回数最大の手を選択
            var bestIndex = -1
            var bestNum = 0
            for (i in curr_node.N.indices) {
                if (curr_node.N[i] > bestNum) {
                    bestIndex = i
                    bestNum = curr_node.N[i]
                }
            }
            return curr_node.moves[bestIndex]
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
        val curr_indices = Stack<Int>()
        val curr_actions = Stack<Int>()

        var index = hash_table_.root_index

        //未展開の局面に至るまで遷移を繰り返す
        while (index != NOT_EXPANDED) {
            if (pos.turnNumber > draw_turn) {
                //手数が制限まで達している場合,抜ける
                break
            }

            if (index != hash_table_.root_index && pos.getFinishStatus() == 0) {
                //繰り返しが発生している場合も抜ける
                break
            }

            //状態を記録
            curr_indices.push(index)

            //選択
            val action = selectMaxUcbChild(hash_table_[index])

            //取った行動を記録
            curr_actions.push(action)

            //遷移
            pos.doMove(hash_table_[index].moves[action])

            //index更新
            index = hash_table_[index].child_indices[action]
        }

        if (curr_indices.empty()) {
            println("curr_indices.empty")
        }

        //expandNode内でこれらの情報は壊れる可能性があるので保存しておく
        index = curr_indices.peek()
        val action = curr_actions.peek()
        val move_num = curr_actions.size

        //今の局面を展開
        val leaf_index = expand(pos)
        if (leaf_index == -1) {
            //置換表に空きがなかった場合こうなる
            //ここには来ないように制御しているはずだが、現状ときどき来ているっぽい
            //別に止める必要はないので進行
        } else {
            //葉の直前ノードを更新
            hash_table_[index].child_indices[action] = leaf_index
        }

        //局面を戻す
        repeat(move_num) {
            pos.undo()
        }

        //バックアップ
        val leaf = leaf_index
        val value = hash_table_[leaf].value

        //バックアップ
        while (!curr_actions.empty()) {
            val index = curr_indices.peek()
            curr_indices.pop()

            val action = curr_actions.peek()
            curr_actions.pop()

            //手番が変わるので反転
            value.reverse()

            // 探索結果の反映
            val node = hash_table_[index]
            //探索回数の更新
            node.N[action]++
            node.sum_N++

            //価値の更新
            val curr_v = node.value
            val alpha = 1.0f / (node.sum_N + 1)
            for (i in 0 until BIN_SIZE) {
                node.value[i] += alpha * (value[i] - curr_v[i])
            }
        }
    }

    private fun selectMaxUcbChild(node: HashEntry): Int {
        var best_num = -1
        var best_index = -1
        for (i in 0 until node.moves.size) {
            if (node.N[i] > best_num) {
                best_num = node.N[i]
                best_index = i
            }
        }
        val best_value = expOfValueDist(hash_table_.QfromNextValue(node, best_index))
        val best_value_index = min(valueToIndex(best_value) + 1, BIN_SIZE - 1)
        val reversed_best_value_index = BIN_SIZE - best_value_index
        var max_index = -1
        var max_value = -100000f

        val sum = node.sum_N

        for (i in node.moves.indices) {
            val U = sqrt((sum + 1).toFloat()) / (node.N[i] + 1)
            var P = 0.0f
            if (node.child_indices[i] == NOT_EXPANDED) {
                P = 0.0f
            } else {
                for (j in 0 until reversed_best_value_index) {
                    P += hash_table_[node.child_indices[i]].value[j]
                }
            }
            val ucb = C_PUCT * node.nn_policy[i] * U + P
            if (ucb > max_value) {
                max_value = ucb
                max_index = i
            }
        }
        return max_index
    }

    private fun expand(pos: Position): Int {
        var index = hash_table_.findSameHashIndex(pos)

        //合流先が検知できればそれを返す
        if (index != hash_table_.size()) {
            return index
        }

        //空のインデックスを探す
        index = hash_table_.searchEmptyIndex(pos)

        //空のインデックスが見つからなかった
        if (index == hash_table_.size()) {
            return -1
        }

        //ノードを取得
        val curr_node = hash_table_[index]

        // 候補手の展開
        curr_node.moves = pos.generateAllMoves()
        curr_node.child_indices = Array(curr_node.moves.size) { NOT_EXPANDED }
        curr_node.N = Array(curr_node.moves.size) { 0 }
        curr_node.sum_N = 0
        curr_node.evaled = false
        curr_node.nn_policy
        curr_node.value

        //ノードを評価
        if (pos.getFinishStatus() == 1 || pos.turnNumber > draw_turn) {
            curr_node.value = Array(BIN_SIZE) { 0.0f }
            //TODO:どこかonethotで立てる
            curr_node.evaled = true
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
            curr_node.nn_policy = Array(curr_node.moves.size) { 0.0f }
            for (i in curr_node.nn_policy.indices) {
                curr_node.nn_policy[i] = scores[curr_node.moves[i].toLabel()]
            }
            curr_node.nn_policy = softmax(curr_node.nn_policy, 1.0f)

            curr_node.value = Array(BIN_SIZE) { 0.0f }
            for (i in curr_node.value.indices) {
                curr_node.value[i] = value.dataAsFloatArray[i]
            }

            //Softmaxが必要じゃね: TODO
            curr_node.evaled = true
        }

        return index
    }
}