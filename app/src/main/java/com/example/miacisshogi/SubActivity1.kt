package com.example.miacisshogi

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class SubActivity1 : AppCompatActivity() {
    lateinit var imageViews: ArrayList<ImageView>
    lateinit var piece: ArrayList<Int>
    private val marginRate = 0.05

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub1)

        //初期化
        imageViews = ArrayList<ImageView>()
        piece = ArrayList<Int>()

        //ここで9 x 9のImageViewを作り、置かれている駒に応じて適切な画像を選択して置く
        val frame = findViewById<FrameLayout>(R.id.frame)

        for (i in 0..8) {
            for (j in 0..8) {
                val imageView = ImageView(this)
                imageView.setImageResource(R.drawable.sgl01)
                imageView.x = (j).toFloat()
                imageView.y = (i).toFloat()
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                frame.addView(imageView, 0, 0)

                frame.viewTreeObserver.addOnGlobalLayoutListener {
                    val params: ViewGroup.LayoutParams = imageView.layoutParams
                    val xOffset = frame.width * marginRate
                    val boardWidth = frame.width - 2 * xOffset
                    val boardHeight = boardWidth * 1.07
                    val squareWidth = boardWidth / 9
                    val squareHeight = boardHeight / 9
                    val yOffset = frame.height / 2 - boardHeight * 0.5

                    params.width = (squareWidth).toInt()
                    params.height = (squareHeight).toInt()
                    imageView.x = (xOffset + j * squareWidth).toFloat()
                    imageView.y = (yOffset + i * squareHeight).toFloat()
                    imageView.layoutParams = params
                }

                imageViews.add(imageView)
                piece.add(0)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val frame = findViewById<FrameLayout>(R.id.frame)
        val xOffset = frame.width * marginRate
        val boardWidth = frame.width - 2 * xOffset
        val boardHeight = boardWidth * 1.07
        val squareWidth = boardWidth / 9
        val squareHeight = boardHeight / 9
        val yOffset = frame.height / 2 - boardHeight * 0.45

        val pointX = event.x
        val pointY = event.y

        if (pointX < xOffset || xOffset + boardWidth < pointX ||
            pointY < yOffset || yOffset + boardHeight < pointY
        ) {
            //画面外
            Log.d("TouchEvent", "X:$pointX,Y:$pointY out of board")
        } else {
            val sqX = ((pointX - xOffset) / squareWidth).toInt()
            val sqY = ((pointY - yOffset) / squareHeight).toInt()
            val sq = sqY * 9 + sqX
            piece[sq]++
            piece[sq] %= 58

            imageViews[sq].setImageResource(
                this.resources.getIdentifier(
                    "sgl" + (piece[sq] + 1).toString(),
                    "drawable",
                    "com.example.miacisshogi"
                )
            )

            Log.d("TouchEvent", "X:$pointX,Y:$pointY sqX:$sqX, sqY:$sqY piece[sq]:${piece[sq]}")
        }

        return true
    }
}