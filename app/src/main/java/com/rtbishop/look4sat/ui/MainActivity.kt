/*
 * Look4Sat. Amateur radio and weather satellite tracker and passes predictor for Android.
 * Copyright (C) 2019, 2020 Arty Bishop (bishop.arty@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rtbishop.look4sat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.Result
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.ActivityMainBinding
import com.rtbishop.look4sat.databinding.DrawerHeaderBinding
import com.rtbishop.look4sat.ui.fragments.TleSourcesDialogFragment
import com.rtbishop.look4sat.utility.GeneralUtils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : AppCompatActivity(), TleSourcesDialogFragment.SourcesSubmitListener {

    private val pickFileReqCode = 111
    private val permLocationCode = 101
    private val permLocation = Manifest.permission.ACCESS_FINE_LOCATION
    private val permGranted = PackageManager.PERMISSION_GRANTED

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var viewModel: SharedViewModel
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var drawerBinding: DrawerHeaderBinding
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        drawerBinding = DrawerHeaderBinding.inflate(layoutInflater, mainBinding.navView, true)
        setContentView(mainBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        (application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(SharedViewModel::class.java)

        setupComponents()
        setupObservers()
        setupDrawer()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupComponents() {
        val navController = findNavController(R.id.nav_host)
        val toolbar = mainBinding.includeAppBar.toolbar
        setSupportActionBar(toolbar)

        appBarConfig = AppBarConfiguration(
            setOf(R.id.nav_pass_list, R.id.nav_map_view),
            mainBinding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfig)
        mainBinding.navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_pass_list -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    toolbar.visibility = View.VISIBLE
                    mainBinding.includeAppBar.toolbarLayout.visibility = View.VISIBLE
                }
                R.id.nav_map_view -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    toolbar.visibility = View.GONE
                }
                else -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    toolbar.visibility = View.VISIBLE
                    mainBinding.includeAppBar.toolbarLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.getGSP().observe(this, Observer { result ->
            when (result) {
                is Result.Success -> {
                    drawerBinding.drawerLatValue.text =
                        String.format(getString(R.string.pat_location), result.data.latitude)
                    drawerBinding.drawerLonValue.text =
                        String.format(getString(R.string.pat_location), result.data.longitude)
                }
                is Result.Error -> {
                    when (result.exception) {
                        is SecurityException ->
                            getString(R.string.err_no_permissions).toast(this)
                        is IllegalArgumentException ->
                            getString(R.string.update_loc_failure).toast(this)
                    }
                }
            }
        })
        viewModel.getUpdateStatus().observe(this, Observer { result ->
            when (result) {
                is Result.Success -> {
                    when (result.data) {
                        0 -> getString(R.string.update_tle_success).toast(this)
                        1 -> getString(R.string.update_trans_success).toast(this)
                    }
                }
                is Result.Error -> getString(R.string.update_failure).toast(this)
            }
        })
    }

    private fun setupDrawer() {
        drawerBinding.drawerBtnLoc.setOnClickListener {
            requestLocationUpdate()
            it.lockButton()
        }
        drawerBinding.drawerBtnTleFile.setOnClickListener {
            val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
            startActivityForResult(openFileIntent, pickFileReqCode)
            mainBinding.drawerLayout.closeDrawers()
            it.lockButton()
        }
        drawerBinding.drawerBtnTleUrl.setOnClickListener {
            showTleSourcesDialog()
            mainBinding.drawerLayout.closeDrawers()
            it.lockButton()
        }
        drawerBinding.drawerBtnTrans.setOnClickListener {
            viewModel.updateTransmitters()
            it.lockButton()
        }
        drawerBinding.drawerBtnGithub.setOnClickListener {
            val gitHubUrl = "https://github.com/rt-bishop/LookingSat"
            val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubUrl))
            startActivity(githubIntent)
        }
        drawerBinding.drawerBtnExit.setOnClickListener { finish() }
    }

    private fun showTleSourcesDialog() {
        val sources = viewModel.getTleSources().map { TleSource(it) }
        TleSourcesDialogFragment(sources).apply {
            setSourcesListener(this@MainActivity)
            show(supportFragmentManager, "TleSourcesDialog")
        }
    }

    override fun onSourcesSubmit(list: List<TleSource>) {
        viewModel.updateEntriesFromWeb(list)
    }

    private fun View.lockButton() {
        lifecycleScope.launch {
            this@lockButton.isEnabled = false
            delay(3000)
            this@lockButton.isEnabled = true
        }
    }

    private fun requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, permLocation) != permGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permLocation)) {
                getString(R.string.err_no_permissions).toast(this)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permLocation), permLocationCode)
            }
        } else {
            viewModel.updatePosition()
        }
    }

    override fun onRequestPermissionsResult(reqCode: Int, perms: Array<out String>, res: IntArray) {
        when (reqCode) {
            permLocationCode -> {
                if (res.isNotEmpty() && res[0] == permGranted) {
                    viewModel.updatePosition()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == pickFileReqCode && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                viewModel.updateEntriesFromFile(uri)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
