package com.rtbishop.lookingsat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.rtbishop.lookingsat.R
import com.rtbishop.lookingsat.vm.MainViewModel
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val permLocationCode = 101
    private val permLocation = Manifest.permission.ACCESS_FINE_LOCATION
    private val permGranted = PackageManager.PERMISSION_GRANTED
    private val githubUrl = "https://github.com/rt-bishop/LookingSat"

    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var viewModel: MainViewModel
    private lateinit var timerLayout: ConstraintLayout
    private lateinit var timeToAos: TextView
    private lateinit var drawerLat: TextView
    private lateinit var drawerLon: TextView
    private lateinit var drawerHeight: TextView
    private lateinit var drawerSubTitle: TextView
    private lateinit var drawerGetLocation: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupComponents()
        setupDrawer()
        setupTimer()
        updateLocation()
    }

    private fun setupComponents() {
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        timerLayout = findViewById(R.id.timer_layout)
        timeToAos = findViewById(R.id.time_to_aos)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val header = navView.getHeaderView(0)

        drawerLat = header.findViewById(R.id.drawer_lat_value)
        drawerLon = header.findViewById(R.id.drawer_lon_value)
        drawerHeight = header.findViewById(R.id.drawer_height_value)
        drawerSubTitle = header.findViewById(R.id.drawer_link)
        drawerGetLocation = header.findViewById(R.id.drawer_get_location)

        val navController = findNavController(R.id.nav_host)
        appBarConfig = AppBarConfiguration(setOf(R.id.nav_sky, R.id.nav_single_sat), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfig)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when {
                destination.id == R.id.nav_sky -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    toolbar.visibility = View.VISIBLE
                    timerLayout.visibility = View.VISIBLE
                }
                destination.id == R.id.nav_map -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    toolbar.visibility = View.GONE
                }
                else -> {
                    timerLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun setupDrawer() {
        drawerSubTitle.setOnClickListener { goToGithub() }

        drawerGetLocation.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        drawerGetLocation.setOnClickListener {
            it.isEnabled = false
            updateLocation()
            it.postDelayed({ it.isEnabled = true }, 3600)
        }

        viewModel.gsp.observe(this, androidx.lifecycle.Observer { gsp ->
            drawerLat.text = String.format("%.4f", gsp.latitude)
            drawerLon.text = String.format("%.4f", gsp.longitude)
            drawerHeight.text = String.format("%.1fm", gsp.heightAMSL)
        })
    }

    private fun setupTimer() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val totalMillis = cal.timeInMillis - System.currentTimeMillis()

        val timer = object : CountDownTimer(totalMillis, 1000) {
            override fun onFinish() {
                Toast.makeText(this@MainActivity, "Time is up!", Toast.LENGTH_SHORT).show()
            }

            override fun onTick(millisUntilFinished: Long) {
                timeToAos.text = String.format(
                    resources.getString(R.string.next_aos_pattern),
                    TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                )
            }
        }
        timer.start()
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

    private fun goToGithub() {
        val intentGitHub = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
        startActivity(intentGitHub)
    }

    private fun exit() {
        finish()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> Toast.makeText(this, "Refresh", Toast.LENGTH_SHORT).show()
        }
        return super.onOptionsItemSelected(item)
    }
}