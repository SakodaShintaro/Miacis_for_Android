package com.example.miacisshogi

enum class Square {
    WALL00, WALL01, WALL02, WALL03, WALL04, WALL05, WALL06, WALL07, WALL08, WALL09, WALL0A,
    WALL10,   SQ11,   SQ12,   SQ13,   SQ14,   SQ15,   SQ16,   SQ17,   SQ18,   SQ19, WALL1A,
    WALL20,   SQ21,   SQ22,   SQ23,   SQ24,   SQ25,   SQ26,   SQ27,   SQ28,   SQ29, WALL2A,
    WALL30,   SQ31,   SQ32,   SQ33,   SQ34,   SQ35,   SQ36,   SQ37,   SQ38,   SQ39, WALL3A,
    WALL40,   SQ41,   SQ42,   SQ43,   SQ44,   SQ45,   SQ46,   SQ47,   SQ48,   SQ49, WALL4A,
    WALL50,   SQ51,   SQ52,   SQ53,   SQ54,   SQ55,   SQ56,   SQ57,   SQ58,   SQ59, WALL5A,
    WALL60,   SQ61,   SQ62,   SQ63,   SQ64,   SQ65,   SQ66,   SQ67,   SQ68,   SQ69, WALL6A,
    WALL70,   SQ71,   SQ72,   SQ73,   SQ74,   SQ75,   SQ76,   SQ77,   SQ78,   SQ79, WALL7A,
    WALL80,   SQ81,   SQ82,   SQ83,   SQ84,   SQ85,   SQ86,   SQ87,   SQ88,   SQ89, WALL8A,
    WALL90,   SQ91,   SQ92,   SQ93,   SQ94,   SQ95,   SQ96,   SQ97,   SQ98,   SQ99, WALL9A,
    WALLA0, WALLA1, WALLA2, WALLA3, WALLA4, WALLA5, WALLA6, WALLA7, WALLA8, WALLA9, WALLAA,
    SquareNum,
}

enum class File {
    File0, File1, File2, File3, File4, File5, File6, File7, File8, File9, FileA, FileNum,
}

enum class Rank {
    Rank0, Rank1, Rank2, Rank3, Rank4, Rank5, Rank6, Rank7, Rank8, Rank9, RankA, RankNum,
}

enum class DiagR {
    DiagR0, DiagR1, DiagR2, DiagR3, DiagR4, DiagR5, DiagR6, DiagR7, DiagR8, DiagR9, DiagRA, DiagRB, DiagRC, DiagRD, DiagRE, DiagRF, DiagRG, DiagRH,
}

enum class DiagL {
    DiagL0, DiagL1, DiagL2, DiagL3, DiagL4, DiagL5, DiagL6, DiagL7, DiagL8, DiagL9, DiagLA, DiagLB, DiagLC, DiagLD, DiagLE, DiagLF, DiagLG, DiagLH,
}

val SquareToRank = arrayListOf(
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
    Rank.Rank0, Rank.Rank1, Rank.Rank2, Rank.Rank3, Rank.Rank4, Rank.Rank5, Rank.Rank6, Rank.Rank7, Rank.Rank8, Rank.Rank9, Rank.RankA,
)

val SquareToFile = arrayListOf(
    File.File0, File.File0, File.File0, File.File0, File.File0, File.File0, File.File0, File.File0, File.File0, File.File0, File.File0,
    File.File1, File.File1, File.File1, File.File1, File.File1, File.File1, File.File1, File.File1, File.File1, File.File1, File.File1,
    File.File2, File.File2, File.File2, File.File2, File.File2, File.File2, File.File2, File.File2, File.File2, File.File2, File.File2,
    File.File3, File.File3, File.File3, File.File3, File.File3, File.File3, File.File3, File.File3, File.File3, File.File3, File.File3,
    File.File4, File.File4, File.File4, File.File4, File.File4, File.File4, File.File4, File.File4, File.File4, File.File4, File.File4,
    File.File5, File.File5, File.File5, File.File5, File.File5, File.File5, File.File5, File.File5, File.File5, File.File5, File.File5,
    File.File6, File.File6, File.File6, File.File6, File.File6, File.File6, File.File6, File.File6, File.File6, File.File6, File.File6,
    File.File7, File.File7, File.File7, File.File7, File.File7, File.File7, File.File7, File.File7, File.File7, File.File7, File.File7,
    File.File8, File.File8, File.File8, File.File8, File.File8, File.File8, File.File8, File.File8, File.File8, File.File8, File.File8,
    File.File9, File.File9, File.File9, File.File9, File.File9, File.File9, File.File9, File.File9, File.File9, File.File9, File.File9,
    File.FileA, File.FileA, File.FileA, File.FileA, File.FileA, File.FileA, File.FileA, File.FileA, File.FileA, File.FileA, File.FileA,
)

