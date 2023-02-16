package com.benjaminwan.ocr.onnx

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.rationale.createDialogRationale
import com.benjaminwan.ocr.onnx.app.App
import com.benjaminwan.ocr.onnx.databinding.ActivityGalleryBinding
import com.benjaminwan.ocr.onnx.dialog.DebugDialog
import com.benjaminwan.ocr.onnx.dialog.TextResultDialog
import com.benjaminwan.ocr.onnx.utils.decodeUri
import com.benjaminwan.ocr.onnx.utils.showToast
import com.benjaminwan.ocrlibrary.OcrResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.orhanobut.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlin.math.max

class GalleryActivity : AppCompatActivity(), View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private lateinit var binding: ActivityGalleryBinding

    private var selectedImg: Bitmap? = null
    private var ocrResult: OcrResult? = null
    private var detectJob: Job? = null

    private val glideOptions =
        RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)

    private fun initViews() {
        binding.selectBtn.setOnClickListener(this)
        binding.detectBtn.setOnClickListener(this)
        binding.resultBtn.setOnClickListener(this)
        binding.debugBtn.setOnClickListener(this)
        binding.benchBtn.setOnClickListener(this)
        binding.stopBtn.setOnClickListener(this)
        binding.stopBtn.isEnabled = false
        binding.doAngleSw.isChecked = App.ocrEngine.doAngle
        binding.mostAngleSw.isChecked = App.ocrEngine.mostAngle
        updatePadding(App.ocrEngine.padding)
        updateBoxScoreThresh((App.ocrEngine.boxScoreThresh * 100).toInt())
        updateBoxThresh((App.ocrEngine.boxThresh * 100).toInt())
        updateUnClipRatio((App.ocrEngine.unClipRatio * 10).toInt())
        binding.paddingSeekBar.setOnSeekBarChangeListener(this)
        binding.boxScoreThreshSeekBar.setOnSeekBarChangeListener(this)
        binding.boxThreshSeekBar.setOnSeekBarChangeListener(this)
        binding.maxSideLenSeekBar.setOnSeekBarChangeListener(this)
        binding.scaleUnClipRatioSeekBar.setOnSeekBarChangeListener(this)
        binding.doAngleSw.setOnCheckedChangeListener { _, isChecked ->
            App.ocrEngine.doAngle = isChecked
            binding.mostAngleSw.isEnabled = isChecked
        }
        binding.mostAngleSw.setOnCheckedChangeListener { _, isChecked ->
            App.ocrEngine.mostAngle = isChecked
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.ocrEngine.doAngle = true//相册识别时，默认启用文字方向检测
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        val rationaleHandler = createDialogRationale(R.string.storage_permission) {
            onPermission(
                Permission.READ_EXTERNAL_STORAGE, "请点击允许"
            )
        }

        if (!isAllGranted(Permission.READ_EXTERNAL_STORAGE)) {
            askForPermissions(
                Permission.READ_EXTERNAL_STORAGE,
                rationaleHandler = rationaleHandler
            ) { result ->
                val permissionGranted: Boolean =
                    result.isAllGranted(
                        Permission.READ_EXTERNAL_STORAGE
                    )
                if (!permissionGranted) {
                    showToast("未获取权限，应用无法正常使用！")
                }
            }
        }
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.selectBtn -> {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = "image/*"
                }
                startActivityForResult(
                    intent, REQUEST_SELECT_IMAGE
                )
            }
            R.id.detectBtn -> {
                val img = selectedImg
                if (img == null) {
                    showToast("请先选择一张图片")
                    return
                }
                val ratio = binding.maxSideLenSeekBar.progress.toFloat() / 100.toFloat()
                val maxSize = max(img.width, img.height)
                val maxSideLen = (ratio * maxSize).toInt()
                detectJob = detect(img, maxSideLen)
            }
            R.id.stopBtn -> {
                detectJob?.cancel()
                clearLoading()
                ocrResult = null
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
            R.id.benchBtn -> {
                val img = selectedImg
                if (img == null) {
                    showToast("请先选择一张图片")
                    return
                }
                val loop = 50
                showToast("开始循环${loop}次的测试")
                benchmark(img, loop)
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
        val ratio = progress.toFloat() / 100.toFloat()
        if (selectedImg != null) {
            val img = selectedImg ?: return
            val maxSize = max(img.width, img.height)
            val maxSizeLen = (ratio * maxSize).toInt()
            binding.maxSideLenTv.text = "MaxSideLen:$maxSizeLen(${ratio * 100}%)"
        } else {
            binding.maxSideLenTv.text = "MaxSideLen:0(${ratio * 100}%)"
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_SELECT_IMAGE) {
            val imgUri = data.data ?: return
            Glide.with(this).load(imgUri).apply(glideOptions).into(binding.imageView)
            selectedImg = decodeUri(imgUri)
            updateMaxSideLen(binding.maxSideLenSeekBar.progress)
            clearLastResult()
        }
    }

    private fun showLoading() {
        Glide.with(this).load(R.drawable.loading_anim).into(binding.imageView)
    }

    private fun clearLoading() {
        Glide.with(this).clear(binding.imageView)
    }

    private fun clearLastResult() {
        binding.timeTV.text = ""
        ocrResult = null
    }

    private fun benchmark(img: Bitmap, loop: Int) = flow {
        val aveTime = App.ocrEngine.benchmark(img, loop)
        //showToast("循环${loop}次，平均时间${aveTime}ms")
        emit(aveTime)
    }.flowOn(Dispatchers.IO)
        .onStart {
            showLoading()
            binding.benchBtn.isEnabled = false
        }
        .onCompletion {
            binding.benchBtn.isEnabled = true
            clearLoading()
        }
        .onEach {
            binding.timeTV.text = "循环${loop}次，平均时间${it}ms"
        }
        .launchIn(lifecycleScope)

    private fun detect(img: Bitmap, reSize: Int) = flow {
        val boxImg: Bitmap = Bitmap.createBitmap(
            img.width, img.height, Bitmap.Config.ARGB_8888
        )
        Logger.i("selectedImg=${img.height},${img.width} ${img.config}")
        val start = System.currentTimeMillis()
        val ocrResult = App.ocrEngine.detect(img, boxImg, reSize)
        val end = System.currentTimeMillis()
        val time = "time=${end - start}ms"
        emit(ocrResult)
    }.flowOn(Dispatchers.IO)
        .onStart {
            showLoading()
            binding.detectBtn.isEnabled = false
            binding.stopBtn.isEnabled = true
        }
        .onCompletion {
            binding.detectBtn.isEnabled = true
            binding.stopBtn.isEnabled = false
            binding.resultBtn.callOnClick()
        }
        .onEach {
            ocrResult = it
            binding.timeTV.text = "识别时间:${it.detectTime.toInt()}ms"
            Glide.with(this).load(it.boxImg).apply(glideOptions).into(binding.imageView)
            Logger.i("$it")
        }.launchIn(lifecycleScope)

    companion object {
        const val REQUEST_SELECT_IMAGE = 666
    }


}