package vn.edu.ut.its.mobility.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A5035),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5E4D6),
    onPrimaryContainer = Color(0xFF653625),
    secondary = Color(0xFF855834),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E7D9),
    onSecondaryContainer = Color(0xFF664121),
    background = Color(0xFFF9F6EF),
    onBackground = Color(0xFF28282C),
    surface = Color(0xFFFFFDFA),
    onSurface = Color(0xFF28282C),
    surfaceVariant = Color(0xFFEAE7DF),
    onSurfaceVariant = Color(0xFF5D5A55),
    surfaceContainer = Color(0xFFF4EEE3),
    surfaceContainerHigh = Color(0xFFF1EBDD),
    outline = Color(0xFF777B7C),
    outlineVariant = Color(0xFFE6DED1),
    error = Color(0xFFAD343C),
    errorContainer = Color(0xFFF8DFE0),
    onErrorContainer = Color(0xFF64252A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0B493),
    onPrimary = Color(0xFF39251D),
    primaryContainer = Color(0xFF5A3628),
    onPrimaryContainer = Color(0xFFF5DFD2),
    secondary = Color(0xFFE9B88F),
    onSecondary = Color(0xFF52331D),
    secondaryContainer = Color(0xFF543E32),
    onSecondaryContainer = Color(0xFFF4DDC8),
    background = Color(0xFF161D25),
    onBackground = Color(0xFFF3F1EB),
    surface = Color(0xFF202B35),
    onSurface = Color(0xFFF3F1EB),
    surfaceVariant = Color(0xFF34414A),
    onSurfaceVariant = Color(0xFFB7C0C6),
    surfaceContainer = Color(0xFF293742),
    surfaceContainerHigh = Color(0xFF34424E),
    outline = Color(0xFFAFBBC4),
    outlineVariant = Color(0xFF445360),
    error = Color(0xFFFFA8AC),
    errorContainer = Color(0xFF51262C),
    onErrorContainer = Color(0xFFFFD9DA),
)

private val ItsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

internal val ItsEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@Composable
internal fun ItsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = ItsShapes,
        content = content,
    )
}

@Composable
internal fun ItsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.97f else 1f,
        spring(dampingRatio = 0.86f, stiffness = 700f),
        label = "button press",
    )
    val feedback = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    if (outlined) {
        OutlinedButton(onClick, feedback, enabled, shape = MaterialTheme.shapes.medium, contentPadding = contentPadding, interactionSource = source, content = content)
    } else {
        Button(onClick, feedback, enabled, shape = MaterialTheme.shapes.medium, contentPadding = contentPadding, interactionSource = source, content = content)
    }
}

@Composable
internal fun BrandTile(modifier: Modifier = Modifier, size: Dp = 42.dp) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.DirectionsBike, contentDescription = null, modifier = Modifier.size(size * 0.60f))
        }
    }
}


@Composable
internal fun ItsNotice(message: String?, isError: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(180)) + slideInVertically(tween(240, easing = ItsEase)) { it / 4 },
        exit = fadeOut(tween(100)),
    ) {
        val colors = MaterialTheme.colorScheme
        Surface(
            color = if (isError) colors.errorContainer else colors.primaryContainer,
            contentColor = if (isError) colors.onErrorContainer else colors.onPrimaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isError) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle, null, Modifier.size(17.dp))
                Text(message.orEmpty(), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun ItsPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 3.dp,
    ) { Box { content() } }
}
