package com.example.miacisshogi

import kotlin.jvm.internal.Ref
import kotlin.random.Random

class Position {
    //コンストラクタ
    constructor() {
        init()
    }

    //初期化
    fun init() {
        //盤上の初期化
        for (i in 0 until Square.SquareNum.ordinal) {
            board_[i] = WALL
        }
        for (sq in SquareList) board_[sq.ordinal] = EMPTY

        //後手の駒
        board_[Square.SQ11.ordinal] = WHITE_LANCE
        board_[Square.SQ21.ordinal] = WHITE_KNIGHT
        board_[Square.SQ31.ordinal] = WHITE_SILVER
        board_[Square.SQ41.ordinal] = WHITE_GOLD
        board_[Square.SQ51.ordinal] = WHITE_KING
        board_[Square.SQ61.ordinal] = WHITE_GOLD
        board_[Square.SQ71.ordinal] = WHITE_SILVER
        board_[Square.SQ81.ordinal] = WHITE_KNIGHT
        board_[Square.SQ91.ordinal] = WHITE_LANCE
        board_[Square.SQ22.ordinal] = WHITE_BISHOP
        board_[Square.SQ82.ordinal] = WHITE_ROOK
        board_[Square.SQ13.ordinal] = WHITE_PAWN
        board_[Square.SQ23.ordinal] = WHITE_PAWN
        board_[Square.SQ33.ordinal] = WHITE_PAWN
        board_[Square.SQ43.ordinal] = WHITE_PAWN
        board_[Square.SQ53.ordinal] = WHITE_PAWN
        board_[Square.SQ63.ordinal] = WHITE_PAWN
        board_[Square.SQ73.ordinal] = WHITE_PAWN
        board_[Square.SQ83.ordinal] = WHITE_PAWN
        board_[Square.SQ93.ordinal] = WHITE_PAWN

        //先手の駒
        board_[Square.SQ19.ordinal] = BLACK_LANCE
        board_[Square.SQ29.ordinal] = BLACK_KNIGHT
        board_[Square.SQ39.ordinal] = BLACK_SILVER
        board_[Square.SQ49.ordinal] = BLACK_GOLD
        board_[Square.SQ59.ordinal] = BLACK_KING
        board_[Square.SQ69.ordinal] = BLACK_GOLD
        board_[Square.SQ79.ordinal] = BLACK_SILVER
        board_[Square.SQ89.ordinal] = BLACK_KNIGHT
        board_[Square.SQ99.ordinal] = BLACK_LANCE
        board_[Square.SQ88.ordinal] = BLACK_BISHOP
        board_[Square.SQ28.ordinal] = BLACK_ROOK
        board_[Square.SQ17.ordinal] = BLACK_PAWN
        board_[Square.SQ27.ordinal] = BLACK_PAWN
        board_[Square.SQ37.ordinal] = BLACK_PAWN
        board_[Square.SQ47.ordinal] = BLACK_PAWN
        board_[Square.SQ57.ordinal] = BLACK_PAWN
        board_[Square.SQ67.ordinal] = BLACK_PAWN
        board_[Square.SQ77.ordinal] = BLACK_PAWN
        board_[Square.SQ87.ordinal] = BLACK_PAWN
        board_[Square.SQ97.ordinal] = BLACK_PAWN

        //持ち駒
        hand_[BLACK].clear()
        hand_[WHITE].clear()

        //手番
        color_ = BLACK

        //手数
        turn_number_ = 1

        //玉の位置
        king_sq_[BLACK] = Square.SQ59
        king_sq_[WHITE] = Square.SQ51

        //ハッシュ値の初期化
        initHashValue()

        stack_.clear()
        kifu_.clear()

        is_checked_ = false

        //合法手生成のフラグを降ろす
        already_generated_moves_ = false
    }

    //内部の状態等を表示する関数
    fun print() {
        //盤上
        println("９８７６５４３２１")
        println("------------------")
        for (r in Rank.Rank1.ordinal..Rank.Rank9.ordinal) {
            for (f in File.File9.ordinal..File.File1.ordinal) {
                print(PieceToSfenStrWithSpace[board_[FRToSquare[f][r].ordinal]])
            }
            println("|${r}")
        }

        //持ち駒
        println("持ち駒")
        println("先手:")
        hand_[BLACK].print()
        println("後手:")
        hand_[WHITE].print()

        //その他
        println("手番:${if (color_ == BLACK) "先手" else "後手"}")
        println("手数:${turn_number_}")
        if (!kifu_.isEmpty()) {
            println("最後の手:${lastMove().toPrettyStr()}")
        }
        println("ハッシュ値:${hash_value_}")
    }

