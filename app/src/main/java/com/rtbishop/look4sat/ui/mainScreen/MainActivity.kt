package com.rtbishop.look4sat.ui.mainScreen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
            val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
            navBottom.setupWithNavController(navHost.navController)
        }
    }
}