//斜め方向右上がり
val SquareToDiagR = arrayListOf(
    DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR1, DiagR.DiagR2, DiagR.DiagR3, DiagR.DiagR4, DiagR.DiagR5, DiagR.DiagR6, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR2, DiagR.DiagR3, DiagR.DiagR4, DiagR.DiagR5, DiagR.DiagR6, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR3, DiagR.DiagR4, DiagR.DiagR5, DiagR.DiagR6, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR4, DiagR.DiagR5, DiagR.DiagR6, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagRC, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR5, DiagR.DiagR6, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagRC, DiagR.DiagRD, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR6, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagRC, DiagR.DiagRD, DiagR.DiagRE, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR7, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagRC, DiagR.DiagRD, DiagR.DiagRE, DiagR.DiagRF, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR8, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagRC, DiagR.DiagRD, DiagR.DiagRE, DiagR.DiagRF, DiagR.DiagRG, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR9, DiagR.DiagRA, DiagR.DiagRB, DiagR.DiagRC, DiagR.DiagRD, DiagR.DiagRE, DiagR.DiagRF, DiagR.DiagRG, DiagR.DiagRH, DiagR.DiagR0,
    DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0, DiagR.DiagR0,
)

//斜め方向左上がり
val SquareToDiagL = arrayListOf(
    DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL6, DiagL.DiagL5, DiagL.DiagL4, DiagL.DiagL3, DiagL.DiagL2, DiagL.DiagL1, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL6, DiagL.DiagL5, DiagL.DiagL4, DiagL.DiagL3, DiagL.DiagL2, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL6, DiagL.DiagL5, DiagL.DiagL4, DiagL.DiagL3, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLC, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL6, DiagL.DiagL5, DiagL.DiagL4, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLD, DiagL.DiagLC, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL6, DiagL.DiagL5, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLE, DiagL.DiagLD, DiagL.DiagLC, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL6, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLF, DiagL.DiagLE, DiagL.DiagLD, DiagL.DiagLC, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL7, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLG, DiagL.DiagLF, DiagL.DiagLE, DiagL.DiagLD, DiagL.DiagLC, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL8, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagLH, DiagL.DiagLG, DiagL.DiagLF, DiagL.DiagLE, DiagL.DiagLD, DiagL.DiagLC, DiagL.DiagLB, DiagL.DiagLA, DiagL.DiagL9, DiagL.DiagL0,
    DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0, DiagL.DiagL0,
)

