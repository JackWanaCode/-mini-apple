package jack.example.config

import com.typesafe.config.ConfigFactory
final case class JwtCfg(secret: String, issuer: String, ttlSeconds: Int)
final case class SignCfg(secret: String, ttlSeconds: Int)
final case class MediaCfg(originBaseUrl: String, publicHlsBase: String)
final case class AppCfg(host: String, port: Int, media: MediaCfg, jwt: JwtCfg, sign: SignCfg, corsAllowed: List[String])

object Settings {
  def load(): AppCfg = {
    val root = ConfigFactory.load().getConfig("app")
    val media = MediaCfg(
      originBaseUrl = root.getString("media.origin-base-url").stripSuffix("/"),
      publicHlsBase = root.getString("media.public-hls-base").stripSuffix("/")
    )
    AppCfg(
      host = root.getString("host"),
      port = root.getInt("port"),
      media = media,
      jwt = JwtCfg(root.getString("jwt.secret"), root.getString("jwt.issuer"), root.getInt("jwt.ttl-seconds")),
      sign = SignCfg(root.getString("signing.secret"), root.getInt("signing.ttl-seconds")),
      corsAllowed = root.getStringList("cors.allowed-origins").toArray(new Array[String](0)).toList
    )
  }
}
