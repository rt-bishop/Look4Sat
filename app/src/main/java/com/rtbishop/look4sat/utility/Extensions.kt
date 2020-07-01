package com.rtbishop.look4sat.utility

import android.content.Context
import android.widget.Toast

object Extensions {

    fun String.toast(context: Context, duration: Int = Toast.LENGTH_SHORT): Toast {
        return Toast.makeText(context, this, duration).apply { show() }
    }
}