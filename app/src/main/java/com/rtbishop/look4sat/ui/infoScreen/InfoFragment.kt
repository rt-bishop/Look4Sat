/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2021 Arty Bishop (bishop.arty@gmail.com)
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
package com.rtbishop.look4sat.ui.infoScreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.Fragment
import com.rtbishop.look4sat.BuildConfig
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.FragmentInfoBinding

class InfoFragment : Fragment(R.layout.fragment_info) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FragmentInfoBinding.bind(view).apply {
            infoVersion.text =
                String.format(getString(R.string.info_version), BuildConfig.VERSION_NAME)
            infoGithub.setOnClickListener {
                gotoUrl("https://github.com/rt-bishop/Look4Sat/")
            }
            infoFdroid.setOnClickListener {
                gotoUrl("https://f-droid.org/en/packages/com.rtbishop.look4sat/")
            }
            infoBmc.setOnClickListener {
                gotoUrl("https://www.buymeacoffee.com/rtbishop")
            }
            val moveMethod = LinkMovementMethod.getInstance()
            infoThanks.movementMethod = moveMethod
            infoWarranty.movementMethod = moveMethod
        }
    }

    private fun gotoUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
