package com.tokumini.miacisshogi

const val BLACK = 0
const val WHITE = 1
const val ColorNum = 2
fun color2oppositeColor(color: Int): Int {
    return if (color == BLACK) WHITE else BLACK
}

const val PROMOTE_BIT = 5
const val BLACK_BIT = 6
const val WHITE_BIT = 7
const val WALL_BIT = 8
const val EMPTY = 0                           //0000 0000
const val PAWN = 1                           //0000 0001
const val LANCE = 2                           //0000 0010
const val KNIGHT = 3                           //0000 0011
const val SILVER = 4                           //0000 0100
const val GOLD = 5                           //0000 0101
const val BISHOP = 6                           //0000 0110
const val ROOK = 7                           //0000 0111
const val KING = 8                           //0000 1000
const val PIECE_KIND_MASK = 15                          //0000 1111
const val PROMOTE = 1 shl (PROMOTE_BIT - 1)      //0001 0000  16
const val PAWN_PROMOTE = PAWN + PROMOTE              //0001 0001  17
const val LANCE_PROMOTE = LANCE + PROMOTE             //0001 0010  18
const val KNIGHT_PROMOTE = KNIGHT + PROMOTE            //0001 0011  19
const val SILVER_PROMOTE = SILVER + PROMOTE            //0001 0100  20
const val BISHOP_PROMOTE = BISHOP + PROMOTE            //0001 0110  22
const val ROOK_PROMOTE = ROOK + PROMOTE              //0001 0111  23
const val BLACK_FLAG = 1 shl (BLACK_BIT - 1)        //0010 0000  32
const val BLACK_PAWN = PAWN + BLACK_FLAG           //0010 0001  33
const val BLACK_LANCE = LANCE + BLACK_FLAG          //0010 0010  34
const val BLACK_KNIGHT = KNIGHT + BLACK_FLAG         //0010 0011  35
const val BLACK_SILVER = SILVER + BLACK_FLAG         //0010 0100  36
const val BLACK_GOLD = GOLD + BLACK_FLAG           //0010 0101  37
const val BLACK_BISHOP = BISHOP + BLACK_FLAG         //0010 0110  38
const val BLACK_ROOK = ROOK + BLACK_FLAG           //0010 0111  39
const val BLACK_KING = KING + BLACK_FLAG           //0010 1000  40
const val BLACK_PAWN_PROMOTE = PAWN_PROMOTE + BLACK_FLAG   //0011 0001  49
const val BLACK_LANCE_PROMOTE = LANCE_PROMOTE + BLACK_FLAG  //0011 0010  50
const val BLACK_KNIGHT_PROMOTE = KNIGHT_PROMOTE + BLACK_FLAG //0011 0011  51
const val BLACK_SILVER_PROMOTE = SILVER_PROMOTE + BLACK_FLAG //0011 0100  52
const val BLACK_BISHOP_PROMOTE = BISHOP_PROMOTE + BLACK_FLAG //0011 0110  54
const val BLACK_ROOK_PROMOTE = ROOK_PROMOTE + BLACK_FLAG   //0011 0111  55
const val WHITE_FLAG = 1 shl (WHITE_BIT - 1)        //0100 0000  64
const val WHITE_PAWN = PAWN + WHITE_FLAG           //0100 0001  65
const val WHITE_LANCE = LANCE + WHITE_FLAG          //0100 0010  66
const val WHITE_KNIGHT = KNIGHT + WHITE_FLAG         //0100 0011  67
const val WHITE_SILVER = SILVER + WHITE_FLAG         //0100 0100  68
const val WHITE_GOLD = GOLD + WHITE_FLAG           //0100 0101  69
const val WHITE_BISHOP = BISHOP + WHITE_FLAG         //0100 0110  70
const val WHITE_ROOK = ROOK + WHITE_FLAG           //0100 0111  71
const val WHITE_KING = KING + WHITE_FLAG           //0100 1000  72
const val WHITE_PAWN_PROMOTE = PAWN_PROMOTE + WHITE_FLAG   //0101 0001  81
const val WHITE_LANCE_PROMOTE = LANCE_PROMOTE + WHITE_FLAG  //0101 0010  82
const val WHITE_KNIGHT_PROMOTE = KNIGHT_PROMOTE + WHITE_FLAG //0101 0011  83
const val WHITE_SILVER_PROMOTE = SILVER_PROMOTE + WHITE_FLAG //0101 0100  84
const val WHITE_BISHOP_PROMOTE = BISHOP_PROMOTE + WHITE_FLAG //0101 0110  86
const val WHITE_ROOK_PROMOTE = ROOK_PROMOTE + WHITE_FLAG   //0101 0111  87
const val PieceNum = WHITE_ROOK_PROMOTE + 1
const val WALL = 1 shl (WALL_BIT) //1000 0000

