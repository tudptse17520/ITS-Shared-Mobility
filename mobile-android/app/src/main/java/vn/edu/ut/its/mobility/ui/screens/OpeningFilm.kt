package vn.edu.ut.its.mobility.ui.screens

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.io.IOException
import kotlinx.coroutines.delay
import vn.edu.ut.its.mobility.R

private const val PlaybackLimitMillis = 13_000L
private val FilmCream = Color(0xFFFBF5ED)
private val FilmInk = Color(0xFF30251D)

@Composable
fun OpeningFilm(onFinish: () -> Unit) {
    val finish by rememberUpdatedState(onFinish)
    BackHandler { finish() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { finish() }
    LaunchedEffect(Unit) {
        delay(PlaybackLimitMillis)
        finish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(FilmCream)
            .semantics { paneTitle = "Giới thiệu ITS Mobility" },
    ) {
        AndroidView(
            factory = { context -> OpeningFilmView(context) { finish() } },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.release() },
        )
        TextButton(
            onClick = { finish() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 8.dp, end = 14.dp)
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = FilmInk),
            contentPadding = PaddingValues(0.dp),
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 30.dp)
                    .background(FilmCream.copy(alpha = .87f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = .55f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Bỏ qua", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private class OpeningFilmView(
    context: Context,
    private val onFinish: () -> Unit,
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var outputSurface: Surface? = null

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        release()
        val clip = if (width * 5 <= height * 4) R.raw.its_opening_portrait else R.raw.its_opening_landscape
        val surface = Surface(texture).also { outputSurface = it }
        val playback = MediaPlayer().also { player = it }
        try {
            val descriptor = resources.openRawResourceFd(clip) ?: throw IOException("Opening film unavailable")
            descriptor.use { playback.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            playback.setSurface(surface)
            playback.setVolume(0f, 0f)
            playback.setOnPreparedListener {
                fitVideo(it.videoWidth, it.videoHeight)
                it.start()
            }
            playback.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                fitVideo(videoWidth, videoHeight)
            }
            playback.setOnCompletionListener { onFinish() }
            playback.setOnErrorListener { _, _, _ ->
                onFinish()
                true
            }
            playback.prepareAsync()
        } catch (_: IOException) {
            release()
            onFinish()
        } catch (_: IllegalStateException) {
            release()
            onFinish()
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        player?.let { fitVideo(it.videoWidth, it.videoHeight) }
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    private fun fitVideo(videoWidth: Int, videoHeight: Int) {
        if (width == 0 || height == 0 || videoWidth == 0 || videoHeight == 0) return
        val scale = maxOf(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
        setTransform(Matrix().apply {
            setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f)
        })
    }

    fun release() {
        player?.release()
        player = null
        outputSurface?.release()
        outputSurface = null
    }
}
