package com.rtbishop.look4sat.presentation.aboutScreen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.rtbishop.look4sat.BuildConfig
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
                contentPadding = PaddingValues(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { CardAbout(BuildConfig.VERSION_NAME) }
                item { CardCredits() }
            }
        }
    }

    @Composable
    private fun CardAbout(version: String, modifier: Modifier = Modifier) {
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
                    modifier = modifier.padding(top = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                ) {
                    LinkButton(
                        onClick = { gotoUrl("https://sites.google.com/view/look4sat-privacy-policy/home") },
                        text = stringResource(id = R.string.btn_privacy),
                        modifier = Modifier.weight(1f)
                    )
                    LinkButton(
                        onClick = { gotoUrl("https://www.gnu.org/licenses/gpl-3.0.html") },
                        text = stringResource(id = R.string.btn_license),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun CardCredits(modifier: Modifier = Modifier) {
        Card(modifier = modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.outro_title),
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.secondary,
                    modifier = modifier.padding(8.dp)
                )
                Text(
                    text = stringResource(id = R.string.outro_thanks),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = modifier.padding(start = 8.dp, end = 8.dp)
                )
                Text(
                    text = stringResource(id = R.string.outro_license),
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.secondary,
                    modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                ) {
                    LinkButton(
                        onClick = { gotoUrl("https://github.com/rt-bishop/Look4Sat/") },
                        text = stringResource(id = R.string.btn_github),
                        modifier = Modifier.weight(1f)
                    )
                    LinkButton(
                        onClick = { gotoUrl("https://ko-fi.com/rt_bishop") },
                        text = stringResource(id = R.string.btn_support),
                        modifier = Modifier.weight(1f)
                    )
                    LinkButton(
                        onClick = { gotoUrl("https://f-droid.org/en/packages/com.rtbishop.look4sat/") },
                        text = stringResource(id = R.string.btn_fdroid),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    @Composable
    private fun LinkButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
        Button(
            onClick = { onClick() },
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primaryVariant),
            modifier = modifier.padding(4.dp)
        ) {
            Text(text = text, style = TextStyle(fontWeight = FontWeight.Bold), fontSize = 18.sp)
        }
    }

    private fun gotoUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
