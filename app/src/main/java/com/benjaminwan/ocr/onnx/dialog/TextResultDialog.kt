package com.benjaminwan.ocr.onnx.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.benjaminwan.ocr.onnx.R
import com.benjaminwan.ocr.onnx.databinding.DialogTextResultBinding
import com.benjaminwan.ocr.onnx.utils.hideSoftInput
import com.benjaminwan.ocr.onnx.utils.toClipboard

class TextResultDialog : BaseDialog(), View.OnClickListener {
    companion object {
        val instance: TextResultDialog
            get() {
                val dialog = TextResultDialog()
                dialog.setCanceledBack(true)
                dialog.setCanceledOnTouchOutside(false)
                dialog.setGravity(Gravity.CENTER)
                dialog.setAnimStyle(R.style.diag_top_down_up_animation)
                return dialog
            }
    }

    private var content: String = ""
    private var title: String = ""

    private var _binding: DialogTextResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTextResultBinding.inflate(inflater, viewGroup, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    override fun dismiss() {
        hideSoftInput()
        super.dismiss()
    }

    private fun initViews() {
        binding.negativeBtn.setOnClickListener(this)
        binding.positiveBtn.setOnClickListener(this)
        binding.contentEdit.setText(content)
        if (title.isNotEmpty()) {
            binding.titleTV.text = title
        }
    }

    fun setTitle(title: String): TextResultDialog {
        this.title = title
        return this
    }

    fun setContent(textContent: String): TextResultDialog {
        content = textContent
        return this
    }

    override fun onClick(view: View) {
        val resId = view.id
        if (resId == R.id.negativeBtn) {
            dismiss()
        } else if (resId == R.id.positiveBtn) {
            requireContext().toClipboard(content)
            this.dismiss()
        }
    }

}
