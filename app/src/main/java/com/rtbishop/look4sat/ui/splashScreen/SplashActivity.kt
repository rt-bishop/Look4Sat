package com.rtbishop.look4sat.ui.splashScreen

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ActivitySplashBinding
import com.rtbishop.look4sat.ui.mainScreen.MainActivity
import com.rtbishop.look4sat.utility.PrefsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : FragmentActivity() {

    private val pagesCount = 2

    @Inject
    lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefsManager.isFirstLaunch()) showOnboarding()
        else startMainActivity()
    }

    private fun showOnboarding() {
        setTheme(R.style.AppTheme)
        ActivitySplashBinding.inflate(layoutInflater).apply {
            setContentView(root)
            splashPager.apply {
                adapter = SplashPagerAdapter(this@SplashActivity)
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        if (position == pagesCount - 1) {
                            splashNext.setImageResource(R.drawable.ic_check)
                        }
                    }
                })
                isUserInputEnabled = false
            }
            splashNext.setOnClickListener {
                if (splashPager.currentItem < pagesCount - 1) splashPager.currentItem++
                else if (splashPager.currentItem == pagesCount - 1) startMainActivity()
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private inner class SplashPagerAdapter(activity: FragmentActivity) :
        FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = pagesCount

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> TleFragment()
                1 -> PosFragment()
                else -> TleFragment()
            }
        }
    }
}