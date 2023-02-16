package com.benjaminwan.ocr.onnx

import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.rationale.createDialogRationale
import com.benjaminwan.ocr.onnx.app.App
import com.benjaminwan.ocr.onnx.databinding.ActivityPlateBinding
import com.benjaminwan.ocr.onnx.utils.getMatchPlateStr
import com.benjaminwan.ocr.onnx.utils.showToast
import com.benjaminwan.ocr.onnx.utils.trimBlankAndSymbols
import com.benjaminwan.ocrlibrary.OcrFailed
import com.benjaminwan.ocrlibrary.OcrResult
import com.benjaminwan.ocrlibrary.OcrStop
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.math.max

class PlateActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPlateBinding

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var detectStart: Boolean = false

    private val vibrator: Vibrator by lazy {
        applicationContext.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
    }

    //手机振动
    private fun vibration() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(
                100, VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(vibrationEffect)
        } else {
            vibrator.vibrate(100)
        }
    }

    private fun initViews() {
        binding.startBtn.setOnClickListener(this)
        binding.stopBtn.setOnClickListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.ocrEngine.doAngle = false//摄像头拍摄一般都是正的，不需要判断方向
        App.ocrEngine.padding = 100
        App.ocrEngine.boxScoreThresh = 0.2f
        App.ocrEngine.unClipRatio = 2.0f
        binding = ActivityPlateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        val rationaleHandler = createDialogRationale(R.string.app_permission) {
            onPermission(
                Permission.CAMERA, "请点击允许"
            )
        }

        if (!isAllGranted(Permission.CAMERA)) {
            askForPermissions(
                Permission.CAMERA,
                rationaleHandler = rationaleHandler
            ) { result ->
                val permissionGranted: Boolean =
                    result.isAllGranted(
                        Permission.CAMERA
                    )
                if (!permissionGranted) {
                    showToast("未获取权限，应用无法正常使用！")
                } else {
                    startCamera()
                    binding.viewFinder.postDelayed({ detectLoop() }, 100)
                }
            }
        } else {
            startCamera()
            binding.viewFinder.postDelayed({ detectLoop() }, 100)
        }
    }

    override fun onStop() {
        super.onStop()
        setDetectState(false)
    }

    private fun setResult() {
        val text = binding.resultEdit.text.toString().trim()
        if (text.isNotEmpty()) {
            val result = Intent().apply {
                putExtra("scanResult", text)
            }
            setResult(1, result)
        }
    }

    override fun onBackPressed() {
        setResult()
        super.onBackPressed()
    }

    private fun showResult(result: OcrResult) {
        binding.cameraLensView.cameraLensBitmap = result.boxImg
        binding.resultEdit.setText(result.strRes)
        binding.resultEdit.setSelection(result.strRes.length)
    }


    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.startBtn -> {
                detectLoop()
            }
            R.id.stopBtn -> {
                detectStart = false
            }
        }
    }

    private fun detectOnce(bitmap: Bitmap): OcrResult {
        val maxSize = max(bitmap.height, bitmap.width)
        val boxImg = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        return App.ocrEngine.detect(bitmap, boxImg, maxSize / 2)
    }

    private fun setDetectState(isStart: Boolean) {
        detectStart = isStart
        binding.startBtn.isEnabled = !isStart
        binding.stopBtn.isEnabled = isStart
        binding.resultEdit.isEnabled = !isStart
    }

    private fun getBitMap(): Bitmap? {
        val camPic = binding.viewFinder.bitmap ?: return null
        if (camPic.width <= 0 || camPic.height <= 0) return null
        return binding.cameraLensView.cropCameraLensRectBitmap(camPic, false)
    }

    private fun detectLoop() {
        binding.cameraLensView.cameraLensBitmap = null
        binding.resultEdit.setText("")
        setDetectState(true)
        flow {
            var success: OcrResult? = null
            do {
                val bitmap = withContext(Dispatchers.Main) { getBitMap() }
                bitmap ?: continue
                val once = detectOnce(bitmap)
                val text = once.strRes.trimBlankAndSymbols().uppercase()
                Logger.i(text)
                val matchId = getMatchPlateStr(text)
                if (matchId != null) {
                    success = once.copy(strRes = matchId)
                }
            } while (success == null && detectStart)
            val result = success ?: if (!detectStart) {
                OcrStop
            } else {
                OcrFailed
            }
            emit(result)
        }
            .flowOn(Dispatchers.IO)
            .onEach {
                when (it) {
                    is OcrResult -> {
                        setDetectState(false)
                        vibration()
                        showResult(it)
                    }
                    is OcrStop -> {
                        setDetectState(false)
                    }
                    is OcrFailed -> {
                        detectLoop()
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            val imageCaptureBuilder = ImageCapture.Builder()
            imageCaptureBuilder.setTargetAspectRatio(AspectRatio.RATIO_4_3)
            imageCapture = imageCaptureBuilder.build()

            // Select back camera
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            } catch (exc: Exception) {
                Logger.e("Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

}