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

class ImageListActivity : AppCompatActivity() {

    private val retrofit = Retrofit.Builder().baseUrl(getString(R.string.api_remote))
        .addConverterFactory(GsonConverterFactory.create()).build()
    private val service = retrofit.create(ImageAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)

        val listaFotos = findViewById<RecyclerView>(R.id.lista_fotos)
        listaFotos.layoutManager = LinearLayoutManager(this)
        listaFotos.setHasFixedSize(true)

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