package com.gotalent.camera.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gotalent.camera.R

/**
 * Images adapter
 *
 * @property dataSet lista de imagenes
 * @constructor Create Images adapter
 *
 * @author Yesid Rosas Toro
 * @since 1.0.0
 */
class ImagesAdapter(private val dataSet: Array<String>): RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {
    /**
     * View holder
     *
     * @property view vista de un item de la lista
     * @constructor Create View holder
     */
    class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        /**
         * Conecta los datos ingresados con la vista
         *
         * @param text texto a mostrar
         */
        fun bind(text: String) {
            val textView = view.findViewById<TextView>(R.id.textView)
            textView.text = text
        }
    }

    /**
     * On create view holder
     *
     * @param parent padre del view holder
     * @param viewType tipo de vista
     * @return view holder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    /**
     * Get item count
     *
     * @return cantidad de elementos
     */
    override fun getItemCount(): Int = dataSet.size

    /**
     * On bind view holder
     *
     * @param holder view holder
     * @param position posicion del elemento
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataSet[position])
    }

}