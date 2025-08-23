package com.shinhan.campung.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

// View 확장 함수들
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

// Context 확장 함수들
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// Fragment 확장 함수들
fun Fragment.showToast(message: String) {
    requireContext().showToast(message)
}

fun Fragment.showLongToast(message: String) {
    requireContext().showLongToast(message)
}