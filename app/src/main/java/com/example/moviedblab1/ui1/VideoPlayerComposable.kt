package com.example.moviedblab1.ui1

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/* VideoPlayerComposable embeds ExoPlayer in Compose using AndroidView and releases the player correctly with DisposableEffect*/

@Composable
fun VideoPlayerComposable() {
    val context = LocalContext.current

    /* creates the player object */
    val exoPlayer = ExoPlayer.Builder(context).build().apply {
        val mediaItem = MediaItem.fromUri(
            "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
        )
        setMediaItem(mediaItem)
        prepare()
        playWhenReady = false
    }

    /* when the composable leaves the screen, the player is released */
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            }
        }
    )
}