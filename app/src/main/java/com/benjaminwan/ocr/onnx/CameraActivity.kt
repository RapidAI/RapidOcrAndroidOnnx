package com.benjaminwan.ocr.onnx

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
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
import com.benjaminwan.ocr.onnx.databinding.ActivityCameraBinding
import com.benjaminwan.ocr.onnx.dialog.DebugDialog
import com.benjaminwan.ocr.onnx.dialog.TextResultDialog
import com.benjaminwan.ocr.onnx.utils.showToast
import com.benjaminwan.ocrlibrary.OcrResult
import com.bumptech.glide.Glide
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlin.math.max

class CameraActivity : AppCompatActivity(), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private lateinit var binding: ActivityCameraBinding

    private var ocrResult: OcrResult? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var detectJob: Job? = null

    private fun initViews() {
        binding.clearBtn.setOnClickListener(this)
        binding.detectBtn.setOnClickListener(this)
        binding.resultBtn.setOnClickListener(this)
        binding.debugBtn.setOnClickListener(this)
        binding.stopBtn.setOnClickListener(this)
        binding.stopBtn.isEnabled = false
        updatePadding(App.ocrEngine.padding)
        updateBoxScoreThresh((App.ocrEngine.boxScoreThresh * 100).toInt())
        updateBoxThresh((App.ocrEngine.boxThresh * 100).toInt())
        updateUnClipRatio((App.ocrEngine.unClipRatio * 10).toInt())
        binding.paddingSeekBar.setOnSeekBarChangeListener(this)
        binding.boxScoreThreshSeekBar.setOnSeekBarChangeListener(this)
        binding.boxThreshSeekBar.setOnSeekBarChangeListener(this)
        binding.maxSideLenSeekBar.setOnSeekBarChangeListener(this)
        binding.scaleUnClipRatioSeekBar.setOnSeekBarChangeListener(this)
        binding.cameraLensView.postDelayed({
            updateMaxSideLen(100)
        }, 500)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.ocrEngine.doAngle = false//摄像头一般不需要考虑倒过来的情况
        binding = ActivityCameraBinding.inflate(layoutInflater)
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
                }
            }
        } else {
            startCamera()
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.clearBtn -> {
                clearLastResult()
            }
            R.id.detectBtn -> {
                val width = binding.cameraLensView.measuredWidth * 9 / 10
                val height = binding.cameraLensView.measuredHeight * 9 / 10
                val ratio = binding.maxSideLenSeekBar.progress.toFloat() / 100.toFloat()
                val maxSize = max(width, height)
                val maxSideLen = (ratio * maxSize).toInt()
                detectJob = detect(maxSideLen)
            }
            R.id.stopBtn -> {
                detectJob?.cancel()
                clearLastResult()
            }
            R.id.resultBtn -> {
                val result = ocrResult ?: return
                TextResultDialog.instance
                    .setTitle("识别结果")
                    .setContent(result.strRes)
                    .show(supportFragmentManager, "TextResultDialog")
            }
            R.id.debugBtn -> {
                val result = ocrResult ?: return
                DebugDialog.instance
                    .setTitle("调试信息")
                    .setResult(result)
                    .show(supportFragmentManager, "DebugDialog")
            }
            else -> {
            }
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        seekBar ?: return
        when (seekBar.id) {
            R.id.maxSideLenSeekBar -> {
                updateMaxSideLen(progress)
            }
            R.id.paddingSeekBar -> {
                updatePadding(progress)
            }
            R.id.boxScoreThreshSeekBar -> {
                updateBoxScoreThresh(progress)
            }
            R.id.boxThreshSeekBar -> {
                updateBoxThresh(progress)
            }
            R.id.scaleUnClipRatioSeekBar -> {
                updateUnClipRatio(progress)
            }
            else -> {
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    private fun updateMaxSideLen(progress: Int) {
        val width = binding.cameraLensView.measuredWidth * 9 / 10
        val height = binding.cameraLensView.measuredHeight * 9 / 10
        val ratio = progress.toFloat() / 100.toFloat()
        val maxSize = max(width, height)
        val maxSideLen = (ratio * maxSize).toInt()
        Logger.i("======$width,$height,$ratio,$maxSize,$maxSideLen")
        binding.maxSideLenTv.text = "MaxSideLen:$maxSideLen(${ratio * 100}%)"
    }

    private fun updatePadding(progress: Int) {
        binding.paddingTv.text = "Padding:$progress"
        App.ocrEngine.padding = progress
    }

    private fun updateBoxScoreThresh(progress: Int) {
        val thresh = progress.toFloat() / 100.toFloat()
        binding.boxScoreThreshTv.text = "${getString(R.string.box_score_thresh)}:$thresh"
        App.ocrEngine.boxScoreThresh = thresh
    }

    private fun updateBoxThresh(progress: Int) {
        val thresh = progress.toFloat() / 100.toFloat()
        binding.boxThreshTv.text = "BoxThresh:$thresh"
        App.ocrEngine.boxThresh = thresh
    }

    private fun updateUnClipRatio(progress: Int) {
        val scale = progress.toFloat() / 10.toFloat()
        binding.unClipRatioTv.text = "${getString(R.string.box_un_clip_ratio)}:$scale"
        App.ocrEngine.unClipRatio = scale
    }

    private fun showLoading() {
        binding.loadingImg.visibility = View.VISIBLE
        Glide.with(this).load(R.drawable.loading_anim).into(binding.loadingImg)
    }

    private fun hideLoading() {
        binding.loadingImg.visibility = View.GONE
    }

    private fun clearLastResult() {
        ocrResult = null
        binding.cameraLensView.cameraLensBitmap = null
        binding.timeTV.text = ""
    }

    private fun detect(reSize: Int) = flow {
        emit(binding.cameraLensView.cropCameraLensRectBitmap(binding.viewFinder.bitmap, false))
    }.flowOn(Dispatchers.Main)
        .map { src ->
            val boxImg: Bitmap = Bitmap.createBitmap(
                src.width, src.height, Bitmap.Config.ARGB_8888
            )
            Logger.i("selectedImg=${src.height},${src.width} ${src.config}")
            App.ocrEngine.detect(src, boxImg, reSize)
        }
        .flowOn(Dispatchers.IO)
        .onStart {
            showLoading()
            binding.detectBtn.isEnabled = false
            binding.stopBtn.isEnabled = true
        }
        .onCompletion {
            binding.detectBtn.isEnabled = true
            binding.stopBtn.isEnabled = false
            hideLoading()
            binding.resultBtn.callOnClick()
        }
        .onEach {
            ocrResult = it
            binding.timeTV.text = "识别时间:${it.detectTime.toInt()}ms"
            binding.cameraLensView.cameraLensBitmap = it.boxImg
        }
        .launchIn(lifecycleScope)


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
                Logger.e("Use case binding failed", exc.message.toString())
            }

        }, ContextCompat.getMainExecutor(this))
    }

}