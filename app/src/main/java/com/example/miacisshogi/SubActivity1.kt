package com.example.miacisshogi

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class SubActivity1 : AppCompatActivity() {
    lateinit var imageViews: ArrayList<ImageView>
    lateinit var piece: ArrayList<Int>
    lateinit var pos: Position
    private val marginRate = 0.05

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub1)

        //初期化
        imageViews = ArrayList<ImageView>()
        piece = ArrayList<Int>()
        pos = Position()

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
                    val piece = pos.on(SquareList[j * BOARD_WIDTH + i])
                    imageView.setImageResource(piece2resourceID(piece))
                    imageView.visibility = if (piece == EMPTY) View.INVISIBLE else View.VISIBLE
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

    fun piece2resourceID(piece: Int): Int {
        return when(piece) {
            BLACK_PAWN -> R.drawable.sgl08
            BLACK_LANCE -> R.drawable.sgl07
            BLACK_KNIGHT -> R.drawable.sgl06
            BLACK_SILVER -> R.drawable.sgl05
            BLACK_GOLD -> R.drawable.sgl04
            BLACK_BISHOP -> R.drawable.sgl03
            BLACK_ROOK -> R.drawable.sgl02
            BLACK_KING -> R.drawable.sgl11
            BLACK_PAWN_PROMOTE -> R.drawable.sgl28
            BLACK_LANCE_PROMOTE -> R.drawable.sgl27
            BLACK_KNIGHT_PROMOTE -> R.drawable.sgl26
            BLACK_SILVER_PROMOTE -> R.drawable.sgl25
            BLACK_BISHOP_PROMOTE -> R.drawable.sgl23
            BLACK_ROOK_PROMOTE -> R.drawable.sgl22
            WHITE_PAWN -> R.drawable.sgl38
            WHITE_LANCE -> R.drawable.sgl37
            WHITE_KNIGHT -> R.drawable.sgl36
            WHITE_SILVER -> R.drawable.sgl35
            WHITE_GOLD -> R.drawable.sgl34
            WHITE_BISHOP -> R.drawable.sgl33
            WHITE_ROOK -> R.drawable.sgl32
            WHITE_KING -> R.drawable.sgl31
            WHITE_PAWN_PROMOTE -> R.drawable.sgl58
            WHITE_LANCE_PROMOTE -> R.drawable.sgl57
            WHITE_KNIGHT_PROMOTE -> R.drawable.sgl56
            WHITE_SILVER_PROMOTE -> R.drawable.sgl55
            WHITE_BISHOP_PROMOTE -> R.drawable.sgl53
            WHITE_ROOK_PROMOTE -> R.drawable.sgl51

            else -> R.drawable.sgl18
        }
    }
}