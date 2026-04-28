//Functional youtube player, AMDG

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView


// --- HELPER FUNCTION: EXTRACT YOUTUBE ID ---
fun extractYoutubeVideoId(url: String): String {
    val trimmedUrl = url.trim()
    if (trimmedUrl.length == 11 && !trimmedUrl.contains("://")) return trimmedUrl
    val regex = Regex("(?:v=|v\\/|vi=|vi\\/|youtu\\.be\\/|embed\\/|shorts\\/|live\\/|\\/v\\/|\\/e\\/|watch\\?v=|&v=)([a-zA-Z0-9_-]{11})")
    val match = regex.find(trimmedUrl)
    if (match != null && match.groups.size > 1) return match.groupValues[1]
    val fallbackRegex = Regex("(?:\\/|=)([a-zA-Z0-9_-]{11})(?:[?&]|$)")
    val fallbackMatch = fallbackRegex.find(trimmedUrl)
    if (fallbackMatch != null && fallbackMatch.groups.size > 1) return fallbackMatch.groupValues[1]
    if (trimmedUrl.contains("youtu", ignoreCase = true)) {
        val ultimateFallback = Regex("[a-zA-Z0-9_-]{11}").find(trimmedUrl)
        if (ultimateFallback != null) return ultimateFallback.value
    }
    return trimmedUrl
}



@Composable
fun YoutubePlayer(
    youtubeVideoUrl: String,
    lifecycleOwner: LifecycleOwner
) {
    // Automatically parse the ID from the URL using our new helper function
    val videoId = remember(youtubeVideoUrl) { extractYoutubeVideoId(youtubeVideoUrl) }

    // Only render the player if we have a valid extracted ID
    if (videoId.isNotBlank()) {
        // FIX: Forces the view to cleanly rebuild if the admin changes the video ID mid-stream
        key(videoId) {
            val viewToRelease = remember { mutableStateOf<YouTubePlayerView?>(null) }

            // FIX: MUST remove observer & release player when leaving Live page.
            // Failing to do this causes a Lifecycle leak and crashes the app when switching tabs.
            DisposableEffect(lifecycleOwner) {
                onDispose {
                    viewToRelease.value?.let { view ->
                        lifecycleOwner.lifecycle.removeObserver(view)
                        view.release()
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp)),
                factory = { context ->
                    YouTubePlayerView(context).apply {
                        viewToRelease.value = this
                        lifecycleOwner.lifecycle.addObserver(this)

                        addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                // Load using the extracted 11 character ID, not the full URL string.
                                youTubePlayer.loadVideo(videoId, 0f)
                            }
                        })
                    }
                }
            )
        }
    }
}

