package com.tokumini.miacisshogi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tokumini.miacisshogi.databinding.ActivityBattleSettingBinding
import kotlin.random.Random

const val KEY_RANDOM_TURN = "key_random_turn"

class BattleSettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBattleSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBattleSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.radioTurn.check(R.id.radio_random)

        binding.numPicker.maxValue = 512
        binding.numPicker.minValue = 0

        binding.buttonBattleStart.setOnClickListener {
            val mode = when (binding.radioTurn.checkedRadioButtonId) {
                R.id.radio_black -> HUMAN_TURN_BLACK
                R.id.radio_white -> HUMAN_TURN_WHITE
                R.id.radio_random -> Random.nextInt(HUMAN_TURN_BLACK, HUMAN_TURN_WHITE + 1)
                else -> -1
            }

            val intent = Intent(this, BattleActivity::class.java)
            intent.putExtra(KEY_BATTLE_MODE, mode)
            intent.putExtra(KEY_RANDOM_TURN, binding.numPicker.value)
            startActivity(intent)
        }
    }
}