    //一手進める・戻す関数
    fun doMove(move: Move) {
//        Log.d("doMove", move.toPrettyStr())

        //動かす前の状態を残しておく
        stack_.add(StateInfo(this))

        val to = move.to().ordinal
        val from = move.from().ordinal

        //実際に動かす
        if (move.isDrop()) { //持ち駒を打つ手

            //持ち駒を減らす
            hand_[color_].sub(kind(move.subject()))

            //移動先にsubjectを設置
            board_[to] = move.subject()

            //ハッシュ値の更新
            //打つ前のHandHashとXORして消す
            val num = hand_[color_].num(kind(move.subject()))
            hand_hash_ = hand_hash_ xor handHashSeed[color_][kind(move.subject())][num + 1]
            //打った後のHandHashとXOR
            hand_hash_ = hand_hash_ xor handHashSeed[color_][kind(move.subject())][num]
            //打った後の分をXOR
            board_hash_ = board_hash_ xor hashSeed[move.subject()][to]

        } else { //盤上の駒を動かす手

            //移動する駒を消す
            board_[from] = EMPTY

            //取った駒があるならその駒を消し、持ち駒を増やす
            if (move.capture() != EMPTY) {
                //取った駒を消す
                board_[to] = EMPTY

                //持ち駒を増やす
                hand_[color_].add(kind(move.capture()))

                //ハッシュ値の更新
                //取った駒分のハッシュをXOR
                board_hash_ = board_hash_ xor hashSeed[move.capture()][to]
                //増える前の持ち駒の分をXORして消す
                hand_hash_ =
                    hand_hash_ xor handHashSeed[color_][kind(move.capture())][hand_[color_].num(
                        kind(move.capture())
                    ) - 1]
                //増えた後の持ち駒の分XOR
                hand_hash_ =
                    hand_hash_ xor handHashSeed[color_][kind(move.capture())][hand_[color_].num(
                        kind(move.capture())
                    )]
            }

            //成る手ならsubjectに成りのフラグを立てて,そうでないならsubjectをそのまま移動先に設置
            if (move.isPromote()) {
                board_[to] = promote(move.subject())
            } else {
                board_[to] = move.subject()
            }

            //ハッシュ値の更新
            //移動前の分をXORして消す
            board_hash_ = board_hash_ xor hashSeed[move.subject()][from]
            //移動後の分をXOR
            if (move.isPromote()) {
                board_hash_ = board_hash_ xor hashSeed[promote(move.subject())][to]
            } else {
                board_hash_ = board_hash_ xor hashSeed[move.subject()][to]
            }
        }

        //玉を動かす手ならblack_king_pos,white_king_posに反映
        if (kind(move.subject()) == KING) {
            king_sq_[color_] = move.to()
        }

        //手番の更新
        color_ = if (color_ == BLACK) WHITE else BLACK

        //手数の更新
        turn_number_++

        //棋譜に指し手を追加
        kifu_.add(move)

        //王手
        is_checked_ = false

        //hashの手番要素を更新
        hash_value_ = board_hash_ xor hand_hash_
        //1bit目を0にする
        hash_value_ = hash_value_ and (1).toLong().inv()
        //手番が先手だったら1bitは0のまま,後手だったら1bit目は1になる
        hash_value_ = hash_value_ or color_.toLong()

        //合法手生成のフラグを降ろす
        already_generated_moves_ = false
    }