val FRToSquare = arrayListOf(
    arrayListOf(Square.WALL00, Square.WALL01, Square.WALL02, Square.WALL03, Square.WALL04, Square.WALL05, Square.WALL06, Square.WALL07, Square.WALL08, Square.WALL09, Square.WALL0A),
    arrayListOf(Square.WALL10, Square.SQ11,   Square.SQ12,   Square.SQ13,   Square.SQ14,   Square.SQ15,   Square.SQ16,   Square.SQ17,   Square.SQ18,   Square.SQ19,   Square.WALL1A),
    arrayListOf(Square.WALL20, Square.SQ21,   Square.SQ22,   Square.SQ23,   Square.SQ24,   Square.SQ25,   Square.SQ26,   Square.SQ27,   Square.SQ28,   Square.SQ29,   Square.WALL2A),
    arrayListOf(Square.WALL30, Square.SQ31,   Square.SQ32,   Square.SQ33,   Square.SQ34,   Square.SQ35,   Square.SQ36,   Square.SQ37,   Square.SQ38,   Square.SQ39,   Square.WALL3A),
    arrayListOf(Square.WALL40, Square.SQ41,   Square.SQ42,   Square.SQ43,   Square.SQ44,   Square.SQ45,   Square.SQ46,   Square.SQ47,   Square.SQ48,   Square.SQ49,   Square.WALL4A),
    arrayListOf(Square.WALL50, Square.SQ51,   Square.SQ52,   Square.SQ53,   Square.SQ54,   Square.SQ55,   Square.SQ56,   Square.SQ57,   Square.SQ58,   Square.SQ59,   Square.WALL5A),
    arrayListOf(Square.WALL60, Square.SQ61,   Square.SQ62,   Square.SQ63,   Square.SQ64,   Square.SQ65,   Square.SQ66,   Square.SQ67,   Square.SQ68,   Square.SQ69,   Square.WALL6A),
    arrayListOf(Square.WALL70, Square.SQ71,   Square.SQ72,   Square.SQ73,   Square.SQ74,   Square.SQ75,   Square.SQ76,   Square.SQ77,   Square.SQ78,   Square.SQ79,   Square.WALL7A),
    arrayListOf(Square.WALL80, Square.SQ81,   Square.SQ82,   Square.SQ83,   Square.SQ84,   Square.SQ85,   Square.SQ86,   Square.SQ87,   Square.SQ88,   Square.SQ89,   Square.WALL8A),
    arrayListOf(Square.WALL90, Square.SQ91,   Square.SQ92,   Square.SQ93,   Square.SQ94,   Square.SQ95,   Square.SQ96,   Square.SQ97,   Square.SQ98,   Square.SQ99,   Square.WALL9A),
    arrayListOf(Square.WALLA0, Square.WALLA1, Square.WALLA2, Square.WALLA3, Square.WALLA4, Square.WALLA5, Square.WALLA6, Square.WALLA7, Square.WALLA8, Square.WALLA9, Square.WALLAA),
)
// clang-format on

const val BOARD_WIDTH = 9
const val SQUARE_NUM = BOARD_WIDTH * BOARD_WIDTH

//方向を示す定数
const val DIR_H = 0
const val DIR_U = -1
const val DIR_D = 1
const val DIR_R = -11
const val DIR_L = 11
const val DIR_RU = DIR_R + DIR_U
const val DIR_RD = DIR_R + DIR_D
const val DIR_LD = DIR_L + DIR_D
const val DIR_LU = DIR_L + DIR_U
const val DIR_RUU = DIR_RU + DIR_U
const val DIR_RDD = DIR_RD + DIR_D
const val DIR_LDD = DIR_LD + DIR_D
const val DIR_LUU = DIR_LU + DIR_U

fun isOnBoard(pos: Square): Boolean {
    return (Rank.Rank1 <= SquareToRank[pos.ordinal] && SquareToRank[pos.ordinal] <= Rank.Rank9 && File.File1 <= SquareToFile[pos.ordinal] && SquareToFile[pos.ordinal] <= File.File9);
}

fun directionAtoB(A: Square, B: Square):Int {
    //8方向のうちどれかか、あるいはどれでもないかだけ判定できればいい
    //Aの位置を0とすると周囲8マスは
    //10 -1 -12
    //11  0 -11
    //12  1 -10
    //だから差の正負と段、筋、斜めの一致具合で方向がわかるはず
    val a = A.ordinal
    val b = B.ordinal
    if (A == B) return DIR_H;
    else if (b - a > 0) {
        if (SquareToRank[a] == SquareToRank[b]) return DIR_L;
        if (SquareToFile[a] == SquareToFile[b]) return DIR_D;
        if (SquareToDiagR[a] == SquareToDiagR[b]) return DIR_LU;
        if (SquareToDiagL[a] == SquareToDiagL[b]) return DIR_LD;
    } else {
        if (SquareToRank[a] == SquareToRank[b]) return DIR_R;
        if (SquareToFile[a] == SquareToFile[b]) return DIR_U;
        if (SquareToDiagR[a] == SquareToDiagR[b]) return DIR_RD;
        if (SquareToDiagL[a] == SquareToDiagL[b]) return DIR_RU;
    }
    return DIR_H;
}

