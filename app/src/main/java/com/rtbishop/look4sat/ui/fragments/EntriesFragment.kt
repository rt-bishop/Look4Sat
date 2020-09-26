package com.rtbishop.look4sat.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.rtbishop.look4sat.Look4SatApp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.dagger.ViewModelFactory
import com.rtbishop.look4sat.data.TleSource
import com.rtbishop.look4sat.databinding.FragmentEntriesBinding
import com.rtbishop.look4sat.ui.SharedViewModel
import javax.inject.Inject

class EntriesFragment : Fragment(R.layout.fragment_entries), SourcesDialog.SourcesSubmitListener {

    @Inject
    lateinit var factory: ViewModelFactory

    private lateinit var binding: FragmentEntriesBinding
    private lateinit var viewModel: SharedViewModel
    private val pickFileReqCode = 100

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEntriesBinding.bind(view)
        (requireActivity().application as Look4SatApp).appComponent.inject(this)
        viewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        binding.importWeb.setOnClickListener { showSourcesDialog() }
        binding.importFile.setOnClickListener { showFileDialog() }
    }

    private fun showSourcesDialog() {
        val fragmentManager = childFragmentManager
        SourcesDialog(viewModel.getTleSources()).apply {
            setSourcesListener(this@EntriesFragment)
            show(fragmentManager, "TleSourcesDialog")
        }
    }

    private fun showFileDialog() {
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            startActivityForResult(this, pickFileReqCode)
        }
    }

    override fun onSourcesSubmit(list: List<TleSource>) {
        viewModel.updateSatData(list)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == pickFileReqCode && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.also { uri -> viewModel.updateEntriesFromFile(uri) }
        } else super.onActivityResult(requestCode, resultCode, data)
    }
}