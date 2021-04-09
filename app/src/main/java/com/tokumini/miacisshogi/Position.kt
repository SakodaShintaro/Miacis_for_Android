package com.tokumini.miacisshogi

import kotlin.random.Random

class Position {
    //------------------
    //    クラス変数
    //------------------
    companion object {
        //ハッシュの各駒・位置に対する決められた値
        private val hashSeed = Array(PieceNum) { Array(Square.SquareNum.ordinal) { Random.nextLong() } }
        private val handHashSeed = Array(ColorNum) { Array(PieceNum) { Array(19) { Random.nextLong() } } }

        //getFinishStatusで返す結果
        const val WIN = 0
        const val DRAW = 1
        const val LOSE = 2
        const val NOT_FINISHED = 3
    }

    //------------------------
    //    インスタンス変数
    //------------------------
    //手番
    var color: Int = 0

    //盤面
    private var board = Array(Square.SquareNum.ordinal) { WALL }

    //持ち駒
    var hand = arrayListOf(Hand(), Hand())

    //手数
    var turnNumber: Int = 0

    //玉の位置
    private var kingSq = arrayListOf(Square.WALL00, Square.WALL00)

    //現局面までの指し手履歴
    private var kifu = ArrayList<Move>()

    //現局面の合法手
    private var moves = ArrayList<Move>()

    private var alreadyGeneratedMoves = false

    //現局面のハッシュ値
    var hashValue: Long = 0
    var boardHash: Long = 0
    var handHash: Long = 0

    var isChecked: Boolean = false

    class StateInfo(pos: Position) {
        //千日手判定用に必要な情報
        var boardHash = pos.boardHash
        var handHash = pos.handHash
        var isChecked = pos.isChecked
    }

    private var stack = ArrayList<StateInfo>()

    init {
        init()
    }

    fun copy(): Position {
        val copy = Position()
        copy.fromStr(this.toStr())
        return copy
    }

    //初期化
    fun init() {
        //盤上の初期化
        for (i in 0 until Square.SquareNum.ordinal) {
            board[i] = WALL
        }
        for (sq in SquareList) {
            board[sq.ordinal] = EMPTY
        }

        //後手の駒
        board[Square.SQ11.ordinal] = WHITE_LANCE
        board[Square.SQ21.ordinal] = WHITE_KNIGHT
        board[Square.SQ31.ordinal] = WHITE_SILVER
        board[Square.SQ41.ordinal] = WHITE_GOLD
        board[Square.SQ51.ordinal] = WHITE_KING
        board[Square.SQ61.ordinal] = WHITE_GOLD
        board[Square.SQ71.ordinal] = WHITE_SILVER
        board[Square.SQ81.ordinal] = WHITE_KNIGHT
        board[Square.SQ91.ordinal] = WHITE_LANCE
        board[Square.SQ22.ordinal] = WHITE_BISHOP
        board[Square.SQ82.ordinal] = WHITE_ROOK
        board[Square.SQ13.ordinal] = WHITE_PAWN
        board[Square.SQ23.ordinal] = WHITE_PAWN
        board[Square.SQ33.ordinal] = WHITE_PAWN
        board[Square.SQ43.ordinal] = WHITE_PAWN
        board[Square.SQ53.ordinal] = WHITE_PAWN
        board[Square.SQ63.ordinal] = WHITE_PAWN
        board[Square.SQ73.ordinal] = WHITE_PAWN
        board[Square.SQ83.ordinal] = WHITE_PAWN
        board[Square.SQ93.ordinal] = WHITE_PAWN

        //先手の駒
        board[Square.SQ19.ordinal] = BLACK_LANCE
        board[Square.SQ29.ordinal] = BLACK_KNIGHT
        board[Square.SQ39.ordinal] = BLACK_SILVER
        board[Square.SQ49.ordinal] = BLACK_GOLD
        board[Square.SQ59.ordinal] = BLACK_KING
        board[Square.SQ69.ordinal] = BLACK_GOLD
        board[Square.SQ79.ordinal] = BLACK_SILVER
        board[Square.SQ89.ordinal] = BLACK_KNIGHT
        board[Square.SQ99.ordinal] = BLACK_LANCE
        board[Square.SQ88.ordinal] = BLACK_BISHOP
        board[Square.SQ28.ordinal] = BLACK_ROOK
        board[Square.SQ17.ordinal] = BLACK_PAWN
        board[Square.SQ27.ordinal] = BLACK_PAWN
        board[Square.SQ37.ordinal] = BLACK_PAWN
        board[Square.SQ47.ordinal] = BLACK_PAWN
        board[Square.SQ57.ordinal] = BLACK_PAWN
        board[Square.SQ67.ordinal] = BLACK_PAWN
        board[Square.SQ77.ordinal] = BLACK_PAWN
        board[Square.SQ87.ordinal] = BLACK_PAWN
        board[Square.SQ97.ordinal] = BLACK_PAWN

        //持ち駒
        hand[BLACK].clear()
        hand[WHITE].clear()

        //手番
        color = BLACK

        //手数
        turnNumber = 1

        //玉の位置
        kingSq[BLACK] = Square.SQ59
        kingSq[WHITE] = Square.SQ51

        //ハッシュ値の初期化
        initHashValue()

        stack.clear()
        kifu.clear()

        isChecked = false

        //合法手生成のフラグを降ろす
        alreadyGeneratedMoves = false
    }