val SquareList = arrayListOf(
    Square.SQ11, Square.SQ12, Square.SQ13, Square.SQ14, Square.SQ15, Square.SQ16, Square.SQ17, Square.SQ18, Square.SQ19,
    Square.SQ21, Square.SQ22, Square.SQ23, Square.SQ24, Square.SQ25, Square.SQ26, Square.SQ27, Square.SQ28, Square.SQ29,
    Square.SQ31, Square.SQ32, Square.SQ33, Square.SQ34, Square.SQ35, Square.SQ36, Square.SQ37, Square.SQ38, Square.SQ39,
    Square.SQ41, Square.SQ42, Square.SQ43, Square.SQ44, Square.SQ45, Square.SQ46, Square.SQ47, Square.SQ48, Square.SQ49,
    Square.SQ51, Square.SQ52, Square.SQ53, Square.SQ54, Square.SQ55, Square.SQ56, Square.SQ57, Square.SQ58, Square.SQ59,
    Square.SQ61, Square.SQ62, Square.SQ63, Square.SQ64, Square.SQ65, Square.SQ66, Square.SQ67, Square.SQ68, Square.SQ69,
    Square.SQ71, Square.SQ72, Square.SQ73, Square.SQ74, Square.SQ75, Square.SQ76, Square.SQ77, Square.SQ78, Square.SQ79,
    Square.SQ81, Square.SQ82, Square.SQ83, Square.SQ84, Square.SQ85, Square.SQ86, Square.SQ87, Square.SQ88, Square.SQ89,
    Square.SQ91, Square.SQ92, Square.SQ93, Square.SQ94, Square.SQ95, Square.SQ96, Square.SQ97, Square.SQ98, Square.SQ99
)

val SquareListWithWALL = arrayListOf(
    Square.WALL00, Square.WALL01, Square.WALL02, Square.WALL03, Square.WALL04, Square.WALL05, Square.WALL06, Square.WALL07, Square.WALL08, Square.WALL09, Square.WALL0A,
    Square.WALL10,   Square.SQ11,   Square.SQ12,   Square.SQ13,   Square.SQ14,   Square.SQ15,   Square.SQ16,   Square.SQ17,   Square.SQ18,   Square.SQ19, Square.WALL1A,
    Square.WALL20,   Square.SQ21,   Square.SQ22,   Square.SQ23,   Square.SQ24,   Square.SQ25,   Square.SQ26,   Square.SQ27,   Square.SQ28,   Square.SQ29, Square.WALL2A,
    Square.WALL30,   Square.SQ31,   Square.SQ32,   Square.SQ33,   Square.SQ34,   Square.SQ35,   Square.SQ36,   Square.SQ37,   Square.SQ38,   Square.SQ39, Square.WALL3A,
    Square.WALL40,   Square.SQ41,   Square.SQ42,   Square.SQ43,   Square.SQ44,   Square.SQ45,   Square.SQ46,   Square.SQ47,   Square.SQ48,   Square.SQ49, Square.WALL4A,
    Square.WALL50,   Square.SQ51,   Square.SQ52,   Square.SQ53,   Square.SQ54,   Square.SQ55,   Square.SQ56,   Square.SQ57,   Square.SQ58,   Square.SQ59, Square.WALL5A,
    Square.WALL60,   Square.SQ61,   Square.SQ62,   Square.SQ63,   Square.SQ64,   Square.SQ65,   Square.SQ66,   Square.SQ67,   Square.SQ68,   Square.SQ69, Square.WALL6A,
    Square.WALL70,   Square.SQ71,   Square.SQ72,   Square.SQ73,   Square.SQ74,   Square.SQ75,   Square.SQ76,   Square.SQ77,   Square.SQ78,   Square.SQ79, Square.WALL7A,
    Square.WALL80,   Square.SQ81,   Square.SQ82,   Square.SQ83,   Square.SQ84,   Square.SQ85,   Square.SQ86,   Square.SQ87,   Square.SQ88,   Square.SQ89, Square.WALL8A,
    Square.WALL90,   Square.SQ91,   Square.SQ92,   Square.SQ93,   Square.SQ94,   Square.SQ95,   Square.SQ96,   Square.SQ97,   Square.SQ98,   Square.SQ99, Square.WALL9A,
    Square.WALLA0, Square.WALLA1, Square.WALLA2, Square.WALLA3, Square.WALLA4, Square.WALLA5, Square.WALLA6, Square.WALLA7, Square.WALLA8, Square.WALLA9, Square.WALLAA,
)


