package com.rtbishop.look4sat.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            Icon(painter = painterResource(id = iconId), contentDescription = description)
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

fun Modifier.onClick(onClick: () -> Unit): Modifier = composed {
    clickable(remember { MutableInteractionSource() }, null) { onClick() }
}
