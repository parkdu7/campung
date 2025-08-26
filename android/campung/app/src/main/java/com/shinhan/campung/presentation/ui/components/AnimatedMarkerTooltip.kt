package com.shinhan.campung.presentation.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.shinhan.campung.R

class AnimatedMarkerTooltip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tooltipText: TextView
    private val tooltipBackground: GradientDrawable
    
    init {
        // 툴팁 텍스트 뷰 생성
        tooltipText = TextView(context).apply {
            setPadding(32, 16, 32, 16)
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(Color.WHITE)
            maxLines = 1
            setSingleLine(true)
        }
        
        // 툴팁 배경 생성 (둥근 사각형)
        tooltipBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#DD000000")) // 반투명 검정
            cornerRadius = 24f
            
            // 그림자 효과
            setStroke(2, Color.parseColor("#33FFFFFF"))
        }
        
        tooltipText.background = tooltipBackground
        
        // 레이아웃 파라미터 설정
        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        addView(tooltipText, layoutParams)
        
        // 초기에는 투명하게 설정
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
        visibility = View.GONE
    }
    
    fun showTooltip(text: String) {
        tooltipText.text = text
        visibility = View.VISIBLE
        
        // 스르륵 나타나는 애니메이션
        val fadeInAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 0.8f, 1.05f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 0.8f, 1.05f, 1f)
        
        fadeInAnimator.duration = 300
        scaleXAnimator.duration = 300
        scaleYAnimator.duration = 300
        
        fadeInAnimator.interpolator = android.view.animation.DecelerateInterpolator()
        scaleXAnimator.interpolator = android.view.animation.OvershootInterpolator(0.3f)
        scaleYAnimator.interpolator = android.view.animation.OvershootInterpolator(0.3f)
        
        fadeInAnimator.start()
        scaleXAnimator.start()
        scaleYAnimator.start()
    }
    
    fun hideTooltip(onComplete: (() -> Unit)? = null) {
        // 스르륵 사라지는 애니메이션
        val fadeOutAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
        val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.8f)
        val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.8f)
        
        fadeOutAnimator.duration = 200
        scaleXAnimator.duration = 200
        scaleYAnimator.duration = 200
        
        fadeOutAnimator.interpolator = android.view.animation.AccelerateInterpolator()
        scaleXAnimator.interpolator = android.view.animation.AccelerateInterpolator()
        scaleYAnimator.interpolator = android.view.animation.AccelerateInterpolator()
        
        fadeOutAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
                onComplete?.invoke()
            }
        })
        
        fadeOutAnimator.start()
        scaleXAnimator.start()
        scaleYAnimator.start()
    }
    
    fun updateText(text: String) {
        tooltipText.text = text
    }
}