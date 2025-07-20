package com.rtbishop.look4sat.presentation.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.MainTheme
import com.rtbishop.look4sat.presentation.Screen
import com.rtbishop.look4sat.presentation.components.CardButton

private const val POLICY_URL = "https://sites.google.com/view/look4sat-privacy-policy/home"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

fun NavGraphBuilder.infoDestination() {
    composable(Screen.Info.route) { InfoScreen() }
}

@Composable
private fun InfoScreen() {
    LazyColumn(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { CardCredits() }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardCreditsPreview() = MainTheme { CardCredits() }

@Composable
private fun CardCredits(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.outro_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(6.dp)
            )
            Text(
                text = stringResource(
                    id = R.string.outro_thanks
                ), fontSize = 16.sp, textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.outro_license),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier.padding(6.dp)
            )
            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                CardButton(
                    onClick = { uriHandler.openUri(LICENSE_URL) },
                    text = stringResource(id = R.string.btn_license),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CardButton(
                    onClick = { uriHandler.openUri(POLICY_URL) },
                    text = stringResource(id = R.string.btn_privacy),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
