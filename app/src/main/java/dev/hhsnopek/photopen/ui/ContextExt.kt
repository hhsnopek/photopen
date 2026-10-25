package dev.hhsnopek.photopen.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The activity behind a composable's context, or null in a preview/test host. */
fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
