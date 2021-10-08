package com.videoeditor.compressor.config

import com.videoeditor.compressor.VideoQuality

data class Configuration(
    var quality: VideoQuality = VideoQuality.MEDIUM,
    var frameRate: Int? = null,
    var isMinBitrateCheckEnabled: Boolean = true,
    var videoBitrate: Int? = null,
)