const val PIECE_KIND_NUM = 14
const val HAND_PIECE_KIND_NUM = 7
const val INPUT_CHANNEL_NUM = (PIECE_KIND_NUM + HAND_PIECE_KIND_NUM) * 2

fun kind(p: Int): Int {
    return p and PIECE_KIND_MASK;
}

fun kindWithPromotion(p: Int): Int {
    return (p and (PROMOTE or PIECE_KIND_MASK))
}

fun promote(p: Int): Int {
    return (p or PROMOTE);
}

fun pieceToColor(p: Int): Int {
    return when {
        p and BLACK_FLAG != 0 -> BLACK
        p and WHITE_FLAG != 0 -> WHITE
        else -> ColorNum
    }
}

fun toBlack(p: Int): Int {
    return p or BLACK_FLAG
}

fun toWhite(p: Int): Int {
    return p or WHITE_FLAG
}

fun coloredPiece(c: Int, p: Int): Int {
    return if (c == BLACK) toBlack(p) else toWhite(p);
}

fun piece2oppositeColorPiece(p: Int): Int {
    var result = p
    if (pieceToColor(p) == BLACK) {
        //BLACKのフラグを消して
        result = result and BLACK_FLAG.inv()
        //WHITEのフラグを立てる
        result = result or WHITE_FLAG
    } else if (pieceToColor(p) == WHITE) {
        //BLACKのフラグを消して
        result = result and WHITE_FLAG.inv()
        //WHITEのフラグを立てる
        result = result or BLACK_FLAG
    }
    return result
}


val PieceToStr = mapOf(
    PAWN to "歩",
    LANCE to "香",
    KNIGHT to "桂",
    SILVER to "銀",
    GOLD to "金",
    BISHOP to "角",
    ROOK to "飛",
    KING to "玉",
    PAWN_PROMOTE to "と",
    LANCE_PROMOTE to "成香",
    KNIGHT_PROMOTE to "成桂",
    SILVER_PROMOTE to "成銀",
    BISHOP_PROMOTE to "馬",
    ROOK_PROMOTE to "竜",
    BLACK_PAWN to "先手歩",
    BLACK_LANCE to "先手香",
    BLACK_KNIGHT to "先手桂",
    BLACK_SILVER to "先手銀",
    BLACK_GOLD to "先手金",
    BLACK_BISHOP to "先手角",
    BLACK_ROOK to "先手飛車",
    BLACK_KING to "先手玉",
    BLACK_PAWN_PROMOTE to "先手と",
    BLACK_LANCE_PROMOTE to "先手成香",
    BLACK_KNIGHT_PROMOTE to "先手成桂",
    BLACK_SILVER_PROMOTE to "先手成銀",
    BLACK_BISHOP_PROMOTE to "先手馬",
    BLACK_ROOK_PROMOTE to "先手竜",
    WHITE_PAWN to "後手歩",
    WHITE_LANCE to "後手香",
    WHITE_KNIGHT to "後手桂",
    WHITE_SILVER to "後手銀",
    WHITE_GOLD to "後手金",
    WHITE_BISHOP to "後手角",
    WHITE_ROOK to "後手飛車",
    WHITE_KING to "後手玉",
    WHITE_PAWN_PROMOTE to "後手と",
    WHITE_LANCE_PROMOTE to "後手成香",
    WHITE_KNIGHT_PROMOTE to "後手成桂",
    WHITE_SILVER_PROMOTE to "後手成銀",
    WHITE_BISHOP_PROMOTE to "後手馬",
    WHITE_ROOK_PROMOTE to "後手竜"
)

