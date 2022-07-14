package com.gotalent.camera.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.gotalent.camera.R
import java.io.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_CAMERA_PERMISSION = 200
    private val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 201

    private val CAMERA_FRONT = "1"
    private val CAMERA_BACK = "0"

    private var isFlashOn = false

    private val ORIENTATIONS = mapOf(
        Pair(Surface.ROTATION_0, 90),
        Pair(Surface.ROTATION_90, 0),
        Pair(Surface.ROTATION_180, 270),
        Pair(Surface.ROTATION_270, 180)
    )

    private lateinit var cameraPreview: TextureView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var flashButton: FloatingActionButton
    private lateinit var flipButton: FloatingActionButton

    private var cameraManager: CameraManager? = null

    private var cameraId = CAMERA_BACK
    private var cameraDevice: CameraDevice? = null

    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    private lateinit var imageDimension: Size

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    /*******************************/
    /** Android Framework methods **/
    /*******************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the camera manager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        // Initialize the camera preview canvas
        cameraPreview = findViewById(R.id.camera_preview)
        cameraPreview.surfaceTextureListener = surfaceTextureListener

        // Declare action buttons
        captureButton = findViewById(R.id.button_save)
        flashButton = findViewById(R.id.button_flash)
        flipButton = findViewById(R.id.button_flip)

        // Set action buttons listeners
        captureButton.setOnClickListener { takePhoto() }
        flashButton.setOnClickListener { toggleFlash() }
        flipButton.setOnClickListener { flipCamera() }

        // Aditional configurations
        flashButton.setBackgroundColor(Color.WHITE)
    }

    override fun onResume() {
        super.onResume()

        // Set main thread handler
        backgroundThread = HandlerThread("Camera Background")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        if (cameraPreview.isAvailable) {
            startCamera()
        } else {
            cameraPreview.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        super.onPause()

        // Stop the main thread handler
        try {
            backgroundThread?.quitSafely()
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // No requerido
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mymenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.fotos_action) {
            val intent = Intent(this, ImageListActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Se requieren permisos de acceso a la camara", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /***********************/
    /** Camera Interfaces **/
    /***********************/

    private val cameraStateCallBack = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    private val captureSessionStateCallBack = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            // No requerido
        }
    }

    private val surfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // No requerido
        }
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { return true }
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // No requerido
        }
    }

    /***************************/
    /** Camera Initialization **/
    /***************************/

    private fun startCamera() {
        try {
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            imageDimension = map!!.getOutputSizes(SurfaceTexture::class.java)[0]

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
                return
            }

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION)
                return
            }

            cameraManager!!.openCamera(cameraId, cameraStateCallBack, null)

            // Button flash setup
            val canFlash = checkFlash()
            flashButton.isEnabled = canFlash
            flashButton.setBackgroundColor(if (canFlash) Color.WHITE  else Color.DKGRAY)
            flashButton.setImageResource(R.drawable.ic_baseline_flash_on_24)
            isFlashOn = false
        } catch (e: CameraAccessException) {
            // No requerido
        }
    }

    private fun createCameraPreview() {
        try {
            val texture = cameraPreview.surfaceTexture
            texture?.setDefaultBufferSize(imageDimension.width, imageDimension.height)
            val surface = Surface(texture)
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)
            cameraDevice?.createCaptureSession(listOf(surface), captureSessionStateCallBack, null)
        } catch (e: CameraAccessException) {
            // No requerido
        }
    }

    /********************/
    /** Camera Actions **/
    /********************/

    private fun takePhoto() {
        val manager: CameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            val sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageReader::class.java)

            // Default Values
            var width = 640
            var height = 480

            if (sizes.isNotEmpty()) {
                width = sizes[0].width
                height = sizes[0].height
            }

            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

            val captureBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureBuilder?.addTarget(reader.surface)
            captureBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            val rotation = windowManager.defaultDisplay.rotation
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
            captureBuilder!!.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation, sensorOrientation!!))

            val file = File.createTempFile("test", ".jpg")
            reader.setOnImageAvailableListener({
                val image = it.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)

                var outputStream: FileOutputStream? = null
                try {
                    outputStream = FileOutputStream(file)
                    outputStream.write(bytes)
                } catch (e: IOException) {
                    // No requerido
                } finally {
                    image.close()
                    outputStream?.close()
                }

                file.also {
                    val intent = Intent(this@MainActivity, ImageActivity::class.java)
                    intent.putExtra("imagePath", file.absolutePath)
                    startActivity(intent)
                }
            }, backgroundHandler)

            cameraDevice!!.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                session.close()
                            }
                        }, backgroundHandler)
                    } catch (e: CameraAccessException) {
                        // No requerido
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(this@MainActivity, "Fallo en la configuración de la sesión", Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            // No requerido
        }
    }

    private fun toggleFlash() {
        try {
            if (checkFlash()) {
                if (!isFlashOn) {
                    flashButton.setImageResource(R.drawable.ic_baseline_flash_off_24)
                } else {
                    flashButton.setImageResource(R.drawable.ic_baseline_flash_on_24)
                }

                isFlashOn = !isFlashOn
                val captureFlashMode = if (isFlashOn) CaptureRequest.FLASH_MODE_TORCH else CameraMetadata.FLASH_MODE_OFF

                cameraCaptureSession.stopRepeating()
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, captureFlashMode)
                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null)
            }
        } catch (e: CameraAccessException) {
            // No requerido
        }
    }

    private fun flipCamera() {
        cameraId = if (cameraId == CAMERA_FRONT) CAMERA_BACK else CAMERA_FRONT
        cameraDevice?.close()
        startCamera()
    }

    /******************************/
    /** Camera Basic Subroutines **/
    /******************************/

    private fun checkFlash(): Boolean {
        return try {
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!
        } catch (e: CameraAccessException) {
            false
        }
    }

    private fun getOrientation(rotation: Int, sensorOrientation: Int): Int {
        return (ORIENTATIONS[rotation]!! + sensorOrientation + 270) % 360
    }
}

