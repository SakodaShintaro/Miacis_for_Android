package com.example.miacisshogi

import android.content.Context
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream

class Search(context: Context) {
    private val module: Module

    init {
        //// assetファイルからパスを取得する関数
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

    fun search(pos: Position): Move {
        // 入力のshape
        val shape = longArrayOf(1, 42, 9, 9)
        val feature = pos.makeFeature()
        val tensor = Tensor.fromBlob(feature.toFloatArray(), shape)

        // 結果
        val output = module.forward(IValue.from(tensor))
        val tuple = output.toTuple()
        val policy = tuple[0].toTensor()
        val value = tuple[1].toTensor()
        val scores = policy.dataAsFloatArray

        val moveList = pos.generateAllMoves()

        // 最も確率が高いものを取得する
        var maxScore = -10000.0f
        var bestMove = NULL_MOVE
        for (move in moveList) {
            if (scores[move.toLabel()] > maxScore) {
                maxScore = scores[move.toLabel()]
                bestMove = move
            }
        }

        return bestMove
    }
}