    //内部の状態等を表示する関数
    fun print() {
        //盤上
        println("９８７６５４３２１")
        println("------------------")
        for (r in Rank.Rank1.ordinal..Rank.Rank9.ordinal) {
            for (f in FILE.File9.ordinal..FILE.File1.ordinal) {
                print(PieceToSfenStrWithSpace[board[FRToSquare[f][r].ordinal]])
            }
            println("|${r}")
        }

        //持ち駒
        println("持ち駒")
        println("先手:")
        hand[BLACK].print()
        println("後手:")
        hand[WHITE].print()

        //その他
        println("手番:${if (color == BLACK) "先手" else "後手"}")
        println("手数:${turnNumber}")
        if (kifu.isNotEmpty()) {
            println("最後の手:${lastMove().toPrettyStr()}")
        }
        println("ハッシュ値:${hashValue}")
    }

    //一手進める・戻す関数
    fun doMove(move: Move) {
        //動かす前の状態を残しておく
        stack.add(StateInfo(this))

        val to = move.to().ordinal
        val from = move.from().ordinal

        //実際に動かす
        if (move.isDrop()) { //持ち駒を打つ手

            //持ち駒を減らす
            hand[color].sub(kind(move.subject()))

            //移動先にsubjectを設置
            board[to] = move.subject()

            //ハッシュ値の更新
            //打つ前のHandHashとXORして消す
            val num = hand[color].num(kind(move.subject()))
            handHash = handHash xor handHashSeed[color][kind(move.subject())][num + 1]
            //打った後のHandHashとXOR
            handHash = handHash xor handHashSeed[color][kind(move.subject())][num]
            //打った後の分をXOR
            boardHash = boardHash xor hashSeed[move.subject()][to]

        } else { //盤上の駒を動かす手

            //移動する駒を消す
            board[from] = EMPTY

            //取った駒があるならその駒を消し、持ち駒を増やす
            if (move.capture() != EMPTY) {
                //取った駒を消す
                board[to] = EMPTY

                //持ち駒を増やす
                hand[color].add(kind(move.capture()))

                //ハッシュ値の更新
                //取った駒分のハッシュをXOR
                boardHash = boardHash xor hashSeed[move.capture()][to]
                //増える前の持ち駒の分をXORして消す
                handHash =
                    handHash xor handHashSeed[color][kind(move.capture())][hand[color].num(
                        kind(move.capture())
                    ) - 1]
                //増えた後の持ち駒の分XOR
                handHash =
                    handHash xor handHashSeed[color][kind(move.capture())][hand[color].num(
                        kind(move.capture())
                    )]
            }

            //成る手ならsubjectに成りのフラグを立てて,そうでないならsubjectをそのまま移動先に設置
            if (move.isPromote()) {
                board[to] = promote(move.subject())
            } else {
                board[to] = move.subject()
            }

            //ハッシュ値の更新
            //移動前の分をXORして消す
            boardHash = boardHash xor hashSeed[move.subject()][from]
            //移動後の分をXOR
            boardHash = if (move.isPromote()) {
                boardHash xor hashSeed[promote(move.subject())][to]
            } else {
                boardHash xor hashSeed[move.subject()][to]
            }
        }

        //玉を動かす手ならblack_king_pos,white_king_posに反映
        if (kind(move.subject()) == KING) {
            kingSq[color] = move.to()
        }

        //手番の更新
        color = if (color == BLACK) WHITE else BLACK

        //手数の更新
        turnNumber++

        //棋譜に指し手を追加
        kifu.add(move)

        //王手
        isChecked = isThereControl(color2oppositeColor(color), kingSq[color])

        //hashの手番要素を更新
        hashValue = boardHash xor handHash
        //1bit目を0にする
        hashValue = hashValue and (1).toLong().inv()
        //手番が先手だったら1bitは0のまま,後手だったら1bit目は1になる
        hashValue = hashValue or color.toLong()

        //合法手生成のフラグを降ろす
        alreadyGeneratedMoves = false
    }

