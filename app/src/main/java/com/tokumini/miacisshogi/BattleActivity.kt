package com.tokumini.miacisshogi

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.snackbar.Snackbar
import com.tokumini.miacisshogi.databinding.ActivityBattleBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.threeten.bp.LocalDateTime
import java.io.BufferedReader
import java.io.File
import kotlin.math.max
import kotlin.math.min


class BattleActivity : AppCompatActivity() {
    companion object {
        private const val HUMAN = 0
        private const val MIACIS = 1
        private val backGroundHoldColor = Color.rgb(0, 255, 0)
        private val backGroundMovedColor = Color.rgb(255, 128, 0)
        private const val backGroundTransparent = 0x00000000
    }

    private lateinit var binding: ActivityBattleBinding
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
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val oneTurnData = MutableList(1) { OneTurnData(NULL_MOVE, Array(BIN_SIZE) { 0.0f }) }
    private var searchNum = 0
    private val searchMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBattleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //盤面の準備
        pos = Position()

        //探索部の準備
        var randomTurn = intent?.extras?.get(KEY_RANDOM_TURN)
        randomTurn = if (randomTurn == null) 0 else randomTurn as Int
        searcher = Search(this, randomTurn)

        //ターンの制御
        mode = intent?.extras?.get(KEY_BATTLE_MODE) as Int
        when (mode) {
            HUMAN_TURN_BLACK -> {
                player = arrayOf(HUMAN, MIACIS)
                showSnackbar("あなたは先手です")
            }
            HUMAN_TURN_WHITE -> {
                player = arrayOf(MIACIS, HUMAN)
                binding.board.setImageResource(R.drawable.board2)
                showInverse = true
                showSnackbar("あなたは後手です")
            }
            CONSIDERATION -> player = arrayOf(HUMAN, HUMAN)
        }

        //探索回数の設定
        val searchNumOrNull = intent?.extras?.get(KEY_SEARCH_NUM)
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        if (searchNumOrNull != null) {
            searchNum = searchNumOrNull as Int
            with(sharedPref.edit()) {
                putInt(KEY_SEARCH_NUM, searchNum)
                apply()
            }
        } else {
            searchNum = sharedPref.getInt(KEY_SEARCH_NUM, 0)
        }

