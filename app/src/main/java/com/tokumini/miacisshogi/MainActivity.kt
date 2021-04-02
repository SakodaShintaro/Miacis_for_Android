package com.tokumini.miacisshogi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

const val KEY_BATTLE_MODE = "key_battle_mode"
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
            intent.putExtra(KEY_BATTLE_MODE, CONSIDERATION)
            startActivity(intent)
        }
    }
}