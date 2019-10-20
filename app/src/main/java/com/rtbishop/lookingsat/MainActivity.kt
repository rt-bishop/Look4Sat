package com.rtbishop.lookingsat

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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var mainViewModel: MainViewModel
    private lateinit var timerLayout: ConstraintLayout
    private lateinit var timeToAos: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupNavigation()
        setupTimer()
    }

    private fun setupViews() {
        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        timerLayout = findViewById(R.id.timer_layout)
        timeToAos = findViewById(R.id.time_to_aos)
    }

    private fun setupNavigation() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host)

        appBarConfig = AppBarConfiguration(setOf(R.id.nav_sky), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfig)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_sky) {
                timerLayout.visibility = View.VISIBLE
            } else {
                timerLayout.visibility = View.GONE
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
            R.id.fetch_trans -> Toast.makeText(
                this,
                "Fetching transmitters",
                Toast.LENGTH_SHORT
            ).show()
            R.id.save_to_db -> Toast.makeText(this, "Saving to DB", Toast.LENGTH_SHORT).show()
            R.id.load_from_db -> Toast.makeText(this, "Loading from DB", Toast.LENGTH_SHORT).show()
            R.id.fetch_tle -> Toast.makeText(this, "Fetching TLE", Toast.LENGTH_SHORT).show()
            R.id.load_tle -> Toast.makeText(this, "Loading TLE", Toast.LENGTH_SHORT).show()
            R.id.action_exit -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
