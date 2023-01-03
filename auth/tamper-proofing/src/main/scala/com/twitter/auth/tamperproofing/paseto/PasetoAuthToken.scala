package com.twitter.auth.tamperproofing.paseto

import java.util.Objects
import net.aholbrook.paseto.service.Token

class PasetoAuthToken(plainTextData: String) extends Token {

  private var data: String = plainTextData
  private var hash: Array[Byte] = null

  def this() {
    this("")
  }

  def getData(): String = data

  def setData(plainTextData: String): PasetoAuthToken = {
    data = plainTextData
    this
  }

  def getHash(): Array[Byte] = hash

  def setHash(hash: Array[Byte]): PasetoAuthToken = {
    this.hash = hash
    this
  }

  override def hashCode: Int = Objects.hash(Array[AnyRef](data, hash))

  override def toString: String = "Token{data='" + data + " hash=" + hash + '}'
}
