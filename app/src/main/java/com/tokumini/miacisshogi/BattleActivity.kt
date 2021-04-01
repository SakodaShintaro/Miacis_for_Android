package com.tokumini.miacisshogi

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.widget.*
import android.widget.TableLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.android.material.snackbar.Snackbar


class BattleActivity : AppCompatActivity() {
    companion object {
        private const val HUMAN = 0
        private const val MIACIS = 1
        private val backGroundHoldColor = Color.rgb(0, 255, 0)
        private val backGroundMovedColor = Color.rgb(255, 128, 0)
        private const val backGroundTransparent = 0x00000000
    }

    private lateinit var squareImageViews: ArrayList<ImageView>
    private lateinit var handImageViews: Array<ArrayList<ImageView>>
    private lateinit var handTextViews: Array<ArrayList<TextView>>
    private lateinit var pos: Position
    private var holdPiece: Int = EMPTY
    private var moveFrom: Square = Square.WALLAA
    private val marginRate = 0.05
    private lateinit var searcher: Search
    private lateinit var player: Array<Int>
    private var mode: Int = CONSIDERATION
    private var showInverse: Boolean = false
    private var autoThink: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        //盤面の準備
        pos = Position()

        //探索部の準備
        searcher = Search(this, 10)

        //ターンの制御
        mode = intent?.extras?.get(TURN_STR) as Int
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

        if (mode != CONSIDERATION) {
            findViewById<TableLayout>(R.id.tableLayout).visibility = View.INVISIBLE
            findViewById<BarChart>(R.id.barChartExample).visibility = View.INVISIBLE
            findViewById<Button>(R.id.button_undo).isEnabled = false
            findViewById<Button>(R.id.button_think).isEnabled = false
            findViewById<SwitchCompat>(R.id.switch_auto_think).isEnabled = false
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
            val bestMove = think()
            holdPiece = bestMove.subject()
            moveFrom = bestMove.from()
            doMove(bestMove)
        }

