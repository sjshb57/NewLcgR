package top.easelink.lcg.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import top.easelink.framework.utils.isOnMainThread
import top.easelink.lcg.appinit.LCGApp

private val mainScope = MainScope()

fun showMessage(msg: String) {
    if (isOnMainThread()) {
        Toast.makeText(LCGApp.context, msg, Toast.LENGTH_SHORT).show()
    } else {
        mainScope.launch {
            Toast.makeText(LCGApp.context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}

fun showMessage(@StringRes resource: Int) {
    if (isOnMainThread()) {
        Toast.makeText(LCGApp.context, resource, Toast.LENGTH_SHORT).show()
    } else {
        mainScope.launch {
            Toast.makeText(LCGApp.context, resource, Toast.LENGTH_SHORT).show()
        }
    }
}

fun showMessage(context: Context, msg: String) {
    if (isOnMainThread()) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    } else {
        mainScope.launch {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}