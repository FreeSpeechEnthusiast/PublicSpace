package com.twitter.auth.tamperproofing.paseto

import scala.util.Try

trait PasetoService {
  def encodeToken(data: String, inputKeyIdOpt: Option[Int]): Try[String]
  def decodeToken(token: String): Try[String]
}
