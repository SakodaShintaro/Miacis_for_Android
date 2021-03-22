package com.example.miacisshogi

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.pytorchmobile.ImageNetClasses
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
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

        /// モデルと画像をロード
        val bitmap = BitmapFactory.decodeStream(assets.open("image.jpg"))
        val module = Module.load(assetFilePath(this, "resnet.pt"))

        /// 推論
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        /// 結果
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        /// scoreを格納する変数
        var maxScore: Float = 0F
        var maxScoreIdx = -1
        var maxSecondScore: Float = 0F
        var maxSecondScoreIdx = -1

        /// scoreが高いものを上から2個とる
        for (i in scores.indices) {
            if (scores[i] > maxScore) {
                maxSecondScore = maxScore
                maxSecondScoreIdx = maxScoreIdx
                maxScore = scores[i]
                maxScoreIdx = i
            }
        }

        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setImageBitmap(bitmap)

        ///　インデックスから分類したクラス名を取得
        val className = ImageNetClasses().IMAGENET_CLASSES[maxScoreIdx]
        val className2 = ImageNetClasses().IMAGENET_CLASSES[maxSecondScoreIdx]

        val result1Score = findViewById<TextView>(R.id.result1Score)
        val result1Class = findViewById<TextView>(R.id.result1Class)
        val result2Score = findViewById<TextView>(R.id.result2Score)
        val result2Class = findViewById<TextView>(R.id.result2Class)
        result1Score.text = "score: $maxScore"
        result1Class.text = "分類結果:$className"
        result2Score.text = "score:$maxSecondScore"
        result2Class.text = "分類結果:$className2"
    }
}