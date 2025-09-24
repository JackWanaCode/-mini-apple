package jack.example.models

case class Encoding(
                     videoId: String,
                     rendition: String, // "1080p" | "720p" | "480p"
                     bandwidth: Int, // average bitrate
                     codecs: String, // e.g. "avc1.640028,mp4a.40.2"
                     path: String, // "/hls/{videoId}/{rendition}/index.m3u8"
                     ready: Boolean
                   )
