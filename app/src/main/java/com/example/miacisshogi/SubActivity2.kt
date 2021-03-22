package com.example.miacisshogi

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream

class SubActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub2)

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
        val module = Module.load(assetFilePath(this, "shogi_cat_bl10_ch256_cpu.model"))

        // 入力のshapeは
        val shape = longArrayOf(1, 42, 9, 9)
        val arr = Array(42 * 9 * 9) { 1.0f }
        val a = arr.toFloatArray()

        // tensorのshapeを表示
        Log.d("SubActivity2", shape.joinToString(" ") { it.toString() })

        val tensor = Tensor.fromBlob(a, shape)

        // 結果
        val output = module.forward(IValue.from(tensor))
        val tuple = output.toTuple()
        val policy = tuple[0].toTensor()
        val value = tuple[1].toTensor()
        val scores = policy.dataAsFloatArray

        // scoreを格納する変数
        var maxScore: Float = 0F
        var maxScoreIdx = -1
        var maxSecondScore: Float = 0F
        var maxSecondScoreIdx = -1

        // scoreが高いものを上から2個とる
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxSecondScore = maxScore
                maxSecondScoreIdx = maxScoreIdx
                maxScore = scores[i]
                maxScoreIdx = i
            }
        }

        val result1Score = findViewById<TextView>(R.id.result1Score)
        val result1Class = findViewById<TextView>(R.id.result1Class)
        val result2Score = findViewById<TextView>(R.id.result2Score)
        val result2Class = findViewById<TextView>(R.id.result2Class)
        result1Score.text = "最大score: $maxScore"
        result1Class.text = "最大index:$maxScoreIdx"
        result2Score.text = "2番目score:$maxSecondScore"
        result2Class.text = "2番目index:$maxSecondScoreIdx"
    }
}