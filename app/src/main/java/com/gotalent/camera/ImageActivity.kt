package com.gotalent.camera

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class ImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val imageView = findViewById<ImageView>(R.id.imageView)

        val imagePath = intent.getStringExtra("imagePath")
        val imgFile = File(imagePath!!)
        var myBitmap: Bitmap? = null
        if (imgFile.exists()) {
            myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            imageView.setImageBitmap(myBitmap)
        }

        val shareButton = findViewById<ImageView>(R.id.button_share)
        val saveButton = findViewById<ImageView>(R.id.button_save)
        val deleteButton = findViewById<ImageView>(R.id.button_delete)

        shareButton.setOnClickListener {
            shareImage(imagePath)
        }

        saveButton.setOnClickListener {
            saveImage(imagePath)
        }

        deleteButton.setOnClickListener {
            deleteImage(imagePath)
        }
    }

    private fun shareImage(imagePath: String) {
        try {
            val imageUri = FileProvider.getUriForFile(this, "com.gotalent.camera.provider", File(imagePath))
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.type = "image/*"
            startActivity(Intent.createChooser(shareIntent, "Compartir Foto"))
        } catch (e: Exception) {

        }
    }

    private fun saveImage(imagePath: String) {
        val image = File(imagePath)
        val bitmap = BitmapFactory.decodeFile(imagePath)

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            this@ImageActivity.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, image.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this@ImageActivity, "Foto Guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun deleteImage(imagePath: String) {
        try {
            // Delete image
            val imgFile = File(imagePath)
            imgFile.delete()

            // Close activity
            finish()
        } catch (e: Exception) {

        }
    }
}