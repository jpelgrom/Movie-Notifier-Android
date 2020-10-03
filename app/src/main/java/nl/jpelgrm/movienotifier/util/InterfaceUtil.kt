package nl.jpelgrm.movienotifier.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object InterfaceUtil {
    @JvmStatic
    fun clearFocus(activity: Activity) {
        val current = activity.currentFocus
        current?.clearFocus()
    }

    @JvmStatic
    fun showKeyboard(activity: Activity, view: View?) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }
}