package com.gotalent.camera.interfaces

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface ImageAPI {
    @GET("images")
    fun getImages(): Call<List<String>>

    @Headers("Content-Type: text/plain")
    @POST("images")
    fun uploadImage(@Body body: String): Call<Void>
}