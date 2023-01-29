package com.rtbishop.look4sat.presentation.aboutScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.theme.Look4SatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AboutScreen() }
        }
    }

    @Preview(showBackground = true, widthDp = 400, heightDp = 720, showSystemUi = true)
    @Composable
    private fun AboutScreen() {
        Look4SatTheme {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { CardAbout() }
                item { CardOutro() }
            }
        }
    }

    @Composable
    private fun CardAbout(modifier: Modifier = Modifier, version: String = "3.2.0") {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_satellite),
                        tint = MaterialTheme.colors.secondary,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .padding(top = 8.dp, end = 8.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 48.sp,
                            color = MaterialTheme.colors.secondary
                        )
                        Text(
                            text = stringResource(id = R.string.app_version, version),
                            fontSize = 21.sp
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.app_subtitle),
                    fontSize = 21.sp,
                    modifier = modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
        }
    }

    @Composable
    fun CardOutro(modifier: Modifier = Modifier) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.outro_title),
                    fontSize = 19.sp,
                    color = MaterialTheme.colors.secondary,
                    modifier = modifier.padding(8.dp)
                )
                Text(
                    text = stringResource(id = R.string.outro_thanks),
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = modifier.padding(start = 8.dp, end = 8.dp)
                )
                Text(
                    text = stringResource(id = R.string.outro_license),
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = modifier.padding(8.dp)
                )
            }
        }
    }
}
