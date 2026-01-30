/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
