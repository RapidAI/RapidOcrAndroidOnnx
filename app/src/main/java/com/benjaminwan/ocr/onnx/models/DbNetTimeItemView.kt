package com.benjaminwan.ocr.onnx.models

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.airbnb.epoxy.AfterPropsSet
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.benjaminwan.ocr.onnx.databinding.RvDbnetTimeItemBinding

// The ModelView annotation is used on Views to have models generated from those views.
// This is pretty straightforward with Kotlin, but properties need some special handling.
@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class DbNetTimeItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding: RvDbnetTimeItemBinding =
        RvDbnetTimeItemBinding.inflate(LayoutInflater.from(this.context), this, true)

    init {

    }

    // 2. Or you can use lateinit
    @TextProp
    lateinit var dbNetTimeStr: CharSequence

    @AfterPropsSet
    fun useProps() {
        binding.dbNetTimeTv.text = dbNetTimeStr
    }
}
