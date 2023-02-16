package com.benjaminwan.ocr.onnx

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.benjaminwan.ocr.onnx.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding

    private fun initViews() {
        binding.galleryBtn.setOnClickListener(this)
        binding.cameraBtn.setOnClickListener(this)
        binding.imeiBtn.setOnClickListener(this)
        binding.plateBtn.setOnClickListener(this)
        binding.idCardBtn.setOnClickListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.galleryBtn -> {
                startActivity(Intent(this, GalleryActivity::class.java))
            }
            R.id.cameraBtn -> {
                startActivity(Intent(this, CameraActivity::class.java))
            }
            R.id.imeiBtn -> {
                startActivity(Intent(this, ImeiActivity::class.java))
            }
            R.id.plateBtn -> {
                startActivity(Intent(this, PlateActivity::class.java))
            }
            R.id.idCardBtn -> {
                startActivity(Intent(this, IdCardFrontActivity::class.java))
            }
            else -> {
            }
        }
    }
}