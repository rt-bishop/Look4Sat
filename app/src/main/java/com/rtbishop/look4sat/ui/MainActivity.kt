package com.rtbishop.look4sat.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.databinding.ActivityMainBinding
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var factory: ViewModelFactory

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_frag) as NavHostFragment
        binding.navBottom.setupWithNavController(navHost.navController)
        (application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(this, factory).get(SharedViewModel::class.java)
        viewModel.setDefaultTleSources()
    }
}