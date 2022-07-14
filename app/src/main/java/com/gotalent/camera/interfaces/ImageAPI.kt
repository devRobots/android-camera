package com.gotalent.camera.interfaces

import retrofit2.Call
import retrofit2.http.*

/**
 * Image API
 *
 * @constructor Image API
 *
 * @author Yesid Rosas Toro
 * @since 1.0.0
 */
interface ImageAPI {
    /**
     * Obteniene todas las imagenes del Firebase Storage desde la API
     *
     * @return respuesta con la lista de imagenes (ruta en servidor)
     */
    @GET("images")
    fun getImages(): Call<Array<String>>

    /**
     * Sube una imagen al Firebase Storage a traves de la API
     *
     * @param body image data base64
     * @return respuesta vacia
     */
    @Headers("Content-Type: text/plain")
    @POST("images")
    fun uploadImage(@Body body: String): Call<Void>
}