package jack.example.services

import jack.example.config.JwtCfg
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import java.time.Instant

class JwtService(cfg: JwtCfg) {
  def issue(userId: Long, email: String): String = {
    val now = Instant.now.getEpochSecond
    val claim = JwtClaim(
      issuer = Some(cfg.issuer),
      subject = Some(userId.toString),
      issuedAt = Some(now),
      expiration = Some(now + cfg.ttlSeconds)
    ).+("email", email)
    Jwt.encode(claim, cfg.secret, JwtAlgorithm.HS256)
  }
  def verify(token: String) = Jwt.decode(token, cfg.secret, Seq(JwtAlgorithm.HS256)).toOption
}
