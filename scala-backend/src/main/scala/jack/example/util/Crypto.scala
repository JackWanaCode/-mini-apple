package jack.example.util

import java.security.MessageDigest
import java.util.Base64

object Crypto {
  def md5Bytes(in: Array[Byte]): Array[Byte] = {
    val md = MessageDigest.getInstance("MD5")
    md.digest(in)
  }

  def b64UrlNoPad(bytes: Array[Byte]): String =
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)
}
