package com.rtbishop.look4sat.data.usecase

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.rtbishop.look4sat.domain.usecase.IAddToCalendar

class AddToCalendar(private val context: Context) : IAddToCalendar {
    override fun invoke(name: String, aosTime: Long, losTime: Long) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setData(CalendarContract.Events.CONTENT_URI)
            putExtra(CalendarContract.Events.TITLE, name)
            putExtra(CalendarContract.Events.DESCRIPTION, "Look4Sat Pass")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, aosTime)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, losTime)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