    fun undo() {
        val lastMove = lastMove()
        if (lastMove == NULL_MOVE) {
            return
        }

        kifu.removeLast()

        //手番を戻す(このタイミングでいいかな?)
        color = if (color == BLACK) WHITE else BLACK

        val to = lastMove.to().ordinal
        val from = lastMove.from().ordinal

        //動かした駒を消す
        board[to] = EMPTY

        //盤の状態を戻す
        if (lastMove.isDrop()) { //打つ手

            //持ち駒を増やす
            hand[color].add(kind(lastMove.subject()))

            //ハッシュ値の巻き戻し
            //戻す前のHandHashとXOR
            handHash =
                handHash xor handHashSeed[color][kind(lastMove.subject())][hand[color].num(kind(lastMove.subject())) - 1]
            //戻す前の分をXORして消す
            boardHash = boardHash xor hashSeed[lastMove.subject()][to]
            //戻した後のHandHashとXOR
            handHash =
                handHash xor handHashSeed[color][kind(lastMove.subject())][hand[color].num(kind(lastMove.subject()))]
        } else { //盤上の駒を動かす手
            //取る手だったらtoに取った駒を戻し、持ち駒を減らす
            if (lastMove.capture() != EMPTY) {
                board[to] = lastMove.capture()
                hand[color].sub(kind(lastMove.capture()))

                //ハッシュ値の巻き戻し
                //取る前の分のハッシュをXOR
                boardHash = boardHash xor hashSeed[lastMove.capture()][to]
                //増える前の持ち駒の分
                handHash =
                    handHash xor handHashSeed[color][lastMove.capture() and PIECE_KIND_MASK][hand[color].num(
                        kind(lastMove.capture()))]
                //増えた後の持ち駒の分XORして消す
                handHash =
                    handHash xor handHashSeed[color][lastMove.capture() and PIECE_KIND_MASK][hand[color].num(
                        kind(lastMove.capture())) + 1]
            }

            //動いた駒をfromに戻す
            board[from] = lastMove.subject()

            //ハッシュ値の巻き戻し
            //移動前の分をXOR
            boardHash = boardHash xor hashSeed[lastMove.subject()][from]
            //移動後の分をXORして消す
            boardHash = if (lastMove.isPromote()) {
                boardHash xor hashSeed[promote(lastMove.subject())][to]
            } else {
                boardHash xor hashSeed[lastMove.subject()][to]
            }
        }

        //玉を動かす手ならking_sq_に反映
        if (kind(lastMove.subject()) == KING) kingSq[color] = lastMove.from()

        //ハッシュの更新
        hashValue = boardHash xor handHash
        //一番右のbitを0にする
        hashValue = hashValue and (1).toLong().inv()
        //一番右のbitが先手番だったら0のまま、後手番だったら1になる
        hashValue = hashValue or (color).toLong()

        //手数
        turnNumber--

        //王手判定は重そうなのでstackから取る
        isChecked = stack.last().isChecked

        //Stack更新
        stack.removeLast()

        //合法手生成のフラグを降ろす
        alreadyGeneratedMoves = false
    }

    //合法性に関する関数
    fun isLegalMove(move: Move): Boolean {
        return move in generateAllMoves()
    }

