package com.rtbishop.lookingsat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host)

        appBarConfig =
            AppBarConfiguration(setOf(R.id.nav_sky, R.id.nav_radar, R.id.nav_map), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfig)
        navView.setupWithNavController(navController)
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