val PieceToSfenStrWithSpace = mapOf(
    EMPTY to "  ",
    PAWN to "P",
    LANCE to "L",
    KNIGHT to "N",
    SILVER to "S",
    GOLD to "G",
    BISHOP to "B",
    ROOK to "R",
    BLACK_PAWN to " P",
    BLACK_LANCE to " L",
    BLACK_KNIGHT to " N",
    BLACK_SILVER to " S",
    BLACK_GOLD to " G",
    BLACK_BISHOP to " B",
    BLACK_ROOK to " R",
    BLACK_KING to " K",
    BLACK_PAWN_PROMOTE to "+P",
    BLACK_LANCE_PROMOTE to "+L",
    BLACK_KNIGHT_PROMOTE to "+N",
    BLACK_SILVER_PROMOTE to "+S",
    BLACK_BISHOP_PROMOTE to "+B",
    BLACK_ROOK_PROMOTE to "+R",
    WHITE_PAWN to " p",
    WHITE_LANCE to " l",
    WHITE_KNIGHT to " n",
    WHITE_SILVER to " s",
    WHITE_GOLD to " g",
    WHITE_BISHOP to " b",
    WHITE_ROOK to " r",
    WHITE_KING to " k",
    WHITE_PAWN_PROMOTE to "+p",
    WHITE_LANCE_PROMOTE to "+l",
    WHITE_KNIGHT_PROMOTE to "+n",
    WHITE_SILVER_PROMOTE to "+s",
    WHITE_BISHOP_PROMOTE to "+b",
    WHITE_ROOK_PROMOTE to "+r",
)

val PieceToSfenStrWithoutSpace = mapOf(
    PAWN to "P",
    LANCE to "L",
    KNIGHT to "N",
    SILVER to "S",
    GOLD to "G",
    BISHOP to "B",
    ROOK to "R",
    KING to "K",
    PAWN_PROMOTE to "+P",
    LANCE_PROMOTE to "+L",
    KNIGHT_PROMOTE to "+N",
    SILVER_PROMOTE to "+S",
    BISHOP_PROMOTE to "+B",
    ROOK_PROMOTE to "+R",
    BLACK_PAWN to "P",
    BLACK_LANCE to "L",
    BLACK_KNIGHT to "N",
    BLACK_SILVER to "S",
    BLACK_GOLD to "G",
    BLACK_BISHOP to "B",
    BLACK_ROOK to "R",
    BLACK_KING to "K",
    BLACK_PAWN_PROMOTE to "+P",
    BLACK_LANCE_PROMOTE to "+L",
    BLACK_KNIGHT_PROMOTE to "+N",
    BLACK_SILVER_PROMOTE to "+S",
    BLACK_BISHOP_PROMOTE to "+B",
    BLACK_ROOK_PROMOTE to "+R",
    WHITE_PAWN to "p",
    WHITE_LANCE to "l",
    WHITE_KNIGHT to "n",
    WHITE_SILVER to "s",
    WHITE_GOLD to "g",
    WHITE_BISHOP to "b",
    WHITE_ROOK to "r",
    WHITE_KING to "k",
    WHITE_PAWN_PROMOTE to "+p",
    WHITE_LANCE_PROMOTE to "+l",
    WHITE_KNIGHT_PROMOTE to "+n",
    WHITE_SILVER_PROMOTE to "+s",
    WHITE_BISHOP_PROMOTE to "+b",
    WHITE_ROOK_PROMOTE to "+r"
)

val PieceList = arrayListOf(
    BLACK_PAWN,
    BLACK_LANCE,
    BLACK_KNIGHT,
    BLACK_SILVER,
    BLACK_GOLD,
    BLACK_BISHOP,
    BLACK_ROOK,
    BLACK_KING,
    BLACK_PAWN_PROMOTE,
    BLACK_LANCE_PROMOTE,
    BLACK_KNIGHT_PROMOTE,
    BLACK_SILVER_PROMOTE,
    BLACK_BISHOP_PROMOTE,
    BLACK_ROOK_PROMOTE,
    WHITE_PAWN,
    WHITE_LANCE,
    WHITE_KNIGHT,
    WHITE_SILVER,
    WHITE_GOLD,
    WHITE_BISHOP,
    WHITE_ROOK,
    WHITE_KING,
    WHITE_PAWN_PROMOTE,
    WHITE_LANCE_PROMOTE,
    WHITE_KNIGHT_PROMOTE,
    WHITE_SILVER_PROMOTE,
    WHITE_BISHOP_PROMOTE,
    WHITE_ROOK_PROMOTE,
)

val ColoredJumpPieceList = arrayListOf(
    arrayListOf(BLACK_LANCE, BLACK_BISHOP, BLACK_ROOK),
    arrayListOf(WHITE_LANCE, WHITE_BISHOP, WHITE_ROOK)
)