    fun isLastMoveDropPawn(): Boolean {
        return lastMove().isDrop() && kind(lastMove().subject()) == PAWN
    }

    fun canWinDeclare(): Boolean {
        //手番側が入玉宣言できるかどうか
        //WCSC29のルールに準拠
        //1. 宣言側の手番である
        //   手番側で考えるのでこれは自明
        //2. 宣言側の玉が敵陣三段目以内に入っている
        val rank = SquareToRank[kingSq[color].ordinal]
        if ((color == BLACK && rank > Rank.Rank3) || (color == WHITE && rank < Rank.Rank7)) {
            return false
        }

        //5. 宣言側の玉に王手がかかっていない
        if (isChecked) {
            return false
        }
        //6. 宣言側の持ち時間が残っている
        //   これは自明なものとする

        //3. 宣言側が大駒5点小駒1点で計算して
        //   ・先手の場合28点以上の持点がある
        //   ・後手の場合27点以上の持点がある
        //   ・点数の対象となるのは、宣言側の持駒と敵陣三段目以内に存在する玉を除く宣言側の駒のみである
        //4. 宣言側の敵陣三段目以内の駒は、玉を除いて10枚以上存在する
        val scoreTable = arrayOf(0, 1, 1, 1, 1, 1, 5, 5)
        val lowerBound = if (color == BLACK) Rank.Rank1 else Rank.Rank7
        val upperBound = if (color == BLACK) Rank.Rank3 else Rank.Rank9
        var score = 0
        var num = 0
        for (r in lowerBound.ordinal..upperBound.ordinal) {
            for (f in FILE.File1.ordinal..FILE.File9.ordinal) {
                val p = board[FRToSquare[f][r].ordinal]
                if (pieceToColor(p) != color || kind(p) == KING) {
                    continue
                }
                score += scoreTable[kind(p)]
                num++
            }
        }

        //持ち駒
        for (p in PAWN..ROOK) {
            score += scoreTable[p] * hand[color].num(p)
        }

        val threshold = if (color == BLACK) 28 else 27
        return (score >= threshold && num >= 10)
    }

    //詰み探索中に枝刈りして良いかを判定
    //王手がかかっていないときは枝刈りして良いだろう
    fun canSkipMateSearch(): Boolean {
        return !isChecked
    }

    //この局面が詰み、千日手等で終わっているか確認する関数
    //終わっている場合は手番側から見た点数を引数に書き込んでtrueを返す
    fun getFinishStatus(): Int {
        val moveList = generateAllMoves()
        if (moveList.isEmpty()) {
            return if (isLastMoveDropPawn()) WIN else LOSE
        } else if (canWinDeclare()) {
            return WIN
        }

        //まずこの局面が1回現れているので1からスタート
        var num = 1

        //今の局面と同一に成りうるのは、最短で4手前
        //そこから2手ずつ可能性としてあり得る
        for (index in stack.size - 4 downTo 0 step 2) {
            if (boardHash != stack[index].boardHash || handHash != stack[index].handHash) {
                //ハッシュ値が一致しなかったら関係ない
                continue
            }

            if (++num == 4) {
                //千日手成立
                //どちらかの手が全て王手かどうか確認する
                val allCheck = arrayOf(true, true)
                var c = color
                for (i in index..stack.size) {
                    allCheck[c] = allCheck[c] && stack[index].isChecked
                    c = color2oppositeColor(c)
                }

                return when {
                    allCheck[c] -> WIN                        //手番側が全て王手された
                    allCheck[color2oppositeColor(c)] -> LOSE  //手番側が全て王手した
                    else -> DRAW                              //普通の千日手
                }
            }
        }
        return NOT_FINISHED
    }

