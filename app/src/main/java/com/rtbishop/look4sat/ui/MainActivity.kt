package com.rtbishop.look4sat.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

//    private val pickFileReqCode = 1001
//    private val permissionsReqCode = 1000
//    private val permissions = arrayOf(
//        Manifest.permission.ACCESS_FINE_LOCATION,
//        Manifest.permission.READ_EXTERNAL_STORAGE,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_frag) as NavHostFragment
        binding.navBottom.setupWithNavController(navHost.navController)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        viewModel.setDefaultTleSources()

//        if (!hasPermissions(this, permissions)) {
//            ActivityCompat.requestPermissions(this, permissions, permissionsReqCode)
//        }
    }

//    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
//        permissions.all {
//            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//
//    private fun openFile() {
//        val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "*/*"
//        }
//        startActivityForResult(openFileIntent, pickFileReqCode)
//    }

//    private fun openSourcesDialog() {
//        SourcesDialog(viewModel.getTleSources()).apply {
//            setSourcesListener(this@MainActivity)
//            show(supportFragmentManager, "TleSourcesDialog")
//        }
//    }

//    override fun onSourcesSubmit(list: List<TleSource>) {
//        viewModel.updateSatData(list)
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == pickFileReqCode && resultCode == RESULT_OK) {
//            data?.data?.also { uri ->
//                viewModel.updateEntriesFromFile(uri)
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
//    }

//    private fun gotoGitHub() {
//        val gitHubUrl = "https://github.com/rt-bishop/LookingSat"
//        val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubUrl))
//        startActivity(githubIntent)
//    }
//
//    private fun View.lockButton() {
//        lifecycleScope.launch {
//            this@lockButton.isEnabled = false
//            delay(3000)
//            this@lockButton.isEnabled = true
//        }
//    }
}