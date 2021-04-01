package com.tokumini.miacisshogi

enum class Menu {
    BACK_TO_TOP,
    RESIGN,
    INIT_POSITION,
    INPUT_SFEN,
    OUTPUT_SFEN,
    CLEAR_RESULT,
    MENU_ITEM_SIZE
}

val items = Array(Menu.MENU_ITEM_SIZE.ordinal) {
    when (it) {
        Menu.BACK_TO_TOP.ordinal -> "トップ画面に戻る"
        Menu.RESIGN.ordinal -> "投了"
        Menu.INIT_POSITION.ordinal -> "盤面を初期化"
        Menu.INPUT_SFEN.ordinal -> "SFENを入力"
        Menu.OUTPUT_SFEN.ordinal -> "現局面のSFENをクリップボードへコピー"
        Menu.CLEAR_RESULT.ordinal -> "対局成績の初期化"
        else -> "ERROR"
    }
}