package com.example.miacisshogi

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

const val HUMAN = 0
const val MIACIS = 1

class SubActivity1 : AppCompatActivity() {
    private lateinit var squareImageViews: ArrayList<ImageView>
    private lateinit var handImageViews: Array<ArrayList<ImageView>>
    private lateinit var handTextViews: Array<ArrayList<TextView>>
    private lateinit var pos: Position
    private var holdPiece: Int = EMPTY
    private var moveFrom: Square = Square.WALLAA
    private val marginRate = 0.05
    private val backGroundHoldColor = Color.rgb(0, 255, 0)
    private val backGroundMovedColor = Color.rgb(255, 128, 0)
    private val backGroundTransparent = 0x00000000
    private lateinit var searcher: Search
    private lateinit var player: Array<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub1)

        //盤面の準備
        pos = Position()

        //探索部の準備
        searcher = Search(this)

        //ターンの制御
        println("intent = ${intent?.extras?.get(TURN_STR)}")
        when (intent?.extras?.get(TURN_STR)) {
            HUMAN_TURN_BLACK -> player = arrayOf(HUMAN, MIACIS)
            HUMAN_TURN_WHITE -> player = arrayOf(MIACIS, HUMAN)
            CONSIDERATION -> player = arrayOf(HUMAN, HUMAN)
        }

        //マス画像の初期化
        //ここで9 x 9のImageViewを作り、置かれている駒に応じて適切な画像を選択して置く
        squareImageViews = ArrayList()
        val frame = findViewById<FrameLayout>(R.id.frame)
        for (i in 0..8) {
            for (j in 0..8) {
                val imageView = ImageView(this)
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                frame.addView(imageView, 0, 0)

                frame.viewTreeObserver.addOnGlobalLayoutListener {
                    val xOffset = frame.width * marginRate
                    val boardWidth = frame.width - 2 * xOffset
                    val boardHeight = boardWidth * 1.07
                    val squareWidth = boardWidth / 9
                    val squareHeight = boardHeight / 9
                    val yOffset = frame.height / 2 - boardHeight * 0.5

                    imageView.x = (xOffset + j * squareWidth).toFloat()
                    imageView.y = (yOffset + i * squareHeight).toFloat()
                    imageView.layoutParams.width = (squareWidth).toInt()
                    imageView.layoutParams.height = (squareHeight).toInt()
                }
                squareImageViews.add(imageView)
            }
        }

        //手駒画像の初期化
        val handFrameList = arrayOf<FrameLayout>(findViewById(R.id.frame_hand_down), findViewById(R.id.frame_hand_up))
        handImageViews = Array(ColorNum) { ArrayList() }
        handTextViews = Array(ColorNum) { ArrayList() }

        for (c in BLACK..WHITE) {
            val handFrame = handFrameList[c]
            for (p in PAWN until KING) {
                val imageView = ImageView(this)
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                handFrame.addView(imageView, 0, 0)

                val textView = TextView(this)
                textView.textSize = 20.0f
                handFrame.addView(textView, 200, 200)

                handFrame.viewTreeObserver.addOnGlobalLayoutListener {
                    val frameWidth = handFrame.width
                    val width = (frameWidth / ROOK * 0.75).toInt()
                    imageView.layoutParams.width = width
                    imageView.layoutParams.height = handFrame.height
                    imageView.x = ((p - 1) * frameWidth / ROOK).toFloat()

                    textView.x = (imageView.x + width * 0.85).toFloat()
                    textView.y = if (c == BLACK) 0.0f else imageView.layoutParams.height - textView.textSize * 1.5f
                    textView.bringToFront()
                }
                handImageViews[c].add(imageView)
                handTextViews[c].add(textView)
            }
        }

        //盤面を描画
        showPosition()

        // Miacisの手番なら実行
        if (player[pos.color()] == MIACIS) {
            thinkAndDo()
        }

        // ボタンの初期化
        findViewById<Button>(R.id.button_think).setOnClickListener {
            val bestMove = searcher.search(pos)
            val textView = findViewById<TextView>(R.id.think_result)
            textView.text = bestMove.toPrettyStr()
        }
        findViewById<Button>(R.id.button_think_and_do).setOnClickListener {
            thinkAndDo()
        }
        findViewById<Button>(R.id.button_init_pos).setOnClickListener {
            pos.init()
            showPosition()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val boardFrame = findViewById<FrameLayout>(R.id.frame)
        val boardX = boardFrame.width * marginRate
        val boardWidth = boardFrame.width - 2 * boardX
        val boardHeight = boardWidth * 1.07
        val squareWidth = boardWidth / 9
        val squareHeight = boardHeight / 9
        val boardY = boardFrame.height / 2 - boardHeight * 0.45 + boardFrame.y

        val upHandFrame = findViewById<FrameLayout>(R.id.frame_hand_up)
        val downHandFrame = findViewById<FrameLayout>(R.id.frame_hand_down)
        val handFrames = arrayOf(downHandFrame, upHandFrame)
        val heightRate = 1.5

        println("${upHandFrame.y} ${upHandFrame.height} ${downHandFrame.y} ${downHandFrame.height}")

        //単純な押下以外はとりあえず無視
        if (event.action != ACTION_DOWN) {
            //ここは指を離したときにも訪れてしまうのでresetHoldはしない
            return true
        }

        val pointX = event.x
        val pointY = event.y
        Log.d("TouchEvent", "X:$pointX,Y:$pointY")

        //駒台にタッチしているかどうかを判定
        for (c in BLACK..WHITE) {
            val handFrame = handFrames[c]
            if (handFrame.y <= pointY && pointY <= handFrame.y + handFrame.height * heightRate) {
                //手番と違う方の駒台に触っていたらリセット
                if (pos.color() != c) {
                    resetHold()
                    return true
                }

                for (i in 0 until ROOK) {
                    if (pos.hand_[c].num(ROOK - i) == 0) {
                        continue
                    }
                    if (handImageViews[c][i].x <= pointX && pointX <= handImageViews[c][i].x + handImageViews[c][i].width) {
                        //iが押された
                        holdPiece = coloredPiece(c, ROOK - i)
                        moveFrom = Square.WALL00
                        handImageViews[c][i].setBackgroundColor(backGroundHoldColor)
                        Log.d("TouchEvent", "catch to drop ${holdPiece} ${moveFrom}")
                        return true
                    }
                }
            }
        }

        //盤の中をタッチ
        if (boardX <= pointX && pointX <= boardX + boardWidth &&
            boardY <= pointY && pointY <= boardY + boardHeight) {
            val sqX = ((pointX - boardX) / squareWidth).toInt()
            val sqY = ((pointY - boardY) / squareHeight).toInt()
            val to = xy2square(sqX, sqY)

            if (holdPiece != EMPTY) {
                // Moveを構成
                val incompleteMove = if (moveFrom == Square.WALL00) {
                    dropMove(to, holdPiece)
                } else {
                    Move(to, moveFrom)
                }

                // to, fromから完全な情報を集めて成り、成らずの行動を生成
                val nonPromotiveMove = pos.transformValidMove(incompleteMove)
                val promotiveMove = promotiveMove(nonPromotiveMove)

                //合法性を判定
                val isLegalNonPromotive = pos.isLegalMove(nonPromotiveMove)
                val isLegalPromotive = pos.isLegalMove(promotiveMove)

                if (!isLegalNonPromotive && !isLegalPromotive) {
                    //両方非合法手だとダメ
                    resetHold()
                    Log.d("TouchEvent", "Illegal Move!")
                    return true
                } else if (!isLegalNonPromotive && isLegalPromotive) {
                    //歩、香車、桂馬は成らないと非合法であることがありえる
                    doMove(promotiveMove)
                } else if (isLegalNonPromotive && !isLegalPromotive) {
                    doMove(nonPromotiveMove)
                } else if (isLegalPromotive) {
                    // 選択が発生するのでAlertDialogを作成
                    AlertDialog.Builder(this)
                        .setTitle("成るか成らないか")
                        .setPositiveButton("成る") { dialog, which -> doMove(promotiveMove) }
                        .setNegativeButton("成らない") { dialog, which -> doMove(nonPromotiveMove) }
                        .setCancelable(false)
                        .create()
                        .show()
                }
            } else if (pos.on(to) != EMPTY && pieceToColor(pos.on(to)) == pos.color()) {
                //駒を掴む
                holdPiece = pos.on(xy2square(sqX, sqY))
                moveFrom = xy2square(sqX, sqY)
                squareImageViews[sqY * BOARD_WIDTH + sqX].setBackgroundColor(backGroundHoldColor)
            }
        }

        return true
    }

    private fun doMove(move: Move) {
        Log.d("doMove", "Move   :${move.toPrettyStr()}")

        if (pos.isLegalMove(move)) {
            pos.doMove(move)

            while (player[pos.color()] == MIACIS) {
                showPosition()
                val bestMove = searcher.search(pos)
                findViewById<TextView>(R.id.think_result).text = bestMove.toPrettyStr()
                pos.doMove(bestMove)
            }
        }

        //盤面を再描画
        showPosition()

        //保持した情報を解放
        resetHold()
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

    private fun resetHold() {
        if (moveFrom == Square.WALL00) {
            //持ち駒を指定していた
            for (i in 0 until ROOK) {
                val p = coloredPiece(pos.color(), i + 1)
                if (p == holdPiece) {
                    handImageViews[pos.color()][i].setBackgroundColor(0x00000000)
                }
            }

        } else if (moveFrom != Square.WALLAA) {
            //盤上のどこかを指定していた
            squareImageViews[square2sqid(moveFrom)].setBackgroundColor(0x00000000)
        }

        holdPiece = EMPTY
    }

    private fun showPosition() {
        //盤上の表示
        for (i in 0 until BOARD_WIDTH) {
            for (j in 0 until BOARD_WIDTH) {
                val sq = xy2square(j, i)
                squareImageViews[i * BOARD_WIDTH + j].setImageResource(piece2resourceID(pos.on(sq)))
                squareImageViews[i * BOARD_WIDTH + j].setBackgroundColor(backGroundTransparent)
            }
        }

        //持ち駒の表示
        for (c in BLACK..WHITE) {
            for (p in PAWN until KING) {
                handImageViews[c][ROOK - p].setImageResource(piece2resourceID(coloredPiece(c, p)))
                handImageViews[c][ROOK - p].setBackgroundColor(backGroundTransparent)
                handTextViews[c][ROOK - p].text = pos.hand_[c].num(p).toString()
            }
        }

        //最終行動マスの背景色を変更
        val lastMove = pos.lastMove()
        if (lastMove != NULL_MOVE) {
            val (lastX, lastY) = square2xy(lastMove.to())
            squareImageViews[lastY * BOARD_WIDTH + lastX].setBackgroundColor(backGroundMovedColor)
        }
    }

    private fun thinkAndDo() {
        val bestMove = searcher.search(pos)
        val textView = findViewById<TextView>(R.id.think_result)
        textView.text = bestMove.toPrettyStr()
        holdPiece = bestMove.subject()
        moveFrom = bestMove.from()
        doMove(bestMove)
    }

    private fun xy2square(x: Int, y: Int): Square {
        return SquareList[(BOARD_WIDTH - 1 - x) * BOARD_WIDTH + y]
    }

    private fun square2xy(sq: Square): Pair<Int, Int> {
        return Pair(BOARD_WIDTH - SquareToFile[sq.ordinal].ordinal, SquareToRank[sq.ordinal].ordinal - 1)
    }

    private fun square2sqid(sq: Square): Int {
        val (x, y) = square2xy(sq)
        return y * BOARD_WIDTH + x
    }
}