    //特徴量作成
    fun makeFeature(): Array<Float> {
        val features = Array(SQUARE_NUM * INPUT_CHANNEL_NUM) { 0.0f }

        //盤上の駒の特徴量
        for (i in 0 until PieceList.size) {
            //いま考慮している駒
            val t = if (color == BLACK) PieceList[i] else piece2oppositeColorPiece(PieceList[i])

            //各マスについてそこにあるなら1,ないなら0とする
            for (sq in SquareList) {
                //後手のときは盤面を180度反転させる
                val p = if (color == BLACK) board[sq.ordinal] else board[InvSquare[sq.ordinal].ordinal]
                features[i * SQUARE_NUM + SquareToNum[sq.ordinal]] = if (t == p) 1.0f else 0.0f
            }
        }

        //持ち駒の特徴量:最大枚数で割って正規化する
        val colors = arrayListOf(
            arrayListOf(BLACK, WHITE),
            arrayListOf(WHITE, BLACK),
        )
        val handPieces = arrayListOf(PAWN, LANCE, KNIGHT, SILVER, GOLD, BISHOP, ROOK)
        val maxNum = arrayListOf(18.0f, 4.0f, 4.0f, 4.0f, 4.0f, 2.0f, 2.0f)
        var i = PieceList.size
        for (c in colors[color]) {
            for (j in 0 until handPieces.size) {
                for (sq in SquareList) {
                    features[i * SQUARE_NUM + SquareToNum[sq.ordinal]] = hand[c].num(handPieces[j]) / maxNum[j]
                }
                i++
            }
        }

        return features
    }

    //toとfromしか与えられない状態から完全なMoveに変換する関数
    fun transformValidMove(move: Move): Move {
        //stringToMoveではどっちの手番かがわからない
        //つまりsubjectが完全には入っていないので手番付きの駒を入れる
        return if (move.isDrop()) {
            dropMove(
                move.to(),
                if (color == BLACK) toBlack(move.subject()) else toWhite(move.subject())
            )
        } else {
            return Move(
                move.to(),
                move.from(),
                false,
                move.isPromote(),
                board[move.from().ordinal],
                board[move.to().ordinal]
            )
        }
    }

    //合法手生成
    fun generateAllMoves(): ArrayList<Move> {
        if (alreadyGeneratedMoves) {
            return moves
        }
        moves.clear()
        for (sq in SquareList) {
            if (board[sq.ordinal] == EMPTY) {
                //ここに打つ手が可能

                //歩を打つ手
                if (hand[color].num(PAWN) > 0 && puttablePawnLanceKnight(sq, 1)) {
                    //2歩の除外
                    var ok = true
                    val file = SquareToFile[sq.ordinal].ordinal
                    for (rank in Rank.Rank1.ordinal..Rank.Rank9.ordinal) {
                        if (board[FRToSquare[file][rank].ordinal] == coloredPiece(color, PAWN)) {
                            ok = false
                            break
                        }
                    }
                    if (ok) {
                        pushMove(dropMove(sq, coloredPiece(color, PAWN)), moves)
                    }
                }

                //香車
                //最奥の段は除外する
                if (hand[color].num(LANCE) > 0 && puttablePawnLanceKnight(sq, 1)) {
                    pushMove(dropMove(sq, coloredPiece(color, LANCE)), moves)
                }

                //桂馬
                //奥の2段は除外する
                if (hand[color].num(KNIGHT) > 0 && puttablePawnLanceKnight(sq, 2)) {
                    pushMove(dropMove(sq, coloredPiece(color, KNIGHT)), moves)
                }

                //その他
                for (p in arrayListOf(SILVER, GOLD, BISHOP, ROOK)) {
                    if (hand[color].num(p) > 0) {
                        pushMove(dropMove(sq, coloredPiece(color, p)), moves)
                    }
                }
            } else if (pieceToColor(board[sq.ordinal]) == color) {
                //この駒を動かす
                val toList = movableSquareList(sq, board[sq.ordinal])
                for (to in toList) {
                    val move = Move(
                        to, sq,
                        isDrop = false,
                        isPromote = false,
                        subject = board[sq.ordinal],
                        capture = board[to.ordinal])
                    pushMove(move, moves)
                }
            }
        }
        return moves
    }

    private fun puttablePawnLanceKnight(sq: Square, forbiddenWidth: Int): Boolean {
        return color == BLACK && (SquareToRank[sq.ordinal].ordinal >= Rank.Rank1.ordinal + forbiddenWidth)
                || color == WHITE && (SquareToRank[sq.ordinal].ordinal <= Rank.Rank9.ordinal - forbiddenWidth)
    }

