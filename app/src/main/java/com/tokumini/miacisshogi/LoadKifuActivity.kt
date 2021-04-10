package com.tokumini.miacisshogi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import java.io.File

val KEY_LOAD_KIFU_FILE = "key_load_kifu_file"

class ItemAdapter(private val context: Context, private val data: List<String>) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    lateinit var listener: OnItemClickListener
    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = data[position]
        holder.textView.text = item
        holder.itemView.setOnClickListener {
            listener.onItemClickListener(it, position, data[position], context)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    //インターフェースの作成
    interface OnItemClickListener{
        fun onItemClickListener(view: View, position: Int, clickedText: String, context: Context)
    }

    // リスナー
    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}

class LoadKifuActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_kifu)

        val filenames = File(applicationContext.filesDir, ".").list()
        val list = MutableList(0) {""}

        if (filenames != null) {
            for (filename in filenames) {
                val file = File(filename).absoluteFile
                if (file.extension == "txt") {
                    list.add(filename)
                }
            }

            val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
            val adapter = ItemAdapter(this, list)
            adapter.setOnItemClickListener(object:ItemAdapter.OnItemClickListener{
                override fun onItemClickListener(view: View, position: Int, clickedText: String, context: Context) {
                    val intent = Intent(context, BattleActivity::class.java)
                    intent.putExtra(KEY_BATTLE_MODE, CONSIDERATION)
                    intent.putExtra(KEY_LOAD_KIFU_FILE, clickedText)
                    startActivity(intent)
                }
            })
            recyclerView.adapter = adapter
            recyclerView.setHasFixedSize(true)
        }
    }
}