package com.gotalent.camera.interfaces

import retrofit2.Call
import retrofit2.http.*

interface ImageAPI {
    @GET("images")
    fun getImages(): Call<Array<String>>

    @Headers("Content-Type: text/plain")
    @POST("images")
    fun uploadImage(@Body body: String): Call<Void>
}