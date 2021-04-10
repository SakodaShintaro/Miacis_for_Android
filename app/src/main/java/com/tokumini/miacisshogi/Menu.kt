package com.tokumini.miacisshogi

enum class MenuInBattleMode {
    BACK_TO_TOP,
    RESIGN,
    OUTPUT_SFEN,
    CLEAR_RESULT,
    MENU_ITEM_SIZE
}

val itemsInBattleMode = Array(MenuInBattleMode.MENU_ITEM_SIZE.ordinal) {
    when (it) {
        MenuInBattleMode.BACK_TO_TOP.ordinal -> "トップ画面に戻る"
        MenuInBattleMode.RESIGN.ordinal -> "投了"
        MenuInBattleMode.OUTPUT_SFEN.ordinal -> "現局面のSFENをクリップボードへコピー"
        MenuInBattleMode.CLEAR_RESULT.ordinal -> "対局成績の初期化"
        else -> "ERROR"
    }
}

enum class MenuInConsiderationMode {
    BACK_TO_TOP,
    SAVE_KIFU,
    INPUT_SFEN,
    OUTPUT_SFEN,
    CLEAR_RESULT,
    MENU_ITEM_SIZE
}

val itemsInConsiderationMode = Array(MenuInConsiderationMode.MENU_ITEM_SIZE.ordinal) {
    when (it) {
        MenuInConsiderationMode.BACK_TO_TOP.ordinal -> "トップ画面に戻る"
        MenuInConsiderationMode.SAVE_KIFU.ordinal -> "棋譜を保存"
        MenuInConsiderationMode.INPUT_SFEN.ordinal -> "SFENを入力"
        MenuInConsiderationMode.OUTPUT_SFEN.ordinal -> "現局面のSFENをクリップボードへコピー"
        MenuInConsiderationMode.CLEAR_RESULT.ordinal -> "対局成績の初期化"
        else -> "ERROR"
    }
}