    //sfenの入出力
    fun fromStr(sfen: String) {
        //初期化
        for (i in 0 until Square.SquareNum.ordinal) {
            board[i] = WALL
        }
        for (sq in SquareList) {
            board[sq.ordinal] = EMPTY
        }

        val split = sfen.split(' ')
        //SFENは "盤面 手番 持ち駒 手数"(http://shogidokoro.starfree.jp/usi.html)
        if (split.size != 4) {
            init()
            return
        }

        val (strBoard, strTurn, strHand, strTurnNum) = split

        //盤上の設定
        val strs = strBoard.split('/')
        if (strs.size != BOARD_WIDTH) {
            init()
            return
        }
        //各段を処理
        for (i in strs.indices) {
            val r = Rank.Rank1.ordinal + i
            var f = FILE.File9.ordinal
            var promote = false
            for (j in strs[i].indices) {
                when (val c = strs[i][j]) {
                    in '1'..'9' -> { f -= c - '0' }
                    '+' -> { promote = true }
                    else -> {
                        val piece = sfenCharToPiece[c]
                        if (piece == null) {
                            init()
                            return
                        }

                        //玉だったら位置を設定
                        if (kind(piece) == KING) {
                            kingSq[pieceToColor(piece)] = FRToSquare[f][r]
                        }

                        //文字が示す駒をboard_に設置
                        board[FRToSquare[f--][r].ordinal] = if (promote) promote(piece) else piece

                        //成のフラグを降ろす
                        promote = false
                    }
                }
            }
        }

        //手番の設定
        color = when (strTurn) {
            "b" -> BLACK
            "w" -> WHITE
            else -> {
                init()
                return
            }
        }

        //持ち駒
        hand[BLACK].clear()
        hand[WHITE].clear()
        var num = 1
        var i = 0
        while (i < strHand.length) {
            if (strHand[i] == '-') {
                break
            }
            if (strHand[i] in '1'..'9') { //数字なら枚数の取得
                if (strHand[i + 1] in '0'..'9') {
                    //次の文字も数字の場合が一応あるはず(歩が10枚以上)
                    //charをいったんStringにしてからIntにする
                    num = 10 * strHand[i].toString().toInt() + strHand[i + 1].toString().toInt()
                    i += 2
                } else {
                    //次が数字じゃないなら普通に取る
                    num = strHand[i++].toString().toInt()
                }
            } else { //駒なら持ち駒を変更
                val piece = sfenCharToPiece[strHand[i++]]
                if (piece == null) {
                    init()
                    return
                }
                hand[pieceToColor(piece)].set(kind(piece), num)

                //枚数を1に戻す
                num = 1
            }
        }

        //手数
        val turnNumberOrNull = strTurnNum.toIntOrNull()
        if (turnNumberOrNull == null) {
            init()
            return
        }
        turnNumber = turnNumberOrNull

        //ハッシュ値の初期化
        initHashValue()

        alreadyGeneratedMoves = false

        //王手の確認
        isChecked = isThereControl(color2oppositeColor(color), kingSq[color])

        stack.clear()
        kifu.clear()
    }

    fun toStr(): String {
        var result = String()
        for (r in Rank.Rank1.ordinal..Rank.Rank9.ordinal) {
            var emptyNum = 0
            for (f in FILE.File9.ordinal downTo FILE.File1.ordinal) {
                if (board[FRToSquare[f][r].ordinal] == EMPTY) {
                    emptyNum++
                } else {
                    //まずこのマスまでの空白マスを処理
                    result += if (emptyNum == 0) "" else emptyNum.toString()

                    //駒を処理
                    result += PieceToSfenStrWithoutSpace[board[FRToSquare[f][r].ordinal]]

                    //空白マスを初期化
                    emptyNum = 0
                }
            }

            //段最後の空白マスを処理
            result += if (emptyNum == 0) "" else emptyNum.toString()

            if (r < Rank.Rank9.ordinal) {
                result += "/"
            }
        }

        //手番
        result += if (color == BLACK) " b " else " w "

        //持ち駒
        var all0 = true
        for (c in BLACK..WHITE) {
            for (p in ROOK downTo PAWN) {
                if (hand[c].num(p) == 1) {
                    result += PieceToSfenStrWithoutSpace[coloredPiece(c, p)]
                    all0 = false
                } else if (hand[c].num(p) >= 2) {
                    result += hand[c].num(p).toString()
                    result += PieceToSfenStrWithoutSpace[coloredPiece(c, p)]
                    all0 = false
                }
            }
        }

        if (all0) {
            result += "-"
        }

        result += " $turnNumber"

        return result
    }