    fun undo() {
        val last_move = kifu_.last()
        kifu_.removeLast()

        //手番を戻す(このタイミングでいいかな?)
        color_ = if (color_ == BLACK) WHITE else BLACK

        val to = last_move.to().ordinal
        val from = last_move.from().ordinal

        //動かした駒を消す
        board_[to] = EMPTY

        //盤の状態を戻す
        if (last_move.isDrop()) { //打つ手

            //持ち駒を増やす
            hand_[color_].add(kind(last_move.subject()))

            //ハッシュ値の巻き戻し
            //戻す前のHandHashとXOR
            hand_hash_ =
                hand_hash_ xor handHashSeed[color_][kind(last_move.subject())][hand_[color_].num(kind(last_move.subject())) - 1]
            //戻す前の分をXORして消す
            board_hash_ = board_hash_ xor hashSeed[last_move.subject()][to]
            //戻した後のHandHashとXOR
            hand_hash_ =
                hand_hash_ xor handHashSeed[color_][kind(last_move.subject())][hand_[color_].num(kind(last_move.subject()))]
        } else { //盤上の駒を動かす手
            //取る手だったらtoに取った駒を戻し、持ち駒を減らす
            if (last_move.capture() != EMPTY) {
                board_[to] = last_move.capture()
                hand_[color_].sub(kind(last_move.capture()))

                //ハッシュ値の巻き戻し
                //取る前の分のハッシュをXOR
                board_hash_ = board_hash_ xor hashSeed[last_move.capture()][to]
                //増える前の持ち駒の分
                hand_hash_ =
                    hand_hash_ xor handHashSeed[color_][last_move.capture() and PIECE_KIND_MASK][hand_[color_].num(
                        kind(last_move.capture()))]
                //増えた後の持ち駒の分XORして消す
                hand_hash_ =
                    hand_hash_ xor handHashSeed[color_][last_move.capture() and PIECE_KIND_MASK][hand_[color_].num(
                        kind(last_move.capture())) + 1]
            }

            //動いた駒をfromに戻す
            board_[from] = last_move.subject()

            //ハッシュ値の巻き戻し
            //移動前の分をXOR
            board_hash_ = board_hash_ xor hashSeed[last_move.subject()][from]
            //移動後の分をXORして消す
            if (last_move.isPromote()) {
                board_hash_ = board_hash_ xor hashSeed[promote(last_move.subject())][to]
            } else {
                board_hash_ = board_hash_ xor hashSeed[last_move.subject()][to]
            }
        }

        //玉を動かす手ならking_sq_に反映
        if (kind(last_move.subject()) == KING) king_sq_[color_] = last_move.from()

        //ハッシュの更新
        hash_value_ = board_hash_ xor hand_hash_
        //一番右のbitを0にする
        hash_value_ = hash_value_ and (1).toLong().inv()
        //一番右のbitが先手番だったら0のまま、後手番だったら1になる
        hash_value_ = hash_value_ or (color_).toLong()

        //手数
        turn_number_--

        //Stack更新
        stack_.removeLast()

        //合法手生成のフラグを降ろす
        already_generated_moves_ = false
    }

    //合法性に関する関数
    fun isLegalMove(move: Move): Boolean {
        return move in generateAllMoves()
    }

    fun isLastMoveDropPawn(): Boolean {
        return kind(lastMove().subject()) == PAWN
    }

    fun canWinDeclare(): Boolean {
        return false
    }

    //詰み探索中に枝刈りして良いかを判定
    //王手がかかっていないときは枝刈りして良いだろう
    fun canSkipMateSearch(): Boolean {
        return !is_checked_
    }

    //この局面が詰み、千日手等で終わっているか確認する関数
    //終わっている場合は手番側から見た点数を引数に書き込んでtrueを返す
    fun isFinish(score: Ref.FloatRef, check_repeat: Boolean = true): Boolean {
        return false
    }

    //千日手の判定
    fun isRepeating(score: Ref.FloatRef): Boolean {
        return false
    }

