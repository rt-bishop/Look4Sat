package com.rtbishop.lookingsat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.rtbishop.lookingsat.MainViewModel
import com.rtbishop.lookingsat.R

class MainActivity : AppCompatActivity() {

    private val permLocationCode = 101
    private val permLocation = Manifest.permission.ACCESS_FINE_LOCATION
    private val permGranted = PackageManager.PERMISSION_GRANTED
    private val githubUrl = "https://github.com/rt-bishop/LookingSat"

    private lateinit var viewModel: MainViewModel
    private lateinit var timerLayout: ConstraintLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerLat: TextView
    private lateinit var drawerLon: TextView
    private lateinit var drawerBtnLoc: ImageButton
    private lateinit var drawerBtnTle: ImageButton
    private lateinit var drawerBtnTrans: ImageButton
    private lateinit var drawerBtnGithub: ImageButton
    private lateinit var drawerBtnExit: ImageButton
    private lateinit var appBarConfig: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupComponents()
        setupDrawer()
    }

    private fun setupComponents() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val header = navView.getHeaderView(0)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        timerLayout = findViewById(R.id.toolbar_layout_timer)
        drawerLayout = findViewById(R.id.drawer_layout)

        drawerLat = header.findViewById(R.id.drawer_lat_value)
        drawerLon = header.findViewById(R.id.drawer_lon_value)

        drawerBtnLoc = header.findViewById(R.id.drawer_btn_loc)
        drawerBtnTle = header.findViewById(R.id.drawer_btn_tle)
        drawerBtnTrans = header.findViewById(R.id.drawer_btn_trans)
        drawerBtnGithub = header.findViewById(R.id.drawer_btn_github)
        drawerBtnExit = header.findViewById(R.id.drawer_btn_exit)

        appBarConfig = AppBarConfiguration(
            setOf(R.id.nav_sky, R.id.nav_map, R.id.nav_credits, R.id.nav_about),
            drawerLayout
        )

        val navController = findNavController(R.id.nav_host)
        setupActionBarWithNavController(navController, appBarConfig)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_sky -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    toolbar.visibility = View.VISIBLE
                    timerLayout.visibility = View.VISIBLE
                }
                R.id.nav_map -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    toolbar.visibility = View.GONE
                }
                else -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    toolbar.visibility = View.VISIBLE
                    timerLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun setupDrawer() {
        viewModel.debugMessage.observe(this, Observer { debug_message ->
            if (debug_message != "") {
                Toast.makeText(this, debug_message, Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.gsp.observe(this, Observer { gsp ->
            drawerLat.text = String.format("%.4f", gsp.latitude)
            drawerLon.text = String.format("%.4f", gsp.longitude)
        })

        drawerBtnLoc.setOnClickListener { updateLocation() }
        drawerBtnTle.setOnClickListener { viewModel.updateTwoLineElementFile() }
        drawerBtnTrans.setOnClickListener { viewModel.updateTransmittersDatabase() }
        drawerBtnGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
        }
        drawerBtnExit.setOnClickListener { finish() }
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(this, permLocation) != permGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permLocation)) {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permLocation), permLocationCode)
            }
        } else {
            viewModel.updateLocation()
        }
    }

    override fun onRequestPermissionsResult(reqCode: Int, perms: Array<out String>, res: IntArray) {
        when (reqCode) {
            permLocationCode -> {
                if (res.isNotEmpty() && res[0] == permGranted) {
                    viewModel.updateLocation()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host)
        return navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
    }
}