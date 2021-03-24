package com.example.miacisshogi

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class SubActivity1 : AppCompatActivity() {
    private lateinit var squareImageViews: ArrayList<ImageView>
    private lateinit var handImageViews: ArrayList<ArrayList<ImageView>>
    private lateinit var handTextViews: ArrayList<ArrayList<TextView>>
    private lateinit var pos: Position
    private var holdPiece: Int = EMPTY
    private var moveFrom: Square = Square.WALLAA
    private val marginRate = 0.05

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub1)

        //盤面の準備
        pos = Position()

        //マス画像の初期化
        //ここで9 x 9のImageViewを作り、置かれている駒に応じて適切な画像を選択して置く
        squareImageViews = ArrayList()
        val frame = findViewById<FrameLayout>(R.id.frame)
        for (i in 0..8) {
            for (j in 0..8) {
                val imageView = ImageView(this)
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

                val piece = pos.on(xy2square(j, i))
                imageView.setImageResource(piece2resourceID(piece))
                squareImageViews.add(imageView)
            }
        }

        //手駒画像の初期化
        val handFrameList =
            arrayListOf<FrameLayout>(findViewById(R.id.frame_hand_down), findViewById(R.id.frame_hand_up))
        handImageViews = ArrayList()
        handImageViews.add(ArrayList())
        handImageViews.add(ArrayList())
        handTextViews = ArrayList()
        handTextViews.add(ArrayList())
        handTextViews.add(ArrayList())

        for (c in BLACK..WHITE) {
            println(c)
            val handFrame = handFrameList[c]
            for (p in PAWN until KING) {
                val imageView = ImageView(this)
                imageView.x = 0.0f
                imageView.y = 0.0f
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                handFrame.addView(imageView, 0, 0)

                val textView = TextView(this)
                textView.x = 0.0f
                textView.y = 0.0f
                textView.text = "x0"
                textView.textSize = 20.0f
                handFrame.addView(textView, 200, 200)

                handFrame.viewTreeObserver.addOnGlobalLayoutListener {
                    val frameWidth = handFrame.width
                    val width = (frameWidth / ROOK * 0.75).toInt()
                    val params: ViewGroup.LayoutParams = imageView.layoutParams
                    params.width = width
                    params.height = handFrame.height
                    imageView.x = ((p - 1) * frameWidth / ROOK).toFloat()
                    imageView.layoutParams = params

                    textView.x = (imageView.x + width * 0.85).toFloat()
                    textView.y = if (c == BLACK) 0.0f else params.height - textView.textSize * 1.5f
                    textView.bringToFront()
                }
                imageView.setImageResource(piece2resourceID(coloredPiece(c, p)))
                println("${coloredPiece(c, p)} ${piece2resourceID(coloredPiece(c, p))}")
                handImageViews[c].add(imageView)
                handTextViews[c].add(textView)
            }
        }

        showHand()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val frame = findViewById<FrameLayout>(R.id.frame)
        val xOffset = frame.width * marginRate
        val boardWidth = frame.width - 2 * xOffset
        val boardHeight = boardWidth * 1.07
        val squareWidth = boardWidth / 9
        val squareHeight = boardHeight / 9
        val yOffset = frame.height / 2 - boardHeight * 0.45 + frame.y

        val upHandFrame = findViewById<FrameLayout>(R.id.frame_hand_up)
        val downHandFrame = findViewById<FrameLayout>(R.id.frame_hand_down)
        val heightRate = 1.5

        println("${upHandFrame.y} ${upHandFrame.height} ${downHandFrame.y} ${downHandFrame.height}")

        //単純な押下以外はとりあえず無視
        if (event.action != ACTION_DOWN) {
            return true
        }

        val pointX = event.x
        val pointY = event.y
        Log.d("TouchEvent", "X:$pointX,Y:$pointY")

        if (upHandFrame.y <= pointY && pointY <= upHandFrame.y + upHandFrame.height * heightRate) {
            //上側(後手)の駒台
            //先手で触っていたら無視
            if (pos.color() == BLACK) {
                return true
            }

            for (i in 0 until ROOK) {
                if (pos.hand_[WHITE].num(i + 1) == 0) {
                    continue
                }
                if (handImageViews[WHITE][i].x <= pointX && pointX <= handImageViews[WHITE][i].x + handImageViews[WHITE][i].width) {
                    //iが押された
                    holdPiece = coloredPiece(WHITE, i + 1)
                    moveFrom = Square.WALL00
                    Log.d("TouchEvent", "catch to drop ${holdPiece} ${moveFrom}")
                    return true
                }
            }
        } else if (downHandFrame.y <= pointY && pointY <= downHandFrame.y + downHandFrame.height * heightRate) {
            //下側(先手)の駒台
            //先手で触っていたら無視
            if (pos.color() == WHITE) {
                return true
            }

            for (i in 0 until ROOK) {
                if (pos.hand_[BLACK].num(i + 1) == 0) {
                    continue
                }
                if (handImageViews[BLACK][i].x <= pointX && pointX <= handImageViews[BLACK][i].x + handImageViews[BLACK][i].width) {
                    //iが押された
                    holdPiece = coloredPiece(BLACK, i + 1)
                    moveFrom = Square.WALL00
                    Log.d("TouchEvent", "catch to drop ${holdPiece} ${moveFrom}")
                    return true
                }
            }
        } else if (pointX < xOffset || xOffset + boardWidth < pointX ||
            pointY < yOffset || yOffset + boardHeight < pointY) {
            //画面外
        } else {
            //盤の中
            val sqX = ((pointX - xOffset) / squareWidth).toInt()
            val sqY = ((pointY - yOffset) / squareHeight).toInt()
            if (holdPiece != EMPTY) {
                // Moveを構成
                val to = xy2square(sqX, sqY)
                val move = if (moveFrom == Square.WALL00) {
                    dropMove(to, holdPiece)
                } else {
                    Move(to, moveFrom)
                }

                // to, fromから完全な情報を集める
                val nonPromotiveMove = pos.transformValidMove(move)

                Log.d("TouchEvent", "from   :${nonPromotiveMove.from().ordinal}")
                Log.d("TouchEvent", "to     :${nonPromotiveMove.to().ordinal}")
                Log.d("TouchEvent", "subject:${nonPromotiveMove.subject()}")
                Log.d("TouchEvent", "move   :${nonPromotiveMove.toPrettyStr()}")

                val promotiveMove = promotiveMove(nonPromotiveMove)

                val moveList = pos.generateAllMoves()
                for (m in moveList) {
                    println("move :${m.toPrettyStr()}")
                }

                val isLegalNonPromotive = pos.isLegalMove(nonPromotiveMove)
                val isLegalPromotive = pos.isLegalMove(promotiveMove)

                if (!isLegalNonPromotive && !isLegalPromotive) {
                    //両方非合法手だとダメ
                    Log.d("TouchEvent", "Illegal Move!")
                    holdPiece = EMPTY
                    return true
                } else if (!isLegalNonPromotive && isLegalPromotive) {
                    //歩、香車、桂馬は成らないと非合法であることがありえる
                    doMove(promotiveMove)
                } else if (isLegalNonPromotive && !isLegalPromotive) {
                    doMove(nonPromotiveMove)
                } else if (isLegalPromotive) {
                    // 選択が発生する
                    // BuilderからAlertDialogを作成
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("成るか成らないか") // タイトル
                        .setPositiveButton("成る") { dialog, which -> doMove(promotiveMove) }
                        .setNegativeButton("成らない") { dialog, which -> doMove(nonPromotiveMove) }
                        .setCancelable(false)
                        .create()
                    dialog.show()
                }
            } else {
                //駒を掴む
                holdPiece = pos.on(xy2square(sqX, sqY))
                moveFrom = xy2square(sqX, sqY)

                Log.d(
                    "TouchEvent",
                    "catch X:$pointX,Y:$pointY sqX:$sqX, sqY:$sqY hold_piece:${holdPiece}"
                )
            }
        }

        return true
    }

    private fun doMove(move: Move) {
        Log.d("doMove", "Move   :${move.toPrettyStr()}")

        // 局面の遷移
        pos.doMove(move)

        // 画像の更新
        // 掴んだ駒を設置する
        val (toX, toY) = square2xy(move.to())
        val sqTo = toY * BOARD_WIDTH + toX
        val piece = if (move.isPromote()) promote(move.subject()) else move.subject()
        squareImageViews[sqTo].setImageResource(piece2resourceID(piece))
        holdPiece = EMPTY

        //もとにあった位置から削除
        if (moveFrom == Square.WALL00) {
            //持ち駒から
            //持ち駒を更新
            //持ち駒を再描画
        } else {
            //盤上の駒を動かす
            val (x, y) = square2xy(moveFrom)
            squareImageViews[y * BOARD_WIDTH + x].setImageResource(piece2resourceID(EMPTY))
        }
        showHand()
    }

    private fun showHand() {
        for (c in BLACK..WHITE) {
            var str = String()
            for (p in PAWN until KING) {
                handImageViews[c][p - 1].setImageResource(piece2resourceID(coloredPiece(c, p)))
                handTextViews[c][p - 1].text = pos.hand_[c].num(p).toString()
                str += pos.hand_[c].num(p).toString()
                str += " "
            }
        }
    }

    private fun piece2resourceID(piece: Int): Int {
        return when (piece) {
            EMPTY -> R.drawable.empty
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

    private fun xy2square(x: Int, y: Int): Square {
        return SquareList[(BOARD_WIDTH - 1 - x) * BOARD_WIDTH + y]
    }

    private fun square2xy(sq: Square): Pair<Int, Int> {
        return Pair(BOARD_WIDTH - SquareToFile[sq.ordinal].ordinal, SquareToRank[sq.ordinal].ordinal - 1)
    }
}