    //getter
    fun on(sq: Square): Int {
        return board[sq.ordinal]
    }

    //--------------------
    //    内部メソッド
    //--------------------
    private fun addSquare(sq: Square, color: Int, list: ArrayList<Square>) {
        if (board[sq.ordinal] == WALL || pieceToColor(board[sq.ordinal]) == color) {
            return
        }
        list.add(sq)
    }

    private fun movableSquareList(sq: Square, color: Int, dir: Int): ArrayList<Square> {
        val list = ArrayList<Square>()
        var currSquare = SquareListWithWALL[sq.ordinal + dir]
        while (true) {
            if (board[currSquare.ordinal] == EMPTY) {
                //継続
                list.add(currSquare)
                currSquare = SquareListWithWALL[currSquare.ordinal + dir]
            } else if (board[currSquare.ordinal] == WALL || pieceToColor(board[currSquare.ordinal]) == color) {
                //壁や自分側の駒の位置には突っ込めない
                break
            } else {
                //相手の駒がある場合はここまで良し
                list.add(currSquare)
                break
            }
        }
        return list
    }

    private fun movableSquareList(sq: Square, piece: Int): ArrayList<Square> {
        //現状態について、sqマスにpieceがあると仮定したときに動かせるマスのリストを返す
        val list = ArrayList<Square>()
        when (piece) {
            BLACK_PAWN -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_U], BLACK, list)
            }
            BLACK_LANCE -> {
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_U))
            }
            BLACK_KNIGHT -> {
                if (!isWithinXRankFromTheBack(sq, BLACK, 2)) {
                    addSquare(SquareListWithWALL[sq.ordinal + DIR_RUU], BLACK, list)
                    addSquare(SquareListWithWALL[sq.ordinal + DIR_LUU], BLACK, list)
                }
            }
            BLACK_SILVER -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_U], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RU], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RD], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LD], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LU], BLACK, list)
            }
            BLACK_GOLD,
            BLACK_PAWN_PROMOTE,
            BLACK_LANCE_PROMOTE,
            BLACK_KNIGHT_PROMOTE,
            BLACK_SILVER_PROMOTE -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_U], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RU], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_R], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_D], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_L], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LU], BLACK, list)
            }
            WHITE_PAWN -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_D], WHITE, list)
            }
            WHITE_LANCE -> {
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_D))
            }
            WHITE_KNIGHT -> {
                if (!isWithinXRankFromTheBack(sq, WHITE, 2)) {
                    addSquare(SquareListWithWALL[sq.ordinal + DIR_RDD], WHITE, list)
                    addSquare(SquareListWithWALL[sq.ordinal + DIR_LDD], WHITE, list)
                }
            }
            WHITE_SILVER -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_D], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LD], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LU], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RU], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RD], WHITE, list)
            }
            WHITE_GOLD,
            WHITE_PAWN_PROMOTE,
            WHITE_LANCE_PROMOTE,
            WHITE_KNIGHT_PROMOTE,
            WHITE_SILVER_PROMOTE -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_D], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LD], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_L], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_U], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_R], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RD], WHITE, list)
            }
            BLACK_BISHOP,
            WHITE_BISHOP -> {
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_RU))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_RD))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_LD))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_LU))
            }
            BLACK_BISHOP_PROMOTE,
            WHITE_BISHOP_PROMOTE -> {
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_RU))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_RD))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_LD))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_LU))
                addSquare(SquareListWithWALL[sq.ordinal + DIR_U], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_R], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_D], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_L], pieceToColor(piece), list)
            }
            BLACK_ROOK,
            WHITE_ROOK -> {
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_U))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_R))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_D))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_L))
            }
            BLACK_ROOK_PROMOTE,
            WHITE_ROOK_PROMOTE -> {
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_U))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_R))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_D))
                list.addAll(movableSquareList(sq, pieceToColor(piece), DIR_L))
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RU], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RD], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LD], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LU], pieceToColor(piece), list)
            }
            BLACK_KING,
            WHITE_KING -> {
                addSquare(SquareListWithWALL[sq.ordinal + DIR_U], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RU], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_R], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RD], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_D], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LD], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_L], pieceToColor(piece), list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LU], pieceToColor(piece), list)
            }
            else -> {
            }
        }
        return list
    }

    private fun isThereControl(color: Int, sq: Square): Boolean {
        //color側の利きがsqマスへあるかどうか
        //sqマスからcolorとは逆側の手番として利きを生成
        val oppColor = color2oppositeColor(color)
        val pieceList = arrayListOf(
            PAWN,
            LANCE,
            KNIGHT,
            SILVER,
            GOLD,
            BISHOP,
            ROOK,
            KING,
            PAWN_PROMOTE,
            LANCE_PROMOTE,
            KNIGHT_PROMOTE,
            SILVER_PROMOTE,
            BISHOP_PROMOTE,
            ROOK_PROMOTE
        )
        for (piece in pieceList) {
            for (s in movableSquareList(sq, coloredPiece(oppColor, piece))) {
                if (board[s.ordinal] == coloredPiece(color, piece)) {
                    return true
                }
            }
        }
        return false
    }

    //合法手生成で用いる関数
    private fun canPromote(move: Move): Boolean {
        val to = move.to().ordinal
        val from = move.from().ordinal
        if (move.isDrop()                        //打つ手だったらダメ
            || board[from] and PROMOTE != 0     //すでに成っている駒を動かす手だったらダメ
            || kind(board[from]) == GOLD        //動かす駒が金だったらダメ
            || kind(board[from]) == KING        //動かす駒が玉だったらダメ
        ) {
            return false
        }

        //位置関係
        return if (color == BLACK) {
            ((Rank.Rank1 <= SquareToRank[to] && SquareToRank[to] <= Rank.Rank3) ||
                    (Rank.Rank1 <= SquareToRank[from] && SquareToRank[from] <= Rank.Rank3))
        } else {
            ((Rank.Rank7 <= SquareToRank[to] && SquareToRank[to] <= Rank.Rank9) ||
                    (Rank.Rank7 <= SquareToRank[from] && SquareToRank[from] <= Rank.Rank9))
        }
    }

    private fun pushMove(move: Move, move_buf: ArrayList<Move>) {
        val moveColor = color

        //動かしてみる
        doMove(move)

        //自玉に王手がかかったまま, あるいは動かしたことで王手になるとダメ
        val ok = !isThereControl(color, kingSq[moveColor])

        undo()

        if (!ok) {
            return
        }

        //成る手が可能だったら先に生成
        if (canPromote(move)) {
            move_buf.add(promotiveMove(move))
            val to = move.to().ordinal
            when (kind(board[move.from().ordinal])) {
                //香、桂は位置によっては成る手しか不可
                PAWN, LANCE -> {
                    if (color == BLACK && SquareToRank[to] <= Rank.Rank1) return
                    if (color == WHITE && SquareToRank[to] >= Rank.Rank9) return
                }
                KNIGHT -> {
                    if (color == BLACK && SquareToRank[to] <= Rank.Rank2) return
                    if (color == WHITE && SquareToRank[to] >= Rank.Rank8) return
                }
            }
        }
        //成らない手を追加
        move_buf.add(move)
    }

    //ハッシュ値の初期化
    private fun initHashValue() {
        hashValue = 0
        boardHash = 0
        handHash = 0
        for (sq in SquareList) {
            boardHash = boardHash xor hashSeed[board[sq.ordinal]][sq.ordinal]
        }
        for (piece in PAWN..ROOK) {
            handHash = handHash xor handHashSeed[BLACK][piece][hand[BLACK].num(piece)]
            handHash = handHash xor handHashSeed[WHITE][piece][hand[WHITE].num(piece)]
        }
        hashValue = boardHash xor handHash
        //1bit目を0にする
        hashValue = hashValue and (1).toLong().inv()
        //手番が先手だったら1bitは0のまま,後手だったら1bit目は1になる
        hashValue = hashValue or color.toLong()
    }

    //emptyの条件分けをいちいち書かないための補助関数
    fun lastMove(): Move {
        return if (kifu.size == 0) NULL_MOVE else kifu.last()
    }
}