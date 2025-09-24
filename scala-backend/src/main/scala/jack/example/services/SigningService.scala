package jack.example.services

import jack.example.config.SignCfg
import jack.example.util.Crypto

import java.nio.charset.StandardCharsets

class SigningService(cfg: SignCfg) {
  /** NGINX secure_link style: base64url( md5( secret + path + exp ) ) */
  def signPath(path: String, nowEpoch: Long): (String, Long) = {
    val exp = nowEpoch + cfg.ttlSeconds
    val bytes = cfg.secret.getBytes(StandardCharsets.UTF_8) ++ path.getBytes(StandardCharsets.UTF_8) ++ exp.toString.getBytes(StandardCharsets.UTF_8)
    val md5 = Crypto.md5Bytes(bytes)
    val sig = Crypto.b64UrlNoPad(md5)
    (sig, exp)
  }
}