        //マス画像の初期化
        //ここで9 x 9のImageViewを作り、置かれている駒に応じて適切な画像を選択して置く
        squareImageViews = ArrayList()
        val frame = binding.frame
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
        val handFrameList = arrayOf(binding.frameHandDown, binding.frameHandUp)
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
            scope.launch {
                val bestMove = think()
                doMove(bestMove)
            }
        }

        val arrayAdapter = ArrayAdapter(this, R.layout.spinner_item, arrayOf("初期局面"))
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerMoves.adapter = arrayAdapter
        binding.spinnerMoves.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                //なぜか設定したタイミングでも呼ばれてしまうため、ターン数が同じ場合は動かさないようにする処理を入れることでごまかす
                //moveToTurnに突入しても局面は変わらないが、余計なthinkが入るため突入もさせない
                if (position != pos.turnNumber && mode == CONSIDERATION) {
                    moveToTurn(position)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        //過去棋譜のロード
        val file = intent?.extras?.get(KEY_LOAD_KIFU_FILE)
        if (file != null) {
            loadKifu(file as String)
        }

        // ボタンの初期化
        binding.buttonMenu.setOnClickListener { showMenu() }
        binding.buttonUndo.setOnClickListener { moveToTurn(max(pos.turnNumber - 1, 0)) }
        binding.buttonUndo.setOnLongClickListener {
            moveToTurn(0)
            true
        }
        binding.buttonRedo.setOnClickListener { redo() }
        binding.buttonRedo.setOnLongClickListener {
            moveToTurn(oneTurnData.size - 1)
            true
        }
        binding.buttonThink.setOnClickListener {
            scope.launch { think() }
        }
        binding.switchAutoThink.isChecked = sharedPref.getBoolean(getString(R.string.switch_auto_think), false)
        autoThink = binding.switchAutoThink.isChecked
        binding.switchAutoThink.setOnCheckedChangeListener { _, isChecked ->
            autoThink = isChecked
            // 書き出す
            with(sharedPref.edit()) {
                putBoolean(getString(R.string.switch_auto_think), isChecked)
                apply()
            }

            if (mode == CONSIDERATION && autoThink) {
                scope.launch { think() }
            }
        }
        binding.radioGraphMode.setOnCheckedChangeListener { _: RadioGroup, _: Int ->
            when (binding.radioGraphMode.checkedRadioButtonId) {
                R.id.radio_curr_value -> {
                    binding.barChart.visibility = View.VISIBLE
                    binding.scatterChart.visibility = View.INVISIBLE
                }
                R.id.radio_value_history -> {
                    binding.barChart.visibility = View.INVISIBLE
                    binding.scatterChart.visibility = View.VISIBLE
                }
            }
            with(sharedPref.edit()) {
                putInt(getString(R.string.radio_graph_mode), binding.radioGraphMode.checkedRadioButtonId)
                apply()
            }
            if (mode == CONSIDERATION && autoThink) {
                scope.launch { think() }
            }
        }
        binding.radioGraphMode.check(sharedPref.getInt(getString(R.string.radio_graph_mode), R.id.radio_value_history))
        binding.scatterChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight?) {
                //Entryのクリックなのでxは整数で得られる(Float型ではある)
                moveToTurn(e.x.toInt())
            }

            override fun onNothingSelected() {}
        })

        //対局モードのときはこのタイミングで表示を下げる
        if (mode != CONSIDERATION) {
            binding.tableLayout.visibility = View.INVISIBLE
            binding.barChart.visibility = View.INVISIBLE
            binding.scatterChart.visibility = View.INVISIBLE
            binding.radioGraphMode.visibility = View.INVISIBLE
            binding.buttonUndo.isEnabled = false
            binding.buttonRedo.isEnabled = false
            binding.buttonThink.isEnabled = false
            binding.switchAutoThink.isEnabled = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val boardFrame = binding.frame
        val boardX = boardFrame.width * marginRate
        val boardWidth = boardFrame.width - 2 * boardX
        val boardHeight = boardWidth * 1.07
        val squareWidth = boardWidth / 9
        val squareHeight = boardHeight / 9
        val boardY = boardFrame.height / 2 - boardHeight * 0.45 + boardFrame.y

        val upHandFrame = binding.frameHandUp
        val downHandFrame = binding.frameHandDown
        val handFrames = arrayOf(downHandFrame, upHandFrame)
        val heightRate = 1.5

        //単純な押下以外はとりあえず無視
        if (event.action != ACTION_DOWN) {
            //ここは指を離したときにも訪れてしまうのでresetHoldはしない
            return true
        }

        // ターンが人間の手番でなかったら無視
        if (player[pos.color] != HUMAN) {
            return true
        }

        val pointX = event.x
        val pointY = event.y

        //駒台にタッチしているかどうかを判定
        for (c in BLACK..WHITE) {
            val showIndex = if (showInverse) 1 - c else c
            val handFrame = handFrames[showIndex]
            if (handFrame.y <= pointY && pointY <= handFrame.y + handFrame.height * heightRate) {
                //手番と違う方の駒台に触っていた、あるいはすでに駒を持っている状態だったらリセット
                if (pos.color != c || holdPiece != EMPTY) {
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
        if (!pos.isLegalMove(move)) {
            //適当にエラー処理
            return
        }

        //盤面を動かす
        pos.doMove(move)

        //データを足す
        addOneTurnData(move)

        //盤面を再描画
        showPosition()

        //終了判定
        if (pos.getFinishStatus() != Position.NOT_FINISHED) {
            finishProcess()
            return
        }

        //対戦モードのとき、手番がCPU側に移ったことを確認して思考開始
        if (player[pos.color] == MIACIS && pos.getFinishStatus() == Position.NOT_FINISHED) {
            scope.launch { doMove(think()) }
        }

        //検討モードのとき、自動思考モードがオンであれば思考開始
        if (mode == CONSIDERATION && autoThink) {
            scope.launch { think() }
        }
    }

    private fun addOneTurnData(move: Move) {
        //データを積む
        //valueは空で初期化する
        if (oneTurnData.size > pos.turnNumber) { //先のデータがある場合は慎重に場合分け
            if (oneTurnData[pos.turnNumber - 1].move == move) {
                //同じ手を指す場合は消さない
            } else {
                //違う手の場合、リセットして詰む
                while (oneTurnData.size > pos.turnNumber) {
                    oneTurnData.removeLast()
                }
                //書き換える
                oneTurnData[pos.turnNumber - 1].move = move

                //次の領域を確保
                oneTurnData.add(OneTurnData(NULL_MOVE, Array(BIN_SIZE) { 0.0f }))
            }
        } else {
            //書き換える
            oneTurnData[pos.turnNumber - 1].move = move

            //次の領域を確保
            oneTurnData.add(OneTurnData(NULL_MOVE, Array(BIN_SIZE) { 0.0f }))
        }
    }

    private fun redo() {
        if (pos.turnNumber > oneTurnData.size) {
            return
        }

        val move = oneTurnData[pos.turnNumber].move

        if (!pos.isLegalMove(move)) {
            //適当にエラー処理
            return
        }

        //盤面を動かす
        pos.doMove(move)

        //盤面を再描画
        showPosition()

        //終了判定
        if (pos.getFinishStatus() != Position.NOT_FINISHED) {
            finishProcess()
            return
        }

        //検討モードのとき、自動思考モードがオンであれば思考開始
        if (mode == CONSIDERATION && autoThink) {
            scope.launch { think() }
        }
    }

    private fun moveToTurn(turn: Int) {
        //turn手目に移動する
        if (turn <= pos.turnNumber) {
            //undoを繰り返す
            val undoNum = pos.turnNumber - turn
            repeat(undoNum) {
                pos.undo()
            }
            if (mode == CONSIDERATION && autoThink) {
                scope.launch { think() }
            }
            showPosition()
        } else {
            //redoを繰り返す
            val redoNum = turn - pos.turnNumber
            repeat(redoNum) {
                if (pos.turnNumber >= oneTurnData.size - 1) {
                    return
                }

                val move = oneTurnData[pos.turnNumber].move

                if (!pos.isLegalMove(move)) {
                    //適当にエラー処理
                    return
                }

                //盤面を動かす
                pos.doMove(move)
            }

            //盤面を再描画
            showPosition()

            //終了判定
            if (pos.getFinishStatus() != Position.NOT_FINISHED) {
                finishProcess()
                return
            }

            //検討モードのとき、自動思考モードがオンであれば思考開始
            if (mode == CONSIDERATION && autoThink) {
                scope.launch { think() }
            }
        }
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
            //valueが常に片方の視点からになっていておかしいので変える
            for (i in oneTurnData.indices) {
                if (player[i % 2] == HUMAN) {
                    oneTurnData[i].value.reverse()
                }
            }

            //その他の変更
            mode = CONSIDERATION
            player = arrayOf(HUMAN, HUMAN)
            binding.tableLayout.visibility = View.VISIBLE
            binding.radioGraphMode.visibility = View.VISIBLE
            binding.buttonUndo.isEnabled = true
            binding.buttonRedo.isEnabled = true
            binding.buttonThink.isEnabled = true
            binding.switchAutoThink.isEnabled = true

            scope.launch { think() }

            when (binding.radioGraphMode.checkedRadioButtonId) {
                R.id.radio_curr_value -> {
                    binding.barChart.visibility = View.VISIBLE
                    binding.scatterChart.visibility = View.INVISIBLE
                }
                R.id.radio_value_history -> {
                    binding.barChart.visibility = View.INVISIBLE
                    binding.scatterChart.visibility = View.VISIBLE
                }
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

        //手数の表示
        binding.positionInfo.text = "手数:%d".format(pos.turnNumber)

        //指し手の表示
        val arrayAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            Array(oneTurnData.size) {
                "%3d:%s".format(it, if (it == 0) "初期局面" else oneTurnData[it - 1].move.toPrettyStr())
            })
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerMoves.adapter = arrayAdapter
        binding.spinnerMoves.setSelection(pos.turnNumber)

        //行動のための変数を初期化
        moveFrom = Square.WALLAA
        holdPiece = EMPTY
    }

    private suspend fun think(): Move {
        return withContext(Dispatchers.Default) {
            //posが書き換わっていく可能性があるためコピーを取る
            val currPosition = pos.copy()
            if (searchNum > 1) {
                binding.textViewThinkResultCover.text = "思考中..."
                binding.textViewThinkResultCover.setBackgroundColor(Color.rgb(255, 255, 255))
            }
            searchMutex.lock()
            val bestMove = searcher.search(currPosition, searchNum)
            val rootEntry = searcher.hashTable.rootEntry()
            showPolicy()
            oneTurnData[currPosition.turnNumber].value = rootEntry.value.clone()
            when (binding.radioGraphMode.checkedRadioButtonId) {
                R.id.radio_curr_value -> showValue()
                R.id.radio_value_history -> showValueHistory()
            }
            searchMutex.unlock()
            binding.textViewThinkResultCover.text = ""
            binding.textViewThinkResultCover.setBackgroundColor(backGroundTransparent)
            bestMove
        }
    }

    private fun showPolicy() {
        val rootEntry = searcher.hashTable.rootEntry()
        val policy = rootEntry.policy
        val moveList = rootEntry.moves
        val searchNum = if (searchNum == 0) Array(moveList.size) { 0 } else rootEntry.searchNum.clone()
        val q = if (this.searchNum == 0) Array(moveList.size) { 0.0f } else Array(moveList.size) { searcher.hashTable.valueExpectation(rootEntry, it) }
        val color = if (rootEntry.turnNumber % 2 == 0) BLACK else WHITE

        class MoveWithInfo(val move: Move, val policy: Float, val searchNum: Int, val q: Float)

        val list = List(moveList.size) { MoveWithInfo(moveList[it], policy[it], searchNum[it], q[it]) }
        val sortedList = list.sortedBy { -it.policy }
        val tableLayout = binding.tableLayout
        tableLayout.post {
            //以前の内容を削除
            tableLayout.removeAllViews()

            //見出し
            val labelRow = layoutInflater.inflate(R.layout.table_row, null) as TableRow
            labelRow.setBackgroundColor(Color.LTGRAY)
            labelRow.findViewById<TextView>(R.id.rowtext0).text = "順位"
            labelRow.findViewById<TextView>(R.id.rowtext1).text = "指し手"
            labelRow.findViewById<TextView>(R.id.rowtext2).text = "方策確率"
            labelRow.findViewById<TextView>(R.id.rowtext3).text = "探索回数"
            labelRow.findViewById<TextView>(R.id.rowtext4).text = "評価値"
            tableLayout.addView(labelRow, TableLayout.LayoutParams())

            //各指し手
            for (i in sortedList.indices) {
                val info = sortedList[i]
                val tableRow = layoutInflater.inflate(R.layout.table_row, null) as TableRow
                tableRow.findViewById<TextView>(R.id.rowtext0).text = (i + 1).toString()
                tableRow.findViewById<TextView>(R.id.rowtext1).text = info.move.toPrettyStr()
                tableRow.findViewById<TextView>(R.id.rowtext2).text = "%5.1f%%".format((info.policy * 100))
                tableRow.findViewById<TextView>(R.id.rowtext3).text = info.searchNum.toString()

                val v = if (color == BLACK) info.q else -info.q
                tableRow.findViewById<TextView>(R.id.rowtext4).text = if (info.searchNum == 0) "None" else "%.3f".format(v)
                tableLayout.addView(tableRow, TableLayout.LayoutParams())
            }
        }
    }

    private fun showValue() {
        val rootEntry = searcher.hashTable.rootEntry()
        val currValue = rootEntry.value.clone()

        //対応するxの値を作成
        val x = Array(BIN_SIZE) { MIN_SCORE + VALUE_WIDTH * (it + 0.5f) }

        if (pos.color == WHITE) {
            currValue.reverse()
        }

        //Entryにデータ格納
        val entryList = mutableListOf<BarEntry>()
        for (i in x.indices) {
            entryList.add(BarEntry(x[i], currValue[i]))
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
        val barChart = binding.barChart
        barChart.post {
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
    }

    private fun showValueHistory() {
        //Entryにデータ格納
        val probBin = 20
        val probWidth = 1.0f / probBin
        val entryList = List(probBin) { mutableListOf<Entry>() }

        for (i in oneTurnData.indices) {
            val v = oneTurnData[i].value.clone()
            if (i % 2 == 1) {
                v.reverse()
            }
            for (j in 0 until BIN_SIZE) {
                val prob = v[j]
                val index = min((prob / probWidth).toInt(), probBin - 1)
                val y = MIN_SCORE + VALUE_WIDTH * (j + 0.5f)
                entryList[index].add(Entry((i).toFloat(), y))
            }
        }

        //DataSetのリスト
        val scatterDataSets = mutableListOf<IScatterDataSet>()
        for (i in 0 until probBin) {
            val scatterDataSet = ScatterDataSet(entryList[i], "value")
            val n = 255 - (255.0 / (probBin - 1) * i).toInt()
            scatterDataSet.color = Color.rgb(n, n, 255)
            scatterDataSet.formSize = 1.0f / BIN_SIZE / 10 / 10
            scatterDataSets.add(scatterDataSet)
        }

        //格納
        val scatterData = ScatterData(scatterDataSets)
        scatterData.setDrawValues(false)

        //ChartにData格納
        val scatterChart = binding.scatterChart
        scatterChart.post {
            scatterChart.data = scatterData
            scatterChart.legend.isEnabled = false
            scatterChart.description.isEnabled = false
            scatterChart.setScaleEnabled(false)
            scatterChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
//            scatterChart.xAxis.axisMaximum = MAX_SCORE
//            scatterChart.xAxis.axisMinimum = MIN_SCORE
//            scatterChart.axisLeft.axisMaximum = 1.1f
//            scatterChart.axisLeft.axisMinimum = 0.0f
//            scatterChart.axisRight.axisMaximum = 1.1f
//            scatterChart.axisRight.axisMinimum = 0.0f

            //chart更新
            scatterChart.invalidate()
        }
    }

    private fun showMenu() {
        fun backToTop() {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        fun inputSfen() {
            val editText = EditText(this)
            editText.hint = "lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1"
            AlertDialog.Builder(this)
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    var sfen = editText.text.toString()
                    sfen = sfen.trim('\n')
                    sfen = sfen.removePrefix("sfen ")
                    if (isValidSfen(sfen)) {
                        pos.fromStr(sfen)
                        showPosition()
                    } else {
                        showSnackbar("SFEN文字列が不正です")
                    }
                }
                .show()
        }

        fun outputSfen() {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("sfen", pos.toStr())
            clipboard.setPrimaryClip(clip)
            showSnackbar("現局面のSFENをクリップボードへコピーしました")
        }

        fun clearResult() {
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

                    showSnackbar("戦績を削除しました")
                }
                .show()
        }

        fun changeSearchNum() {
            val editText = EditText(this)
            editText.hint = "最大${maxSearchNum}まで"
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            AlertDialog.Builder(this)
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    val searchNumOrNull = editText.text.toString().toInt()
                    searchNum = min(searchNumOrNull, maxSearchNum)
                    showSnackbar("探索量を${searchNum}に変更しました")
                    val sharedPref = getPreferences(Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putInt(KEY_SEARCH_NUM, searchNum)
                        apply()
                    }
                }
                .show()
        }

        if (mode != CONSIDERATION) {
            AlertDialog.Builder(this)
                .setTitle("メニュー")
                .setItems(itemsInBattleMode) { _, which ->
                    when (which) {
                        MenuInBattleMode.BACK_TO_TOP.ordinal -> backToTop()
                        MenuInBattleMode.RESIGN.ordinal -> finishProcess()
                        MenuInBattleMode.OUTPUT_SFEN.ordinal -> outputSfen()
                        MenuInBattleMode.CLEAR_RESULT.ordinal -> clearResult()
                    }
                }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("メニュー")
                .setItems(itemsInConsiderationMode) { _, which ->
                    when (which) {
                        MenuInConsiderationMode.BACK_TO_TOP.ordinal -> backToTop()
                        MenuInConsiderationMode.CHANGE_SEARCH_NUM.ordinal -> changeSearchNum()
                        MenuInConsiderationMode.SAVE_KIFU.ordinal -> saveKifu()
                        MenuInConsiderationMode.INPUT_SFEN.ordinal -> inputSfen()
                        MenuInConsiderationMode.OUTPUT_SFEN.ordinal -> outputSfen()
                        MenuInConsiderationMode.CLEAR_RESULT.ordinal -> clearResult()
                    }
                }
                .show()
        }
    }

    private fun saveKifu() {
        val timeStr = LocalDateTime.now().toString()
        val prettyStr = timeStr.removeSuffix(timeStr.takeLast(4))
        val defaultFileName = "$prettyStr.txt"

        val editText = EditText(this)
        editText.setText(defaultFileName)
        AlertDialog.Builder(this)
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                val filename = editText.text.toString().replace(' ', '_')

                val currTurn = pos.turnNumber
                repeat(currTurn) {
                    pos.undo()
                }
                var str = pos.toStr()
                for (i in 0 until oneTurnData.size - 1) {
                    str += "\t"
                    str += oneTurnData[i].move.toSfenStr()
                }
                for (i in 0 until oneTurnData.size) {
                    if (pos.turnNumber == currTurn) {
                        break
                    }
                    pos.doMove(oneTurnData[i].move)
                }
                File(applicationContext.filesDir, filename).writer().use {
                    it.write(str)
                }
                showSnackbar("棋譜を保存しました")
            }
            .show()
    }

    private fun loadKifu(filename: String) {
        val readFile = File(applicationContext.filesDir, filename)

        if (!readFile.exists()) {
            return
        }

        val preAutoThink = autoThink
        autoThink = false

        val str = readFile.bufferedReader().use(BufferedReader::readText)
        val split = str.split("\t")
        pos.fromStr(split[0])
        for (i in 1 until split.size) {
            val move = pos.transformValidMove(stringToMove(split[i]))
            doMove(move)
        }
        autoThink = preAutoThink
    }

    private fun showSnackbar(text: String) {
        Snackbar.make(
            binding.constraintLayout,
            text,
            Snackbar.LENGTH_SHORT
        ).show()
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