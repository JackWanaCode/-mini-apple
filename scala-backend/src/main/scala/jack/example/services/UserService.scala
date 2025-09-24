package jack.example.services

import jack.example.models.User

import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent.TrieMap
import org.mindrot.jbcrypt.BCrypt

class UserService {

  private val seq = new AtomicLong(1)
  private val users = new TrieMap[String, User]() // keyed by email


  def register(email: String, password: String): Either[String, User] =
    if (users.contains(email)) Left("Email already registered")
    else {
      val hash = BCrypt.hashpw(password, BCrypt.gensalt())
      val u = User(seq.getAndIncrement(), email, hash)
      users.put(email, u); Right(u)
    }


  def login(email: String, password: String): Either[String, User] =
    users.get(email) match {
      case None => Left("Invalid email or password")
      case Some(u) => if (BCrypt.checkpw(password, u.passwordHash)) Right(u) else Left("Invalid email or password")
    }

}
