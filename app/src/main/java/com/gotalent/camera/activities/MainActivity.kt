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

/**
 * Main activity
 *
 * @constructor Main activity
 *
 * @author Yesid Rosas Toro
 * @since 1.0.0
 */
class MainActivity : AppCompatActivity() {
    /**
     * Request Camera and Write External Storage Permission
     */
    private val REQUEST_CAMERA_PERMISSION = 200
    private val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 201

    /**
     * Is flash on
     */
    private var isFlashOn = false

    /**
     * Orientations
     *
     * @property ROTATION_0   Rotation 0
     * @property ROTATION_90  Rotation 90
     * @property ROTATION_180 Rotation 180
     * @property ROTATION_270 Rotation 270
     */
    private val orientations = mapOf(
        Pair(Surface.ROTATION_0, 90),
        Pair(Surface.ROTATION_90, 0),
        Pair(Surface.ROTATION_180, 270),
        Pair(Surface.ROTATION_270, 180)
    )

    /**
     * Camera Preview
     *
     * Vista previa de la cámara
     */
    private lateinit var cameraPreview: TextureView

    /**
     * Camera Action Buttons
     */
    private lateinit var captureButton: FloatingActionButton
    private lateinit var flashButton: FloatingActionButton
    private lateinit var flipButton: FloatingActionButton

    /**
     * Camera Manager
     */
    private var cameraManager: CameraManager? = null

    /**
     * Camera Id
     *
     * Id de la camara seleccionada
     */
    private var cameraId = "0"

    /**
     * Camera Device
     *
     * Dispositivo de la camara seleccionado
     */
    private var cameraDevice: CameraDevice? = null

    /**
     * Camera Capture Session
     *
     * Sesión de captura de la camara
     */
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder

    private lateinit var imageDimension: Size

    /**
     * Background Thread and Handler
     *
     * Hilo de fondo y manejador
     */
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    /**
     * On create
     *
     * Genera la vista de la actividad y enlaza toda la logica con el layout
     *
     * @param savedInstanceState Instancia guardada de la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        cameraPreview = findViewById(R.id.camera_preview)
        cameraPreview.surfaceTextureListener = surfaceTextureListener

        captureButton = findViewById(R.id.button_save)
        flashButton = findViewById(R.id.button_flash)
        flipButton = findViewById(R.id.button_flip)

        captureButton.setOnClickListener { takePhoto() }
        flashButton.setOnClickListener { toggleFlash() }
        flipButton.setOnClickListener { flipCamera() }

        flashButton.setBackgroundColor(Color.WHITE)
    }

    /**
     * On resume
     *
     * Inicia los hilos de ejecución de la cámara
     */
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

    /**
     * On pause
     *
     * Detiene los hilos de ejecución de la cámara
     */
    override fun onPause() {
        super.onPause()

        try {
            backgroundThread?.quitSafely()
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            // No requerido
        }
    }

    /**
     * On create options menu
     *
     * @param menu Menu
     * @return confirmacion de creacion del menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mymenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * On options item selected
     *
     * @param item item seleccionado
     * @return confirmacion de seleccion del item
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.fotos_action) {
            val intent = Intent(this, ImageListActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * On request permissions result
     *
     * @param requestCode codigo de solicitud
     * @param permissions lista de permisos
     * @param grantResults lista de resultados de permisos
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, getString(R.string.permisos_camara), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Camera state call back
     */
    private val cameraStateCallBack = object : CameraDevice.StateCallback() {
        /**
         * On opened
         *
         * @param camera dispositivo de camara que se abrio
         */
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        /**
         * On disconnected
         *
         * @param camera dispositivo de camara que se desconecto
         */
        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
        }

        /**
         * On error
         *
         * @param camera dispositivo de camara que fallo
         * @param error codigo de error
         */
        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    /**
     * Capture session state call back
     */
    private val captureSessionStateCallBack = object : CameraCaptureSession.StateCallback() {
        /**
         * On configured
         *
         * @param session sesion de captura que se configuro
         */
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
        }

        /**
         * On configure failed
         *
         * @param session sesion de captura que fallo
         */
        override fun onConfigureFailed(session: CameraCaptureSession) {
            // No requerido
        }
    }

    /**
     * Surface texture listener
     */
    private val surfaceTextureListener = object : SurfaceTextureListener {
        /**
         * On surface texture available
         *
         * @param surface superficie de la camara
         * @param width  ancho de la superficie
         * @param height alto de la superficie
         */
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            startCamera()
        }

        /**
         * On surface texture size changed
         *
         * @param surface superficie de la camara
         * @param width nuevo ancho de la superficie
         * @param height nuevo alto de la superficie
         *
         * @suppress unused
         */
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // No requerido
        }

        /**
         * On surface texture destroyed
         *
         * @param surface superficie de la camara
         *
         * @return confirmacion de destruccion de la superficie
         *
         * @suppress unused
         */
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean { return true }

        /**
         * On surface texture updated
         *
         * @param surface superficie de la camara
         *
         * @suppress unused
         */
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // No requerido
        }
    }

    /**
     * Start camera
     *
     * Inicia la camara y la configura
     */
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

            val canFlash = checkFlash()
            flashButton.isEnabled = canFlash
            flashButton.setBackgroundColor(if (canFlash) Color.WHITE  else Color.DKGRAY)
            flashButton.setImageResource(R.drawable.ic_baseline_flash_on_24)
            isFlashOn = false
        } catch (e: CameraAccessException) {
            // No requerido
        }
    }

    /**
     * Create camera preview
     *
     * Crea la vista previa de la camara
     */
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

    /**
     * Take photo
     *
     * Toma una foto, la guarda de forma provisional y la muestra en otra actividad
     */
    private fun takePhoto() {
        val manager: CameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            val sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageReader::class.java)

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
                    intent.putExtra(getString(R.string.intent_image_path), file.absolutePath)
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
                    Toast.makeText(this@MainActivity, getString(R.string.fallo_sesion), Toast.LENGTH_SHORT).show()
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            // No requerido
        }
    }

    /**
     * Toggle flash
     *
     * Activa o desactiva el flash de la camara
     */
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

    /**
     * Flip camera
     *
     * Cambia la camara entre frontal y trasera
     */
    private fun flipCamera() {
        val cameraFront = getString(R.string.camera_front)
        val cameraBack  = getString(R.string.camera_back)

        cameraId = if (cameraId == cameraFront) cameraBack else cameraFront
        cameraDevice?.close()
        startCamera()
    }

    /**
     * Check flash
     *
     * Verifica si la camara tiene flash
     *
     * @return true si la camara tiene flash, false en caso contrario
     */
    private fun checkFlash(): Boolean {
        return try {
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!
        } catch (e: CameraAccessException) {
            false
        }
    }

    /**
     * Get orientation
     *
     * Obtiene la orientacion de la camara
     *
     * @param rotation rotacion de la pantalla
     * @param sensorOrientation orientacion del sensor
     * @return orientacion de la camara
     */
    private fun getOrientation(rotation: Int, sensorOrientation: Int): Int {
        return (orientations[rotation]!! + sensorOrientation + 270) % 360
    }
}

