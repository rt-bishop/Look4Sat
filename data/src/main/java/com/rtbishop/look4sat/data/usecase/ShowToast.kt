package com.rtbishop.look4sat.data.usecase

import android.content.Context
import android.widget.Toast
import com.rtbishop.look4sat.domain.usecase.IShowToast

class ShowToast(private val context: Context) : IShowToast {
    override fun invoke(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
