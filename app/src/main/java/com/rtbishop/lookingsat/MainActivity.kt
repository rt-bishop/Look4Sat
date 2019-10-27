package com.rtbishop.lookingsat

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import java.util.*
import java.util.concurrent.TimeUnit

const val LOCATION_REQ = 101
const val LOCATION_PERM = Manifest.permission.ACCESS_FINE_LOCATION
const val GRANTED = PackageManager.PERMISSION_GRANTED
const val GITHUB_URL = "https://github.com/rt-bishop/LookingSat"

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var viewModel: MainViewModel
    private lateinit var timerLayout: ConstraintLayout
    private lateinit var timeToAos: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var preferences: SharedPreferences
    private lateinit var drawerLat: TextView
    private lateinit var drawerLon: TextView
    private lateinit var drawerHeight: TextView
    private lateinit var drawerSubTitle: TextView
    private lateinit var drawerGetLocation: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupComponents()
        setupNavigation()
        setupTimer()
        updateDrawerValues()
    }

    private fun setupComponents() {
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        timerLayout = findViewById(R.id.timer_layout)
        timeToAos = findViewById(R.id.time_to_aos)
    }

    private fun setupNavigation() {
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

        drawerSubTitle.setOnClickListener {
            openGitHub()
        }

        drawerGetLocation.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse))
        drawerGetLocation.setOnClickListener {
            it.isEnabled = false
            updateLocation()
            it.postDelayed({ it.isEnabled = true }, 3600)
        }
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
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERM) != GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, LOCATION_PERM)) {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(LOCATION_PERM), LOCATION_REQ)
            }
        } else {
            getPreciseLocation()
        }
    }

    private fun getPreciseLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val lat = location?.latitude ?: 51.5074
            val lon = location?.longitude ?: 0.1278
            val height = location?.altitude ?: 48.0

            preferences.edit {
                putDouble("LATITUDE", lat)
                putDouble("LONGITUDE", lon)
                putDouble("HEIGHT", height)
                apply()
            }
            updateDrawerValues()
            Toast.makeText(this, "Location was set", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDrawerValues() {
        val lat = preferences.getDouble("LATITUDE", 51.5074)
        val lon = preferences.getDouble("LONGITUDE", 0.1278)
        val height = preferences.getDouble("HEIGHT", 48.0)

        drawerLat.text = String.format("%.4f", lat)
        drawerLon.text = String.format("%.4f", lon)
        drawerHeight.text = String.format("%.1fm", height)
    }

    private fun openGitHub() {
        val intentGitHub = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
        startActivity(intentGitHub)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQ -> {
                if (grantResults.isNotEmpty() && grantResults[0] == GRANTED) {
                    getPreciseLocation()
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
            R.id.fetch_trans -> Toast.makeText(this, "Fetch transmit", Toast.LENGTH_SHORT).show()
            R.id.save_to_db -> Toast.makeText(this, "Saving to DB", Toast.LENGTH_SHORT).show()
            R.id.load_from_db -> Toast.makeText(this, "Loading from DB", Toast.LENGTH_SHORT).show()
            R.id.fetch_tle -> Toast.makeText(this, "Fetching TLE", Toast.LENGTH_SHORT).show()
            R.id.load_tle -> Toast.makeText(this, "Loading TLE", Toast.LENGTH_SHORT).show()
            R.id.action_exit -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}

fun SharedPreferences.Editor.putDouble(key: String, double: Double): SharedPreferences.Editor =
    putLong(key, java.lang.Double.doubleToRawLongBits(double))

fun SharedPreferences.getDouble(key: String, default: Double) =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))