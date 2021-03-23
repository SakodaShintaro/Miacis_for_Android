package com.example.miacisshogi

// MoveConst 
//0000 0000 0000 0000 0000 0000 0111 1111 to
//0000 0000 0000 0000 0011 1111 1000 0000 from
//0000 0000 0000 0000 0100 0000 0000 0000 drop
//0000 0000 0000 0000 1000 0000 0000 0000 promote
//0000 0000 1111 1111 0000 0000 0000 0000 subject
//1111 1111 0000 0000 0000 0000 0000 0000 capture
const val MOVE_TO_SHIFT = 0
const val MOVE_FROM_SHIFT = 7
const val MOVE_DROP_SHIFT = 14
const val MOVE_PROMOTE_SHIFT = 15
const val MOVE_SUBJECT_SHIFT = 16
const val MOVE_CAPTURE_SHIFT = 24
const val MOVE_TO_MASK = 0b1111111
const val MOVE_FROM_MASK = MOVE_TO_MASK shl MOVE_FROM_SHIFT
const val MOVE_DROP_MASK = 1 shl MOVE_DROP_SHIFT
const val MOVE_PROMOTE_MASK = 1 shl MOVE_PROMOTE_SHIFT
const val MOVE_SUBJECT_MASK = 0xff shl MOVE_SUBJECT_SHIFT
const val MOVE_CAPTURE_MASK = 0xff shl MOVE_CAPTURE_SHIFT
const val MOVE_DECLARE = -1

//行動の次元数
const val POLICY_CHANNEL_NUM = 27
const val POLICY_DIM = SQUARE_NUM * POLICY_CHANNEL_NUM

fun Boolean.toInt() = if (this) 1 else 0

class Move() {
    //コンストラクタ
    constructor(x: Int) : this() {
        move_ = x
    }

    constructor(to: Square, from: Square) : this() {
        move_ = (from.ordinal shl MOVE_FROM_SHIFT) or (to.ordinal shl MOVE_TO_SHIFT)
    }

    constructor(to: Square, from: Square, isDrop: Boolean) : this() {
        move_ =
            (isDrop.toInt() shl MOVE_DROP_SHIFT or from.ordinal shl MOVE_FROM_SHIFT or to.ordinal shl MOVE_TO_SHIFT)
    }

    constructor(to: Square, from: Square, isDrop: Boolean, isPromote: Boolean) : this() {
        move_ =
            (isPromote.toInt() shl MOVE_PROMOTE_SHIFT or isDrop.toInt() shl MOVE_DROP_SHIFT or from.ordinal shl MOVE_FROM_SHIFT or to.ordinal shl MOVE_TO_SHIFT)
    }

    constructor(
        to: Square,
        from: Square,
        isDrop: Boolean,
        isPromote: Boolean,
        subject: Int
    ) : this() {
        move_ =
            (subject shl MOVE_SUBJECT_SHIFT or isPromote.toInt() shl MOVE_PROMOTE_SHIFT or isDrop.toInt() shl MOVE_DROP_SHIFT or
                    from.ordinal shl MOVE_FROM_SHIFT or to.ordinal shl MOVE_TO_SHIFT)
    }

    constructor(
        to: Square,
        from: Square,
        isDrop: Boolean,
        isPromote: Boolean,
        subject: Int,
        capture: Int
    ) : this() {
        move_ =
            (capture shl MOVE_CAPTURE_SHIFT or subject shl MOVE_SUBJECT_SHIFT or isPromote.toInt() shl MOVE_PROMOTE_SHIFT or
                    isDrop.toInt() shl MOVE_DROP_SHIFT or from.ordinal shl MOVE_FROM_SHIFT or to.ordinal shl MOVE_TO_SHIFT)
    }

    //sfen形式でのstring
    fun toSfenStr(): String {
        if (this == NULL_MOVE) {
            return "resign"
        }
        if (this == DECLARE_MOVE) {
            return "win"
        }
        if (isDrop()) {
            return PieceToSfenStrWithoutSpace[kind(subject())] + "*" +
                    com.example.miacisshogi.fileToSfenString[SquareToFile[to().ordinal].ordinal] +
                    com.example.miacisshogi.rankToSfenString[SquareToRank[to().ordinal].ordinal]
        } else {
            var result = String()
            result = com.example.miacisshogi.fileToSfenString[SquareToFile[from().ordinal].ordinal] +
                     com.example.miacisshogi.rankToSfenString[SquareToRank[from().ordinal].ordinal] +
                     com.example.miacisshogi.fileToSfenString[SquareToFile[to().ordinal].ordinal] +
                     com.example.miacisshogi.rankToSfenString[SquareToRank[to().ordinal].ordinal]
            if (isPromote()) {
                result += '+'
            }
            return result
        }
    }