    //特徴量作成
    fun makeFeature(): Array<Float> {
        val features = Array<Float>(SQUARE_NUM * INPUT_CHANNEL_NUM) { 0.0f }

        //盤上の駒の特徴量
        for (i in 0 until PieceList.size) {
            //いま考慮している駒
            val t = if (color_ == BLACK) PieceList[i] else piece2oppositeColorPiece(PieceList[i])

            //各マスについてそこにあるなら1,ないなら0とする
            for (sq in SquareList) {
                //後手のときは盤面を180度反転させる
                val p = if (color_ == BLACK) board_[sq.ordinal] else board_[InvSquare[sq.ordinal].ordinal]
                features[i * SQUARE_NUM + SquareToNum[sq.ordinal]] = if (t == p) 1.0f else 0.0f
            }
        }

        //持ち駒の特徴量:最大枚数で割って正規化する
        val colors = arrayListOf(
            arrayListOf(BLACK, WHITE),
            arrayListOf(WHITE, BLACK),
        )
        val HAND_PIECE_NUM = 7
        val HAND_PIECES = arrayListOf(PAWN, LANCE, KNIGHT, SILVER, GOLD, BISHOP, ROOK)
        val MAX_NUMS = arrayListOf(18.0f, 4.0f, 4.0f, 4.0f, 4.0f, 2.0f, 2.0f)
        var i = PieceList.size
        for (c in colors[color_]) {
            for (j in 0 until HAND_PIECE_NUM) {
                for (sq in SquareList) {
                    features[i * SQUARE_NUM + SquareToNum[sq.ordinal]] = hand_[c].num(HAND_PIECES[j]) / MAX_NUMS[j]
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
                if (color_ == BLACK) toBlack(move.subject()) else toWhite(move.subject())
            )
        } else {
            return Move(
                move.to(),
                move.from(),
                false,
                move.isPromote(),
                board_[move.from().ordinal],
                board_[move.to().ordinal]
            )
        }
    }

    //合法手生成
    fun generateAllMoves(): ArrayList<Move> {
        val moveList = ArrayList<Move>()
        for (sq in SquareList) {
            if (board_[sq.ordinal] == EMPTY) {
                //ここに打つ手が可能

                //歩を打つ手
                if (hand_[color_].num(PAWN) > 0) {
                    //最奥の段は除外する
                    if (color_ == BLACK && SquareToRank[sq.ordinal] <= Rank.Rank8 ||
                        color_ == WHITE && SquareToRank[sq.ordinal] >= Rank.Rank2) {

                        //2歩の除外
                        var ok = true
                        val file = SquareToFile[sq.ordinal].ordinal
                        for (rank in Rank.Rank1.ordinal..Rank.Rank9.ordinal) {
                            if (board_[FRToSquare[file][rank].ordinal] == coloredPiece(color_, PAWN)) {
                                ok = false
                                break
                            }
                        }
                        if (ok) {
                            pushMove(dropMove(sq, coloredPiece(color_, PAWN)), moveList)
                        }
                    }
                }

                //香車
                //最奥の段は除外する
                if (hand_[color_].num(LANCE) > 0) {
                    if (color_ == BLACK && SquareToRank[sq.ordinal] <= Rank.Rank8 ||
                        color_ == WHITE && SquareToRank[sq.ordinal] >= Rank.Rank2) {
                        pushMove(dropMove(sq, coloredPiece(color_, LANCE)), moveList)
                    }
                }

                //桂馬
                //奥の2段は除外する
                if (hand_[color_].num(KNIGHT) > 0) {
                    if (color_ == BLACK && SquareToRank[sq.ordinal] <= Rank.Rank7 ||
                        color_ == WHITE && SquareToRank[sq.ordinal] >= Rank.Rank3) {
                        pushMove(dropMove(sq, coloredPiece(color_, KNIGHT)), moveList)
                    }
                }

                //その他
                for (p in arrayListOf(SILVER, GOLD, BISHOP, ROOK)) {
                    if (hand_[color_].num(p) > 0) {
                        pushMove(dropMove(sq, coloredPiece(color_, p)), moveList)
                    }
                }
            } else if (pieceToColor(board_[sq.ordinal]) == color_) {
                //この駒を動かす
                val toList = movableSquareList(sq, board_[sq.ordinal])
                for (to in toList) {
                    val move = Move(to, sq, false, false, board_[sq.ordinal], board_[to.ordinal])
                    pushMove(move, moveList)
                }
            }
        }
        return moveList
    }

    //sfenの入出力
    fun fromStr(sfen: String) {}
    fun toStr(): String {
        return "todo impl"
    }

    //ハッシュ
    fun initHashSeed() {}

    //getter
    fun turnNumber(): Int {
        return turn_number_
    }

    fun color(): Int {
        return color_
    }

    fun hashValue(): Long {
        return hash_value_
    }

    fun on(sq: Square): Int {
        return board_[sq.ordinal]
    }

    fun isChecked(): Boolean {
        return is_checked_
    }

    //--------------------
    //    内部メソッド
    //--------------------
    private fun addSquare(sq: Square, color: Int, list: ArrayList<Square>) {
        if (board_[sq.ordinal] == WALL || pieceToColor(board_[sq.ordinal]) == color) {
            return
        }
        list.add(sq)
    }

    private fun movableSquareList(sq: Square, color: Int, dir: Int): ArrayList<Square> {
        val list = ArrayList<Square>()
        var currSquare = SquareListWithWALL[sq.ordinal + dir]
        while (true) {
            if (board_[currSquare.ordinal] == EMPTY) {
                //継続
                list.add(currSquare)
                currSquare = SquareListWithWALL[currSquare.ordinal + dir]
            } else if (board_[currSquare.ordinal] == WALL || pieceToColor(board_[currSquare.ordinal]) == color) {
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
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RUU], BLACK, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LUU], BLACK, list)
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
                addSquare(SquareListWithWALL[sq.ordinal + DIR_RDD], WHITE, list)
                addSquare(SquareListWithWALL[sq.ordinal + DIR_LDD], WHITE, list)
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
                if (board_[s.ordinal] == coloredPiece(color, piece)) {
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
            || board_[from] and PROMOTE != 0     //すでに成っている駒を動かす手だったらダメ
            || kind(board_[from]) == GOLD        //動かす駒が金だったらダメ
            || kind(board_[from]) == KING        //動かす駒が玉だったらダメ
        ) {
            return false
        }

        //位置関係
        if (color_ == BLACK) {
            return ((Rank.Rank1 <= SquareToRank[to] && SquareToRank[to] <= Rank.Rank3) ||
                    (Rank.Rank1 <= SquareToRank[from] && SquareToRank[from] <= Rank.Rank3))
        } else {
            return ((Rank.Rank7 <= SquareToRank[to] && SquareToRank[to] <= Rank.Rank9) ||
                    (Rank.Rank7 <= SquareToRank[from] && SquareToRank[from] <= Rank.Rank9))
        }
    }

