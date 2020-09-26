package com.rtbishop.look4sat.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import javax.inject.Inject

class EntriesFragment : Fragment(R.layout.fragment_entries) {

    @Inject
    lateinit var modelFactory: ViewModelFactory

    private lateinit var binding: FragmentEntriesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
    }

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
//        if (requestCode == pickFileReqCode && resultCode == AppCompatActivity.RESULT_OK) {
//            data?.data?.also { uri ->
//                viewModel.updateEntriesFromFile(uri)
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
//    }

//    private fun openFile() {
//        val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "*/*"
//        }
//        startActivityForResult(openFileIntent, pickFileReqCode)
//    }
}