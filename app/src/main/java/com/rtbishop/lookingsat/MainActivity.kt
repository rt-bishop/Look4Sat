package com.rtbishop.lookingsat

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.view.View
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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var viewModel: MainViewModel
    private lateinit var timerLayout: ConstraintLayout
    private lateinit var timeToAos: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var preferences: SharedPreferences
    private lateinit var drawerLat: TextView
    private lateinit var drawerLon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupComponents()
        setupNavigation()
        setupTimer()
        updateDrawerText()
    }

    private fun updateDrawerText() {
        drawerLat.text = preferences.getString("LATITUDE", "-180.0000")?.substring(0, 9)
        drawerLon.text = preferences.getString("LONGITUDE", "-60.0000")?.substring(0, 9)
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
        val navController = findNavController(R.id.nav_host)

        appBarConfig = AppBarConfiguration(setOf(R.id.nav_sky), drawerLayout)
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
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val currentLat = location?.latitude
            val currentLon = location?.longitude
            val currentHeight = location?.altitude

            preferences.edit {
                putString("LATITUDE", currentLat.toString())
                putString("LONGITUDE", currentLon.toString())
                putString("HEIGHT", currentHeight.toString())
                apply()
            }
            updateDrawerText()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERM) != GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, LOCATION_PERM)) {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(LOCATION_PERM), LOCATION_REQ)
            }
        } else {
            updateLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_REQ -> {
                if (grantResults.isNotEmpty() && grantResults[0] == GRANTED) {
                    updateLocation()
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
            R.id.update_location -> checkPermissions()
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
