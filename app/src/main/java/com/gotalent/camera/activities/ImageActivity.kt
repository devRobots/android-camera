package com.gotalent.camera.activities

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.gotalent.camera.R
import com.gotalent.camera.interfaces.ImageAPI
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Image activity
 *
 * @constructor Create Image activity
 */
class ImageActivity : AppCompatActivity() {
    /**
     * On create
     *
     * Genera la vista de la actividad y enlaza toda la logica con el layout
     *
     * @param savedInstanceState Instancia guardada de la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val imageView = findViewById<ImageView>(R.id.imageView)

        val imagePath = intent.getStringExtra(getString(R.string.intent_image_path))
        val imgFile = File(imagePath!!)
        var myBitmap: Bitmap? = null
        if (imgFile.exists()) {
            myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            imageView.setImageBitmap(myBitmap)
        }

        val uploadButton = findViewById<ImageView>(R.id.button_upload)
        val shareButton = findViewById<ImageView>(R.id.button_share)
        val saveButton = findViewById<ImageView>(R.id.button_save)
        val deleteButton = findViewById<ImageView>(R.id.button_delete)

        shareButton.setOnClickListener {
            shareImage(imagePath)
        }

        uploadButton.setOnClickListener {
            uploadImage(imagePath)
        }

        saveButton.setOnClickListener {
            saveImage(imagePath)
        }

        deleteButton.setOnClickListener {
            deleteImage(imagePath)
        }
    }

    /**
     * Upload image
     *
     * Envia la imagen al servidor mediante una peticion POST de la API
     *
     * @param imagePath Path de la imagen a subir
     */
    private fun uploadImage(imagePath: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val actionLayout = findViewById<LinearLayout>(R.id.action_layout)
        progressBar.visibility = ProgressBar.VISIBLE
        actionLayout.visibility = LinearLayout.GONE

        val file = File(imagePath)
        val buffer = ByteArray(file.length().toInt() + 100)
        val length: Int = FileInputStream(file).read(buffer)
        val base64 = Base64.encodeToString(buffer, 0, length, Base64.NO_WRAP)
        val paramObject = JSONObject()
        paramObject.put("image", "data:${getString(R.string.mime_type)};base64,$base64")

        val retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.api_remote))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ImageAPI::class.java)
        service.uploadImage(paramObject.toString()).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                Toast.makeText(this@ImageActivity, getString(R.string.foto_subida), Toast.LENGTH_SHORT).show()
                this@ImageActivity.finish()
            }

            override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                Toast.makeText(this@ImageActivity, getString(R.string.intente_de_nuevo), Toast.LENGTH_SHORT).show()
                progressBar.visibility = ProgressBar.GONE
                actionLayout.visibility = LinearLayout.VISIBLE
            }
        })
    }

    /**
     * Share image
     *
     * Comparte la imagen con el sistema nativo de Android
     *
     * @param imagePath Path de la imagen a compartir
     */
    private fun shareImage(imagePath: String) {
        try {
            val imageUri = FileProvider.getUriForFile(this, getString(R.string.camera_provider), File(imagePath))
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = getString(R.string.mime_type)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.compartir_foto)))
        } catch (e: Exception) {
            // No requerido
        }
    }

    /**
     * Save image
     *
     * Guarda la imagen en la galeria de fotos del dispositivo
     *
     * @param imagePath Path de la imagen a guardar
     */
    private fun saveImage(imagePath: String) {
        val image = File(imagePath)
        val bitmap = BitmapFactory.decodeFile(imagePath)

        //Output stream
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this@ImageActivity.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, image.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, getString(R.string.mime_type))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            fos = FileOutputStream(imagesDir.absolutePath + "/" + image.name)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this@ImageActivity, getString(R.string.foto_guardada), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Delete image
     *
     * Elimina la imagen del dispositivo
     *
     * @param imagePath Path de la imagen a eliminar
     */
    private fun deleteImage(imagePath: String) {
        try {
            val imgFile = File(imagePath)
            imgFile.delete()
            finish()
        } catch (e: Exception) {
            // No requerido
        }
    }
}