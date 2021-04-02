package com.tokumini.miacisshogi

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlin.random.Random

const val TURN_STR = "turn"
const val HUMAN_TURN_BLACK = 0
const val HUMAN_TURN_WHITE = 1
const val CONSIDERATION = 2

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_battle).setOnClickListener {
            val intent = Intent(this, BattleSettingActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_consideration).setOnClickListener {
            val intent = Intent(this, BattleActivity::class.java)
            intent.putExtra(TURN_STR, CONSIDERATION)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_battle_as_black).setOnClickListener {
            val intent = Intent(this, BattleActivity::class.java)
            intent.putExtra(TURN_STR, HUMAN_TURN_BLACK)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_battle_as_white).setOnClickListener {
            val intent = Intent(this, BattleActivity::class.java)
            intent.putExtra(TURN_STR, HUMAN_TURN_WHITE)
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_battle_as_random).setOnClickListener {
            val intent = Intent(this, BattleActivity::class.java)
            intent.putExtra(TURN_STR, Random.nextInt(HUMAN_TURN_BLACK, HUMAN_TURN_WHITE + 1))
            startActivity(intent)
        }
    }
}