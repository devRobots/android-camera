package com.gotalent.camera.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gotalent.camera.R
import com.gotalent.camera.adapters.ImagesAdapter
import com.gotalent.camera.interfaces.ImageAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Image list activity
 *
 * @constructor Create Image list activity
 */
class ImageListActivity : AppCompatActivity() {

    /**
     * On create
     *
     * Genera la vista de la actividad y enlaza toda la logica con el layout
     *
     * @param savedInstanceState Saved instance state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)

        val listaFotos = findViewById<RecyclerView>(R.id.lista_fotos)
        listaFotos.layoutManager = LinearLayoutManager(this)
        listaFotos.setHasFixedSize(true)

        val retrofit = Retrofit.Builder().baseUrl(getString(R.string.api_remote))
            .addConverterFactory(GsonConverterFactory.create()).build()
        val service = retrofit.create(ImageAPI::class.java)
        service.getImages().enqueue(object: Callback<Array<String>> {
            override fun onFailure(call: Call<Array<String>>, t: Throwable) {
                // No requerido
            }

            override fun onResponse(call: Call<Array<String>>, response: Response<Array<String>>) {
                val lista = response.body()
                listaFotos.adapter = ImagesAdapter(lista!!)
            }
        })
    }
}