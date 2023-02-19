package com.rtbishop.look4sat.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun CardButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    ElevatedButton(
        onClick = { onClick() }, colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ), shape = MaterialTheme.shapes.small, modifier = modifier.padding(start = 4.dp, end = 4.dp)
    ) { Text(text = text, fontSize = 17.sp) }
}

@Composable
fun RadarPing() {
    val pingColor = MaterialTheme.colorScheme.primary
    val pingDurationMs = 1000
    val scale = remember { mutableStateOf(0f) }
    val scaleAnimation = animateFloatAsState(
        targetValue = scale.value,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = pingDurationMs))
    )
    LaunchedEffect(Unit) { scale.value = 1f }
    Box(
        modifier = Modifier
            .size(size = 48.dp)
            .scale(scale = scaleAnimation.value)
            .border(
                width = 4.dp,
                shape = CircleShape,
                color = pingColor.copy(alpha = 1 - scaleAnimation.value)
            )
    )
}

fun gotoUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

fun Modifier.onClick(onClick: () -> Unit): Modifier = composed {
    clickable(remember { MutableInteractionSource() }, null) { onClick() }
}

class NoRippleInteractionSource : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = true
}