val SquareToNum = arrayListOf(
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1,  0,  1,  2,  3,  4,  5,  6,  7,  8, -1,
    -1,  9, 10, 11, 12, 13, 14, 15, 16, 17, -1,
    -1, 18, 19, 20, 21, 22, 23, 24, 25, 26, -1,
    -1, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1,
    -1, 36, 37, 38, 39, 40, 41, 42, 43, 44, -1,
    -1, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1,
    -1, 54, 55, 56, 57, 58, 59, 60, 61, 62, -1,
    -1, 63, 64, 65, 66, 67, 68, 69, 70, 71, -1,
    -1, 72, 73, 74, 75, 76, 77, 78, 79, 80, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
)

val InvSquare = arrayListOf(
    Square.WALL00, Square.WALL01, Square.WALL02, Square.WALL03, Square.WALL04, Square.WALL05, Square.WALL06, Square.WALL07, Square.WALL08, Square.WALL09, Square.WALL0A,
    Square.WALL10, Square.SQ99,   Square.SQ98,   Square.SQ97,   Square.SQ96,   Square.SQ95,   Square.SQ94,   Square.SQ93,   Square.SQ92,   Square.SQ91,   Square.WALL1A,
    Square.WALL20, Square.SQ89,   Square.SQ88,   Square.SQ87,   Square.SQ86,   Square.SQ85,   Square.SQ84,   Square.SQ83,   Square.SQ82,   Square.SQ81,   Square.WALL2A,
    Square.WALL30, Square.SQ79,   Square.SQ78,   Square.SQ77,   Square.SQ76,   Square.SQ75,   Square.SQ74,   Square.SQ73,   Square.SQ72,   Square.SQ71,   Square.WALL3A,
    Square.WALL40, Square.SQ69,   Square.SQ68,   Square.SQ67,   Square.SQ66,   Square.SQ65,   Square.SQ64,   Square.SQ63,   Square.SQ62,   Square.SQ61,   Square.WALL4A,
    Square.WALL50, Square.SQ59,   Square.SQ58,   Square.SQ57,   Square.SQ56,   Square.SQ55,   Square.SQ54,   Square.SQ53,   Square.SQ52,   Square.SQ51,   Square.WALL5A,
    Square.WALL60, Square.SQ49,   Square.SQ48,   Square.SQ47,   Square.SQ46,   Square.SQ45,   Square.SQ44,   Square.SQ43,   Square.SQ42,   Square.SQ41,   Square.WALL6A,
    Square.WALL70, Square.SQ39,   Square.SQ38,   Square.SQ37,   Square.SQ36,   Square.SQ35,   Square.SQ34,   Square.SQ33,   Square.SQ32,   Square.SQ31,   Square.WALL7A,
    Square.WALL80, Square.SQ29,   Square.SQ28,   Square.SQ27,   Square.SQ26,   Square.SQ25,   Square.SQ24,   Square.SQ23,   Square.SQ22,   Square.SQ21,   Square.WALL8A,
    Square.WALL90, Square.SQ19,   Square.SQ18,   Square.SQ17,   Square.SQ16,   Square.SQ15,   Square.SQ14,   Square.SQ13,   Square.SQ12,   Square.SQ11,   Square.WALL9A,
    Square.WALLA0, Square.WALLA1, Square.WALLA2, Square.WALLA3, Square.WALLA4, Square.WALLA5, Square.WALLA6, Square.WALLA7, Square.WALLA8, Square.WALLA9, Square.WALLAA,
)

val fileToString = arrayListOf(
    "０", "１", "２", "３", "４", "５", "６", "７", "８", "９", "A"
)
val rankToString = arrayListOf(
    "零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
)

val fileToSfenString = arrayListOf(
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
)
val rankToSfenString = arrayListOf(
    "X", "a", "b", "c", "d", "e", "f", "g", "h", "X"
)
