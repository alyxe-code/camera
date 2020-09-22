package com.p2lem8dev.testcameraapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.p2lem8dev.testcameraapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), MainViewModel.Navigation {

    private lateinit var binding: ActivityMainBinding

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraControl: CameraControl

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            viewModelStore,
            MainViewModelFactory
        )[MainViewModel::class.java]

        viewModel.navigation.observe(this, this)
    }

    private var snackbar: Snackbar? = null
    private fun showSnackbar(message: String) {
        snackbar?.dismiss()
        snackbar = Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar?.show()
    }

    override fun requestPermissionCamera() = requestPermission(Manifest.permission.CAMERA) {
        viewModel.onPermissionReceived(camera = it)
    }

    override fun requestPermissionStorage() = requestPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) {
        viewModel.onPermissionReceived(storage = it)
    }

    private fun requestPermission(
        permission: String,
        onReceived: (Boolean) -> Unit
    ) {
        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            ) != PackageManager.PERMISSION_GRANTED -> {
                onReceived(false)
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    REQUEST_STORAGE_PERMISSION_CODE
                )
            }
            else -> onReceived(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION_CODE -> viewModel.onPermissionReceived(
                camera = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            )
            REQUEST_STORAGE_PERMISSION_CODE -> viewModel.onPermissionReceived(
                storage = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            )
        }
    }

    override fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            preview.setSurfaceProvider(binding.preview.surfaceProvider)

            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                cameraControl = camera.cameraControl
            } catch (t: Throwable) {
                t.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(applicationContext))
    }

    override fun takePicture() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            applicationContext.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {

                val calendar = Calendar.getInstance()
                calendar.time = Date()

                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    getString(
                        R.string.save_picture_file_name,
                        calendar[Calendar.DAY_OF_MONTH],
                        calendar[Calendar.MONTH],
                        calendar[Calendar.YEAR],
                        calendar[Calendar.HOUR],
                        calendar[Calendar.MINUTE],
                        calendar[Calendar.SECOND],
                    )
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
        ).build()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(applicationContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri ?: return
                    viewModel.onTakePictureSucceeded(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onTakePictureFailure(exception)
                    showSnackbar(
                        exception.localizedMessage
                            ?: exception.message
                            ?: "Something gone wrong..."
                    )
                }
            }
        )
    }


    object MainViewModelFactory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = MainViewModel() as T
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION_CODE = 10
        private const val REQUEST_STORAGE_PERMISSION_CODE = 11
    }
}