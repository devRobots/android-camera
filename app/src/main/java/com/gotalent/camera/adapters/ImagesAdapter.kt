package com.gotalent.camera.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gotalent.camera.R

class ImagesAdapter(private val dataSet: Array<String>): RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {
    class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        fun bind(text: String) {
            val textView = view.findViewById<TextView>(R.id.textView)
            textView.text = text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

}