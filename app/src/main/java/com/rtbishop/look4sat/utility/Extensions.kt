package com.rtbishop.look4sat.utility

import android.view.View
import com.google.android.material.snackbar.Snackbar

object Extensions {

    fun String.snack(view: View) {
        Snackbar.make(view, this, Snackbar.LENGTH_SHORT).show()
    }
}