        // ボタンの初期化
        findViewById<Button>(R.id.button_menu).setOnClickListener {
            showMenu()
        }
        findViewById<Button>(R.id.button_undo).setOnClickListener {
            pos.undo()
            if (mode == CONSIDERATION && autoThink) {
                think()
            }
            showPosition()
        }
        findViewById<Button>(R.id.button_think).setOnClickListener {
            think()
        }
        findViewById<SwitchCompat>(R.id.switch_auto_think).setOnCheckedChangeListener { _, isChecked ->
            autoThink = isChecked
            think()
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

        //駒台にタッチしているかどうかを判定
        for (c in BLACK..WHITE) {
            val showIndex = if (showInverse) 1 - c else c
            val handFrame = handFrames[showIndex]
            if (handFrame.y <= pointY && pointY <= handFrame.y + handFrame.height * heightRate) {
                //手番と違う方の駒台に触っていたらリセット
                if (pos.color != c) {
                    showPosition()
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
                    showPosition()
                    return true
                } else if (!isLegalNonPromotive && isLegalPromotive) {
                    //歩、香車、桂馬は成らないと非合法であることがありえる
                    doMove(promotiveMove)
                } else if (isLegalNonPromotive && !isLegalPromotive) {
                    doMove(nonPromotiveMove)
                } else if (isLegalPromotive) {
                    // 選択が発生するのでAlertDialogを作成
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("成りますか？")
                        .setPositiveButton("成る") { _, _ -> doMove(promotiveMove) }
                        .setNegativeButton("成らない") { _, _ -> doMove(nonPromotiveMove) }
                        .setCancelable(true)
                        .setOnCancelListener {
                            showPosition()
                        }
                        .show()
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).apply {
                        textSize = 24.0f
                    }
                    dialog.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
                        textSize = 24.0f
                    }
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
        if (pos.isLegalMove(move)) {
            pos.doMove(move)

            if (player[pos.color] == MIACIS && pos.getFinishStatus() == Position.NOT_FINISHED) {
                showPosition()
                val bestMove = think()
                pos.doMove(bestMove)
            }

            if (pos.getFinishStatus() != Position.NOT_FINISHED) {
                finishProcess()
            }
        }

        if (mode == CONSIDERATION && autoThink) {
            think()
        }

        //盤面を再描画
        showPosition()
    }

    private fun finishProcess() {
        val status = pos.getFinishStatus()
        val winColor = if (status == Position.WIN) pos.color else color2oppositeColor(pos.color)

        val resultStr = if (mode == CONSIDERATION) {
            when {
                status == Position.DRAW -> "引き分け"
                winColor == BLACK -> "先手の勝ち"
                else -> "後手の勝ち"
            }
        } else {
            val sharedPref = getPreferences(Context.MODE_PRIVATE)
            var winNum = sharedPref.getInt(getString(R.string.result_user_win), 0)
            var drawNum = sharedPref.getInt(getString(R.string.result_user_draw), 0)
            var loseNum = sharedPref.getInt(getString(R.string.result_user_lose), 0)

            // 今回の結果を足す
            when {
                status == Position.DRAW -> drawNum++
                player[winColor] == HUMAN -> winNum++
                else -> loseNum++
            }

            // 書き出す
            with(sharedPref.edit()) {
                putInt(getString(R.string.result_user_win), winNum)
                putInt(getString(R.string.result_user_draw), drawNum)
                putInt(getString(R.string.result_user_lose), loseNum)
                apply()
            }

            val resultHistory = "通算 ${winNum}勝 ${drawNum}引分 ${loseNum}敗"

            when {
                status == Position.DRAW -> "引き分け\n${resultHistory}"
                player[winColor] == HUMAN -> "あなたの勝ち\n${resultHistory}"
                else -> "あなたの負け\n${resultHistory}"
            }
        }

        AlertDialog.Builder(this)
            .setTitle(resultStr)
            .setPositiveButton("OK") { _, _ -> }
            .setCancelable(false)
            .create()
            .show()

        if (mode != CONSIDERATION) {
            //対局モードから検討モードへ移行する
            mode = CONSIDERATION
            player = arrayOf(HUMAN, HUMAN)
            findViewById<TableLayout>(R.id.tableLayout).visibility = View.VISIBLE
            findViewById<BarChart>(R.id.barChartExample).visibility = View.VISIBLE
            findViewById<Button>(R.id.button_undo).isEnabled = true
            findViewById<Button>(R.id.button_think).isEnabled = true
            findViewById<SwitchCompat>(R.id.switch_auto_think).isEnabled = true
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

            else -> R.drawable.illegal
        }
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

        //行動のための変数を初期化
        moveFrom = Square.WALLAA
        holdPiece = EMPTY
    }

    private fun think(): Move {
        val bestMove = searcher.search(pos)
        val moveList = pos.generateAllMoves()
        val policy = searcher.policy
        val policyAndMove = policy.zip(moveList).sortedBy { pair -> -pair.first }

        val tableLayout = findViewById<TableLayout>(R.id.tableLayout)

        //以前の内容を削除
        tableLayout.removeAllViews()

        //見出し
        val labelRow = layoutInflater.inflate(R.layout.table_row, null) as TableRow
        labelRow.findViewById<TextView>(R.id.rowtext1).text = "指し手"
        labelRow.findViewById<TextView>(R.id.rowtext2).text = "方策確率"
        tableLayout.addView(labelRow, TableLayout.LayoutParams())

        //各指し手
        for (i in policy.indices) {
            val tableRow = layoutInflater.inflate(R.layout.table_row, null) as TableRow
            tableRow.findViewById<TextView>(R.id.rowtext1).text = policyAndMove[i].second.toPrettyStr()
            tableRow.findViewById<TextView>(R.id.rowtext2).text = "%5.1f%%".format((policyAndMove[i].first * 100))
            tableLayout.addView(tableRow, TableLayout.LayoutParams())
        }

        showValue(searcher.value)
        return bestMove
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

    private fun showMenu() {
        AlertDialog.Builder(this)
            .setTitle("メニュー")
            .setItems(items) { _, which ->
                when (which) {
                    Menu.BACK_TO_TOP.ordinal -> {
                        finish()
                    }
                    Menu.RESIGN.ordinal -> {
                        finishProcess()
                    }
                    Menu.INIT_POSITION.ordinal -> {
                        pos.init()
                        showPosition()
                    }
                    Menu.INPUT_SFEN.ordinal -> {
                        val editText = EditText(this)
                        editText.hint = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"
                        AlertDialog.Builder(this)
                            .setView(editText)
                            .setPositiveButton("OK") { _, _ ->
                                pos.fromStr(editText.text.toString())
                                showPosition()
                            }
                            .show()
                    }
                    Menu.OUTPUT_SFEN.ordinal -> {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData = ClipData.newPlainText("sfen", pos.toStr())
                        clipboard.setPrimaryClip(clip)
                        Snackbar.make(
                            findViewById(R.id.constraintLayout),
                            "現局面のSFENをクリップボードへコピーしました",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    Menu.CLEAR_RESULT.ordinal -> {
                        AlertDialog.Builder(this)
                            .setMessage("本当に戦績を削除しますか?")
                            .setNegativeButton("キャンセル") { _, _ -> }
                            .setPositiveButton("OK") { _, _ ->
                                // (0, 0, 0)を書き出す
                                val sharedPref = getPreferences(Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putInt(getString(R.string.result_user_win), 0)
                                    putInt(getString(R.string.result_user_draw), 0)
                                    putInt(getString(R.string.result_user_lose), 0)
                                    apply()
                                }

                                Snackbar.make(
                                    findViewById(R.id.constraintLayout),
                                    "戦績を削除しました",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                            .show()
                    }
                    else -> {
                        if (BuildConfig.DEBUG) {
                            error("Assertion failed")
                        }
                    }
                }
            }
            .show()
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