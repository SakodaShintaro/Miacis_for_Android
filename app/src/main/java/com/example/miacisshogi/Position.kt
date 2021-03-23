package com.example.miacisshogi

import kotlin.jvm.internal.Ref

class Position {
    //コンストラクタ
    constructor() {
        init()
    }
    //初期化
    fun init() {
        //盤上の初期化
        for (i in 0 until Square.SquareNum.ordinal) {
            println(i)
            board_[i] = WALL
        }
        for (sq in SquareList) board_[sq.ordinal] = EMPTY;

        //後手の駒
        board_[Square.SQ11.ordinal] = WHITE_LANCE;
        board_[Square.SQ21.ordinal] = WHITE_KNIGHT;
        board_[Square.SQ31.ordinal] = WHITE_SILVER;
        board_[Square.SQ41.ordinal] = WHITE_GOLD;
        board_[Square.SQ51.ordinal] = WHITE_KING;
        board_[Square.SQ61.ordinal] = WHITE_GOLD;
        board_[Square.SQ71.ordinal] = WHITE_SILVER;
        board_[Square.SQ81.ordinal] = WHITE_KNIGHT;
        board_[Square.SQ91.ordinal] = WHITE_LANCE;
        board_[Square.SQ22.ordinal] = WHITE_BISHOP;
        board_[Square.SQ82.ordinal] = WHITE_ROOK;
        board_[Square.SQ13.ordinal] = WHITE_PAWN;
        board_[Square.SQ23.ordinal] = WHITE_PAWN;
        board_[Square.SQ33.ordinal] = WHITE_PAWN;
        board_[Square.SQ43.ordinal] = WHITE_PAWN;
        board_[Square.SQ53.ordinal] = WHITE_PAWN;
        board_[Square.SQ63.ordinal] = WHITE_PAWN;
        board_[Square.SQ73.ordinal] = WHITE_PAWN;
        board_[Square.SQ83.ordinal] = WHITE_PAWN;
        board_[Square.SQ93.ordinal] = WHITE_PAWN;

        //先手の駒
        board_[Square.SQ19.ordinal] = BLACK_LANCE;
        board_[Square.SQ29.ordinal] = BLACK_KNIGHT;
        board_[Square.SQ39.ordinal] = BLACK_SILVER;
        board_[Square.SQ49.ordinal] = BLACK_GOLD;
        board_[Square.SQ59.ordinal] = BLACK_KING;
        board_[Square.SQ69.ordinal] = BLACK_GOLD;
        board_[Square.SQ79.ordinal] = BLACK_SILVER;
        board_[Square.SQ89.ordinal] = BLACK_KNIGHT;
        board_[Square.SQ99.ordinal] = BLACK_LANCE;
        board_[Square.SQ88.ordinal] = BLACK_BISHOP;
        board_[Square.SQ28.ordinal] = BLACK_ROOK;
        board_[Square.SQ17.ordinal] = BLACK_PAWN;
        board_[Square.SQ27.ordinal] = BLACK_PAWN;
        board_[Square.SQ37.ordinal] = BLACK_PAWN;
        board_[Square.SQ47.ordinal] = BLACK_PAWN;
        board_[Square.SQ57.ordinal] = BLACK_PAWN;
        board_[Square.SQ67.ordinal] = BLACK_PAWN;
        board_[Square.SQ77.ordinal] = BLACK_PAWN;
        board_[Square.SQ87.ordinal] = BLACK_PAWN;
        board_[Square.SQ97.ordinal] = BLACK_PAWN;

        //持ち駒
        hand_[BLACK].clear();
        hand_[WHITE].clear();

        //手番
        color_ = BLACK;

        //手数
        turn_number_ = 1;

        //玉の位置
        king_sq_[BLACK] = Square.SQ59;
        king_sq_[WHITE] = Square.SQ51;

        //ハッシュ値の初期化
        initHashValue();

        stack_.clear();
        kifu_.clear();

        is_checked_ = false;

        //合法手生成のフラグを降ろす
        already_generated_moves_ = false;
    }

    //内部の状態等を表示する関数
    fun print() {}

    //一手進める・戻す関数
    fun doMove(move: Move) {}
    fun undo() {}

    //合法性に関する関数
    fun isLegalMove(move: Move): Boolean {
        return true
    }
    fun isLastMoveDropPawn() : Boolean {
        return kind(lastMove().subject()) == PAWN
    }
    fun canWinDeclare() : Boolean {
        return false
    }

    //詰み探索中に枝刈りして良いかを判定
    //王手がかかっていないときは枝刈りして良いだろう
    fun canSkipMateSearch() :Boolean { return !is_checked_; }

    //この局面が詰み、千日手等で終わっているか確認する関数
    //終わっている場合は手番側から見た点数を引数に書き込んでtrueを返す
    fun isFinish(score: Ref.FloatRef, check_repeat: Boolean = true): Boolean {
        return false
    }

    //千日手の判定
    fun isRepeating(score: Ref.FloatRef) : Boolean {
        return false
    }

    //特徴量作成
    fun makeFeature() : ArrayList<Float> {
        return ArrayList<Float>()
    }

    //toとfromしか与えられない状態から完全なMoveに変換する関数
    fun transformValidMove(move: Move): Move {
        return move
    }

    //合法手生成
    fun generateAllMoves() : ArrayList<Move> {
        return ArrayList()
    }

    //sfenの入出力
    fun fromStr(sfen: String) {}
    fun toStr() : String {
        return "todo impl"
    }

    //ハッシュ
    fun initHashSeed() {}

    //getter
    fun turnNumber() : Int { return turn_number_; }
    fun color(): Int { return color_; }
    fun hashValue(): Long { return hash_value_; }
    fun on(sq: Square) :Int { return board_[sq.ordinal]; }
    fun isChecked() : Boolean { return is_checked_; }

    //--------------------
    //    内部メソッド
    //--------------------
    //合法手生成で用いる関数
    fun canPromote(move: Move) : Boolean { return false }
    fun canDropPawn(to: Square) : Boolean { return false }
    fun pushMove(move: Move, move_buf:  ArrayList<Move>) {}
    fun generateNormalMoves(move_buf: ArrayList<Move>) {}
    fun generateEvasionMoves(move_buf: ArrayList<Move>) {}
//    void generateDropMoves(const Bitboard& to_bb, std::vector<Move>& move_buf) const;

    //ハッシュ値の初期化
    fun initHashValue() {}

    //emptyの条件分けをいちいち書かないための補助関数
    fun lastMove() :Move { return if (kifu_.size == 0) NULL_MOVE else kifu_.last(); }

    //------------------
    //    クラス変数
    //------------------
    //ハッシュの各駒・位置に対する決められた値
    val HashSeed = arrayOf<ArrayList<Int>>()
    val HandHashSeed = arrayOf<ArrayList<Int>>()
    //val HandHashSeed = Int[ColorNum][PieceNum][19];

    //------------------------
    //    インスタンス変数
    //------------------------
    //手番
    private var color_: Int = 0

    //盤面
    private var board_ = Array<Int>(Square.SquareNum.ordinal){it}

    //持ち駒
    var hand_ = arrayListOf(Hand(), Hand());

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