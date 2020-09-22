package com.rtbishop.look4sat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ActivityNavBinding

class NavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNavBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_frag) as NavHostFragment
        binding.navBottom.setupWithNavController(navHost.navController)
    }
}