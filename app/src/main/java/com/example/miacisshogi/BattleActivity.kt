package com.example.miacisshogi

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet


const val HUMAN = 0
const val MIACIS = 1

class BattleActivity : AppCompatActivity() {
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
    private var mode: Int = CONSIDERATION
    private var showInverse: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        //盤面の準備
        pos = Position()

        //探索部の準備
        searcher = Search(this)

        //ターンの制御
        mode = intent?.extras?.get(TURN_STR) as Int
        println("mode = $mode")
        when (mode) {
            HUMAN_TURN_BLACK -> player = arrayOf(HUMAN, MIACIS)
            HUMAN_TURN_WHITE -> {
                player = arrayOf(MIACIS, HUMAN)
                val boardView = findViewById<ImageView>(R.id.board)
                boardView.setImageResource(R.drawable.board2)
                showInverse = true
            }
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
        if (player[pos.color] == MIACIS) {
            thinkAndDo()
        }

        // ボタンの初期化
        findViewById<Button>(R.id.button_menu).setOnClickListener {
            val items = arrayOf("トップ画面に戻る", "盤面を初期化", "sfenを入力", "局面のsfenをクリップボードにコピー")
            AlertDialog.Builder(this)
                .setTitle("メニュー")
                .setItems(items) { dialog, which ->
                    when (which) {
                        0 -> {
                            finish()
                        }
                        1 -> {
                            pos.init()
                            showPosition()
                        }
                        2 -> {
                            val editText = EditText(this)
                            editText.hint = "sfen ~"
                            AlertDialog.Builder(this)
                                .setView(editText)
                                .setPositiveButton("OK") { dialog, which ->
                                    pos.fromStr(editText.text.toString())
                                    showPosition()
                                }
                                .show()
                        }
                        3 -> {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip: ClipData = ClipData.newPlainText("sfen", pos.toStr())
                            clipboard.setPrimaryClip(clip)
                        }
                    }
                }
                .show()
        }
        findViewById<Button>(R.id.button_undo).setOnClickListener {
            pos.undo()
            showPosition()
        }
        findViewById<Button>(R.id.button_think).setOnClickListener {
            val bestMove = searcher.search(pos)
            val textView = findViewById<TextView>(R.id.think_result)
            val moveList = pos.generateAllMoves()
            var str = String()
            val policy = searcher.policy
            val policyAndMove = policy.zip(moveList).sortedBy { pair -> -pair.first }

            for (i in policy.indices) {
                str += "%.4f ${policyAndMove[i].second.toPrettyStr()}\n".format(policyAndMove[i].first)

                if (i >= 2) {
                    break
                }
            }
            textView.text = str

            showValue(searcher.value)
        }
        findViewById<Button>(R.id.button_think_and_do).setOnClickListener {
            thinkAndDo()
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
            val showIndex = if (showInverse) 1 - c else c
            val handFrame = handFrames[showIndex]
            if (handFrame.y <= pointY && pointY <= handFrame.y + handFrame.height * heightRate) {
                //手番と違う方の駒台に触っていたらリセット
                if (pos.color != c) {
                    resetHold()
                    return true
                }

                for (i in 0 until ROOK) {
                    if (pos.hand[c].num(ROOK - i) == 0) {
                        continue
                    }
                    if (handImageViews[showIndex][i].x <= pointX && pointX <= handImageViews[showIndex][i].x + handImageViews[showIndex][i].width) {
                        //iが押された
                        holdPiece = coloredPiece(c, ROOK - i)
                        moveFrom = Square.WALL00
                        handImageViews[showIndex][i].setBackgroundColor(backGroundHoldColor)
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
            var to = xy2square(sqX, sqY)
            if (showInverse) {
                to = InvSquare[to.ordinal]
            }

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
                    showPosition()
                    Log.d("TouchEvent", "Illegal Move! ${nonPromotiveMove.toPrettyStr()}")
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
            } else if (pos.on(to) != EMPTY && pieceToColor(pos.on(to)) == pos.color) {
                //駒を掴む
                holdPiece = pos.on(to)
                moveFrom = to
                squareImageViews[sqY * BOARD_WIDTH + sqX].setBackgroundColor(backGroundHoldColor)
            }
        }

        return true
    }

    private fun doMove(move: Move) {
        Log.d("doMove", "Move   :${move.toPrettyStr()}")
        Log.d("doMove", "sfen   :${pos.toStr()}")
        Log.d("doMove", "hash   :${pos.hashValue}")

        if (pos.isLegalMove(move)) {
            pos.doMove(move)

            if (player[pos.color] == MIACIS && pos.isFinish() == pos.NOT_FINISHED) {
                showPosition()
                val bestMove = searcher.search(pos)
                findViewById<TextView>(R.id.think_result).text = bestMove.toPrettyStr()
                pos.doMove(bestMove)
            }

            val status = pos.isFinish()
            if (status != pos.NOT_FINISHED) {
                val winColor = if (status == pos.WIN) pos.color else color2oppositeColor(pos.color)
                val resultStr = if (status == pos.DRAW) {
                    "Draw"
                } else if (player[winColor] == HUMAN) {
                    "You Win"
                } else {
                    "You Lose"
                }
                AlertDialog.Builder(this)
                    .setTitle(resultStr)
                    .setPositiveButton("OK") { dialog, which -> }
                    .setCancelable(false)
                    .create()
                    .show()

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
                handImageViews[pos.color][i].setBackgroundColor(0x00000000)
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
                if (!showInverse) {
                    squareImageViews[i * BOARD_WIDTH + j].setImageResource(piece2resourceID(pos.on(sq)))
                } else {
                    val invSq = InvSquare[sq.ordinal]
                    val piece = piece2oppositeColorPiece(pos.on(invSq))
                    squareImageViews[i * BOARD_WIDTH + j].setImageResource(piece2resourceID(piece))
                }
                squareImageViews[i * BOARD_WIDTH + j].setBackgroundColor(backGroundTransparent)
            }
        }

        //持ち駒の表示
        for (c in BLACK..WHITE) {
            for (p in PAWN until KING) {
                val n = pos.hand[c].num(p)

                val showIndex = if (showInverse) 1 - c else c
                handImageViews[showIndex][ROOK - p].setImageResource(
                    if (n == 0) piece2resourceID(EMPTY) else piece2resourceID(coloredPiece(showIndex, p))
                )
                handImageViews[showIndex][ROOK - p].setBackgroundColor(backGroundTransparent)
                handTextViews[showIndex][ROOK - p].text = if (n > 1) n.toString() else ""
            }
        }

        //最終行動マスの背景色を変更
        val lastMove = pos.lastMove()
        if (lastMove != NULL_MOVE) {
            val to = if (showInverse) InvSquare[lastMove.to().ordinal] else lastMove.to()
            squareImageViews[square2sqid(to)].setBackgroundColor(backGroundMovedColor)
        }
    }

    private fun thinkAndDo() {
        val bestMove = searcher.search(pos)
        val textView = findViewById<TextView>(R.id.think_result)

        val moveList = pos.generateAllMoves()

        var str = String()
        val policy = searcher.policy
        val policyAndMove = policy.zip(moveList).sortedBy { pair -> -pair.first }

        for (i in policy.indices) {
            str += "%.4f ${policyAndMove[i].second.toPrettyStr()}\n".format(policyAndMove[i].first)

            if (i >= 2) {
                break
            }
        }
        textView.text = str

        showValue(searcher.value)

        holdPiece = bestMove.subject()
        moveFrom = bestMove.from()
        doMove(bestMove)
    }

    private fun showValue(value: Array<Float>) {
        //対応するxの値を作成
        val x = Array(BIN_SIZE) { MIN_SCORE + VALUE_WIDTH * (it + 0.5f) }

        if (pos.color == WHITE) {
            value.reverse()
        }

        //Entryにデータ格納
        val entryList = mutableListOf<BarEntry>()
        for (i in x.indices) {
            entryList.add(BarEntry(x[i], value[i]))
        }

        //BarDataSetのリスト
        val barDataSet = BarDataSet(entryList, "value")
        barDataSet.color = Color.BLUE
        val barDataSets = mutableListOf<IBarDataSet>(barDataSet)

        //BarDataにBarDataSet格納
        val barData = BarData(barDataSets)
        barData.barWidth = VALUE_WIDTH
        barData.setDrawValues(false)

        //BarChartにBarData格納
        val barChart = findViewById<BarChart>(R.id.barChartExample)
        barChart.data = barData
        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false
        barChart.setScaleEnabled(false)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.axisMaximum = MAX_SCORE
        barChart.xAxis.axisMinimum = MIN_SCORE
        barChart.axisLeft.axisMaximum = 1.1f
        barChart.axisLeft.axisMinimum = 0.0f
        barChart.axisRight.axisMaximum = 1.1f
        barChart.axisRight.axisMinimum = 0.0f

        //barchart更新
        barChart.invalidate()
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