    //見やすい日本語での表示
    fun toPrettyStr(): String {
        if (move_ == MOVE_DECLARE) {
            return "入玉宣言"
        }
        var result: String = ""
        val c = pieceToColor(subject())
        result += (if (c == BLACK) "▲" else "△")
        result += fileToString[SquareToFile[to().ordinal].ordinal]
        result += rankToString[SquareToRank[to().ordinal].ordinal]
        result += PieceToStr[kindWithPromotion(subject())]
        if (isPromote()) {
            result += "成"
        }
        if (isDrop()) {
            result += "打"
        } else {
            result += "(" + SquareToFile[from().ordinal] + SquareToRank[from().ordinal] + ") "
        }
        //    if (capture() != EMPTY) {
        //        str << "capture:" << PieceToStr[capture()];
        //    }
        return result
    }

    //要素を取り出す関数ら
    fun to(): Square {
        return SquareListWithWALL[move_ and MOVE_TO_MASK]
    }

    fun from(): Square {
        return SquareListWithWALL[move_ and MOVE_FROM_MASK shr MOVE_FROM_SHIFT]
    }

    fun isDrop(): Boolean {
        return (move_ and MOVE_DROP_MASK) != 0; }

    fun isPromote(): Boolean {
        return (move_ and MOVE_PROMOTE_MASK) != 0; }

    fun subject(): Int {
        return ((move_ and MOVE_SUBJECT_MASK) shr MOVE_SUBJECT_SHIFT); }

    fun capture(): Int {
        return ((move_ and MOVE_CAPTURE_MASK) shr MOVE_CAPTURE_SHIFT); }

    //比較演算子
    fun equals(rhs: Move): Boolean {
        return move_ == rhs.move_
    }

    //ラベル系
    //行動から教師ラベルへと変換する関数
    fun toLabel(): Int {
        val c: Int = pieceToColor(subject())

        val to_sq = if (c == BLACK) to() else InvSquare[to().ordinal]
        val to_file = SquareToFile[to_sq.ordinal].ordinal
        val to_rank = SquareToRank[to_sq.ordinal].ordinal
        val from_sq = if (c == BLACK) from() else InvSquare[from().ordinal]
        val from_file = SquareToFile[from_sq.ordinal].ordinal
        val from_rank = SquareToRank[from_sq.ordinal].ordinal

        var direction: Int = -1
        if (from() == Square.WALL00) { //打つ手
            direction = 20 + kind(subject()) - PAWN
        } else if (to_file == from_file - 1 && to_rank == from_rank + 2) { //桂馬
            direction = 4
        } else if (to_file == from_file + 1 && to_rank == from_rank + 2) { //桂馬
            direction = 6
        } else if (to_file == from_file && to_rank > from_rank) { //上
            direction = 0
        } else if (to_file > from_file && to_rank > from_rank) { //右上
            direction = 1
        } else if (to_file > from_file && to_rank == from_rank) { //右
            direction = 2
        } else if (to_file > from_file && to_rank < from_rank) { //右下
            direction = 3
        } else if (to_file == from_file && to_rank < from_rank) { //下
            direction = 5
        } else if (to_file < from_file && to_rank < from_rank) { //左下
            direction = 7
        } else if (to_file < from_file && to_rank == from_rank) { //左
            direction = 8
        } else if (to_file < from_file && to_rank > from_rank) { //左上
            direction = 9
        } else {
            assert(false)
        }
        if (isPromote()) {
            direction += 10
        }

        return (SquareToNum.get(to_sq.ordinal) + SQUARE_NUM * direction)
    }

    var move_: Int = 0
}

//駒を打つ手
fun dropMove(to: Square, p: Int): Move {
    return Move(to, Square.WALL00, true, false, p, EMPTY); }

//駒を動かす手を引数として、それの成った動きを返す
fun promotiveMove(non_promotive_move: Move): Move {
    return Move(
        non_promotive_move.to(),
        non_promotive_move.from(),
        false,
        true,
        non_promotive_move.subject(),
        non_promotive_move.capture()
    )
}

//比較用
val NULL_MOVE = Move(0)

//宣言
val DECLARE_MOVE = Move(MOVE_DECLARE)

//これコンストラクタとかで書いた方がいい気がするけどうまく書き直せなかった
//まぁ動けばいいのかなぁ
fun stringToMove(input: String): Move {
    if ('A' <= input[0] && input[0] <= 'Z') { //持ち駒を打つ手
        val to = FRToSquare[input[2] - '0'][input[3] - 'a' + 1]

        val p = when (input[0]) {
            'P' -> PAWN
            'L' -> LANCE
            'N' -> KNIGHT
            'S' -> SILVER
            'G' -> GOLD
            'B' -> BISHOP
            'R' -> ROOK
            else -> EMPTY
        }

        return dropMove(to, p)
    } else { //盤上の駒を動かす手
        val from = FRToSquare[input[0] - '0'][input[1] - 'a' + 1]
        val to = FRToSquare[input[2] - '0'][input[3] - 'a' + 1]
        val promote = (input.length == 5 && input[4] == '+')
        return Move(to, from, false, promote, EMPTY, EMPTY)
    }
}