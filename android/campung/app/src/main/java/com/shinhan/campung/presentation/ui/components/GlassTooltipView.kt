package com.shinhan.campung.presentation.ui.components

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt

/**
 * 글래스모피즘 툴팁 뷰 (개선된 버전)
 */
class GlassTooltipView(context: Context) : FrameLayout(context) {

    @ColorInt private val fillColor: Int = Color.parseColor("#E6FFFFFF") // 90% 흰색 (더 진하게)
    @ColorInt private val strokeColor: Int = Color.parseColor("#4DFFFFFF") // 30% 흰색

    private val textView = TextView(context).apply {
        setTextColor(Color.parseColor("#1A1A1A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        setShadowLayer(2f, 0f, dp(1).toFloat(), Color.parseColor("#33FFFFFF"))
        maxLines = 1
        isSingleLine = true
        includeFontPadding = false
    }

    init {
        setPadding(dp(3), dp(3), dp(3), dp(3))
        clipToPadding = false
        clipChildren = false
        elevation = dp(12).toFloat() // 더 높은 elevation

        background = GlassBubbleDrawable(
            fillColor = fillColor,
            strokeColor = strokeColor,
            cornerRadius = dpF(12f),
            strokeWidth = dpF(1.5f)
        )

        addView(textView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        // 초기 상태 설정 (애니메이션을 위해)
        scaleX = 0f
        scaleY = 0f
        alpha = 0f
    }

    fun setText(text: CharSequence) {
        textView.text = text
    }

    /**
     * 나타나는 애니메이션
     */
    fun showWithAnimation() {
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.3f))
            .start()
    }

    /**
     * 사라지는 애니메이션
     */
    fun hideWithAnimation(onComplete: (() -> Unit)? = null) {
        animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction { onComplete?.invoke() }
            .start()
    }

    private fun dp(px: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px.toFloat(), resources.displayMetrics).toInt()

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

    private fun dpF(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}

/**
 * 글래스 말풍선 드로어블 (개선된 버전)
 */
private class GlassBubbleDrawable(
    @ColorInt private val fillColor: Int,
    @ColorInt private val strokeColor: Int,
    private val cornerRadius: Float,
    private val strokeWidth: Float
) : Drawable() {
    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFilterBitmap = true
    }
    private val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        isFilterBitmap = true
    }
    private val paintHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }
    private val paintShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    override fun draw(canvas: Canvas) {
        val r = RectF(bounds)

        // 그림자 효과
        val shadowOffset = 2f
        val shadowRect = RectF(
            r.left + shadowOffset,
            r.top + shadowOffset,
            r.right + shadowOffset,
            r.bottom + shadowOffset
        )
        paintShadow.color = Color.parseColor("#1A000000")
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, paintShadow)

        // 배경 그리기
        paintFill.color = fillColor
        canvas.drawRoundRect(r, cornerRadius, cornerRadius, paintFill)

        // 상단 하이라이트 (유리 느낌 강화)
        val highlightGradient = LinearGradient(
            r.left, r.top, r.left, r.centerY(),
            Color.argb(120, 255, 255, 255),
            Color.argb(20, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        paintHighlight.shader = highlightGradient

        val highlightRect = RectF(r.left + 2f, r.top + 2f, r.right - 2f, r.centerY() + 4f)
        canvas.save()
        canvas.clipRoundRect(highlightRect, cornerRadius - 2f, cornerRadius - 2f)
        canvas.drawRoundRect(r, cornerRadius, cornerRadius, paintHighlight)
        canvas.restore()

        // 외곽선 그리기
        paintStroke.color = strokeColor
        paintStroke.strokeWidth = strokeWidth
        canvas.drawRoundRect(r, cornerRadius, cornerRadius, paintStroke)
    }

    override fun setAlpha(alpha: Int) {
        paintFill.alpha = alpha
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    override fun setColorFilter(colorFilter: ColorFilter?) {}

    // clipRoundRect 호환성을 위한 확장
    private fun Canvas.clipRoundRect(rect: RectF, rx: Float, ry: Float) {
        val path = Path().apply {
            addRoundRect(rect, rx, ry, Path.Direction.CW)
        }
        clipPath(path)
    }
}