package com.example.miacisshogi

//0000 0000 0000 0000 0000 0000 0011 1111 PAWN
//0000 0000 0000 0000 0000 0011 1000 0000 LANCE
//0000 0000 0000 0000 0011 1000 0000 0000 KNIGHT
//0000 0000 0000 0011 1000 0000 0000 0000 SILVER
//0000 0000 0011 1000 0000 0000 0000 0000 GOLD
//0000 0001 1000 0000 0000 0000 0000 0000 BISHOP
//0000 1100 0000 0000 0000 0000 0000 0000 ROOK
const val HAND_PAWN_SHIFT = 0
const val HAND_LANCE_SHIFT = HAND_PAWN_SHIFT + 7
const val HAND_KNIGHT_SHIFT = HAND_LANCE_SHIFT + 4
const val HAND_SILVER_SHIFT = HAND_KNIGHT_SHIFT + 4
const val HAND_GOLD_SHIFT = HAND_SILVER_SHIFT + 4
const val HAND_BISHOP_SHIFT = HAND_GOLD_SHIFT + 4
const val HAND_ROOK_SHIFT = HAND_BISHOP_SHIFT + 3
const val HAND_PAWN_MASK = 0b111111
const val HAND_LANCE_MASK = 0b111 shl HAND_LANCE_SHIFT
const val HAND_KNIGHT_MASK = 0b111 shl HAND_KNIGHT_SHIFT
const val HAND_SILVER_MASK = 0b111 shl HAND_SILVER_SHIFT
const val HAND_GOLD_MASK = 0b111 shl HAND_GOLD_SHIFT
const val HAND_BISHOP_MASK = 0b11 shl HAND_BISHOP_SHIFT
const val HAND_ROOK_MASK = 0b11 shl HAND_ROOK_SHIFT

val PieceToHandShift = arrayListOf(
    0,
    HAND_PAWN_SHIFT,
    HAND_LANCE_SHIFT,
    HAND_KNIGHT_SHIFT,
    HAND_SILVER_SHIFT,
    HAND_GOLD_SHIFT,
    HAND_BISHOP_SHIFT,
    HAND_ROOK_SHIFT
)

val PieceToHandMask = arrayListOf(
    0,
    HAND_PAWN_MASK,
    HAND_LANCE_MASK,
    HAND_KNIGHT_MASK,
    HAND_SILVER_MASK,
    HAND_GOLD_MASK,
    HAND_BISHOP_MASK,
    HAND_ROOK_MASK,
)

class Hand {
    //コンストラクタ
    constructor() {
        hand_ = 0
    }

    //持ち駒の数を返す
    fun num(p: Int): Int {
        return ((hand_ and PieceToHandMask[kind(p)]) shr PieceToHandShift[kind(p)]); }

    //capture(Piece型)を受け取って持ち駒を増減する
    fun add(p: Int) {
        hand_ += 1 shl PieceToHandShift[kind(p)];
    }

    fun sub(p: Int) {
        hand_ -= 1 shl PieceToHandShift[kind(p)];
    }

    //初期化のとき使う
    fun set(p: Int, num: Int) {
        hand_ += num shl PieceToHandShift[kind(p)];
    }

//    //「lhsのどの種類の枚数もrhs以上であり、かつ少なくとも一種類はrhsより多い」かどうかを判定
//    bool superior(const Hand rhs) const {
//        bool over = false;
//        for (Piece p : { PAWN, LANCE, KNIGHT, SILVER, GOLD, BISHOP, ROOK }) {
//            if (num(p) < rhs.num(p)) {
//                return false;
//            } else if (num(p) > rhs.num(p)) {
//                over = true;
//            }
//        }
//        return over;
//    }
//
//    //superiorの逆
//    bool inferior(const Hand rhs) const {
//        bool under = false;
//        for (Piece p : { PAWN, LANCE, KNIGHT, SILVER, GOLD, BISHOP, ROOK }) {
//            if (num(p) > rhs.num(p)) {
//                return false;
//            } else if (num(p) < rhs.num(p)) {
//                under = true;
//            }
//        }
//        return under;
//    }

    //zeroクリア
    fun clear() {
        hand_ = 0;
    }

    //表示
//    void print() const {
//        for (Piece p = PAWN; p <= ROOK; p++)
//        if (num(p)) {
//            std::cout << PieceToStr[p] << num(p) << " ";
//        }
//        std::cout << std::endl;
//    }

    var hand_: Int
}