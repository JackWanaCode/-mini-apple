package jack.example.origin

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64


class TokenAuth(secret: Option[Array[Byte]]) {
  private val algo = "HmacSHA256"
  val isEnabled: Boolean = secret.isDefined


  /**
   * Token format: base64url("expEpochSeconds:signature") where signature = HMAC_SHA256(exp + ":" + exp)
   * In practice you should sign canonical request info (path, method). This is a minimal demo.
   */
  def validate(token: String): Boolean = secret match {
    case None => true
    case Some(sec) =>
      try {
        val raw = new String(Base64.getUrlDecoder.decode(token), "UTF-8")
        val parts = raw.split(":", 2)
        if (parts.length != 2) return false
        val exp = parts(0)
        val sig = parts(1)
        if (exp.toLongOption.forall(_.longValue() < (System.currentTimeMillis()/1000L))) return false
        val mac = Mac.getInstance(algo)
        mac.init(new SecretKeySpec(sec, algo))
        val expected = Base64.getUrlEncoder.withoutPadding().encodeToString(mac.doFinal((exp + ":" + exp).getBytes("UTF-8")))
        expected == token
      } catch {
        case _: Throwable => false
      }
  }
}
