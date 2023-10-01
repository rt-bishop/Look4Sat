package com.rtbishop.look4sat.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rtbishop.look4sat.R
import com.rtbishop.look4sat.presentation.MainTheme

@Composable
@Preview(showBackground = true)
private fun TimerBarPreview() {
    MainTheme { TimerBar(45555, "Satellite", "88:88:88", R.drawable.ic_filter) {} }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerBar(id: Int, name: String, time: String, iconId: Int, action: () -> Unit) {
    val barHeightMod = Modifier.height(48.dp)
    val maxSizeMod = Modifier.fillMaxSize()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ElevatedCard(modifier = barHeightMod.weight(1f)) {
            Row(
                modifier = maxSizeMod.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Id:$id - ",
                    modifier = Modifier.width(82.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = name,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
        ElevatedCard(modifier = barHeightMod) {
            Text(
                text = time,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
        CardIcon(onClick = { action() }, iconId = iconId)
    }
}

@Composable
fun CardButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = { onClick() }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ), shape = MaterialTheme.shapes.small, modifier = modifier
    ) { Text(text = text, fontSize = 17.sp) }
}

@Composable
fun CardIcon(onClick: () -> Unit, iconId: Int, description: String? = null) {
    val clickableModifier = Modifier.clickable { onClick() }
    ElevatedCard(modifier = Modifier.size(48.dp)) {
        Box(modifier = clickableModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = ImageVector.vectorResource(iconId), contentDescription = description)
        }
    }
}

@Composable
fun CardLoadingIndicator() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.size(80.dp))
    }
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun gotoUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

//fun Modifier.onClick(onClick: () -> Unit): Modifier = composed {
//    clickable(remember { MutableInteractionSource() }, null) { onClick() }
//}
