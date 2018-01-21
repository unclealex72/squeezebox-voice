package auth

import java.security.SecureRandom

import scala.util.Random

/**
  * Created by alex on 08/01/18
  **/
class RandomStringGeneratorImpl(length: Int) extends RandomStringGenerator {

  val random: Random = new Random(new SecureRandom())

  override def generate: String = random.alphanumeric.take(length).mkString
}
