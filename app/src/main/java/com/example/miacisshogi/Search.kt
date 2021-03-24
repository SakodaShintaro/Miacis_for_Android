package com.example.miacisshogi

import kotlin.random.Random

class Search {
    fun search(pos: Position): Move {
        val moveList = pos.generateAllMoves()
        val index = Random.nextInt(moveList.size)
        return moveList[index]
    }
}