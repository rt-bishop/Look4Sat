package com.rtbishop.look4sat.data.usecase

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.rtbishop.look4sat.domain.usecase.IOpenWebUrl

class OpenWebUrl(private val context: Context) : IOpenWebUrl {
    override fun invoke(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
        }
    }
}
