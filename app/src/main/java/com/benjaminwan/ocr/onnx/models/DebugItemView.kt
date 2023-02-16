package com.benjaminwan.ocr.onnx.models

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.airbnb.epoxy.AfterPropsSet
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.benjaminwan.ocr.onnx.databinding.RvDebugViewItemBinding

// The ModelView annotation is used on Views to have models generated from those views.
// This is pretty straightforward with Kotlin, but properties need some special handling.
@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class DebugItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: RvDebugViewItemBinding =
        RvDebugViewItemBinding.inflate(LayoutInflater.from(this.context), this, true)

    init {

    }

    // 2. Or you can use lateinit
    @TextProp
    lateinit var index: CharSequence

    @TextProp
    lateinit var boxPoint: CharSequence

    @TextProp
    lateinit var boxScore: CharSequence

    @TextProp
    lateinit var angleIndex: CharSequence

    @TextProp
    lateinit var angleScore: CharSequence

    @TextProp
    lateinit var angleTime: CharSequence

    @TextProp
    lateinit var text: CharSequence

    @TextProp
    lateinit var charScores: CharSequence

    @TextProp
    lateinit var crnnTime: CharSequence

    @TextProp
    lateinit var blockTime: CharSequence

    @AfterPropsSet
    fun useProps() {
        binding.blockIndexTv.text = index
        binding.content.boxPointTv.text = boxPoint
        binding.content.boxScoreTv.text = boxScore
        binding.content.angleIndexTv.text = angleIndex
        binding.content.angleScoreTv.text = angleScore
        binding.content.angleTimeTv.text = angleTime
        binding.content.textTv.text = text
        binding.content.charScoresTv.text = charScores
        binding.content.crnnTimeTv.text = crnnTime
        binding.content.blockTimeTv.text = blockTime
    }
}