    private fun pushMove(move: Move, move_buf: ArrayList<Move>) {
        val moveColor = color_

        //動かしてみる
        doMove(move)

        //TODO:ここで打ち歩判定も入れるべきだな

        //自玉に王手がかかったまま, あるいは動かしたことで王手になるとダメ
        val ok = !isThereControl(color_, king_sq_[moveColor])

        undo()

        if (!ok) {
            return
        }

        //成る手が可能だったら先に生成
        if (canPromote(move)) {
            move_buf.add(promotiveMove(move))
            val to = move.to().ordinal
            when (kind(board_[move.from().ordinal])) {
                //香、桂は位置によっては成る手しか不可
                LANCE -> {
                    if (color_ == BLACK && SquareToRank[to] <= Rank.Rank1) return
                    if (color_ == WHITE && SquareToRank[to] >= Rank.Rank9) return
                }
                KNIGHT -> {
                    if (color_ == BLACK && SquareToRank[to] <= Rank.Rank2) return
                    if (color_ == WHITE && SquareToRank[to] >= Rank.Rank8) return
                }
            }
        }
        //成らない手を追加
        move_buf.add(move)
    }

    //ハッシュ値の初期化
    fun initHashValue() {}

    //emptyの条件分けをいちいち書かないための補助関数
    fun lastMove(): Move {
        return if (kifu_.size == 0) NULL_MOVE else kifu_.last()
    }

    //------------------
    //    クラス変数
    //------------------
    //ハッシュの各駒・位置に対する決められた値
    private val hashSeed = Array(PieceNum) { Array(Square.SquareNum.ordinal) { Random.nextLong() } }
    private val handHashSeed = Array(ColorNum) { Array(PieceNum) { Array(19) { Random.nextLong() } } }

    //------------------------
    //    インスタンス変数
    //------------------------
    //手番
    private var color_: Int = 0

    //盤面
    private var board_ = Array<Int>(Square.SquareNum.ordinal) { it }

    //持ち駒
    var hand_ = arrayListOf(Hand(), Hand())

    //手数
    var turn_number_: Int = 0

    //玉の位置
    var king_sq_ = arrayListOf(Square.WALL00, Square.WALL00)

    //現局面までの指し手履歴
    var kifu_ = ArrayList<Move>()

    //現局面の合法手
    var moves_ = ArrayList<Move>()

    var already_generated_moves_ = false

    //現局面のハッシュ値
    var hash_value_: Long = 0
    var board_hash_: Long = 0
    var hand_hash_: Long = 0

    var is_checked_: Boolean = false

    class StateInfo(pos: Position) {
        //千日手判定用に必要な情報
        var board_hash = pos.board_hash_
        var hand_hash = pos.hand_hash_
        var hand = pos.hand_
        var is_checked = pos.is_checked_
    }

    var stack_ = ArrayList<StateInfo>()
}