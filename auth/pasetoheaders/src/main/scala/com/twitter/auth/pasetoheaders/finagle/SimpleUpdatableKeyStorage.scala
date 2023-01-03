package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{InMemoryKeyStorage, KeyIdentifier, VersionedKey}
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import java.security.Key

/**
 * Class provides classic implementation of updatable storage
 */
case class SimpleUpdatableKeyStorage[T <: Key]() extends UpdatableKeyStorage[T] {

  var storage: InMemoryKeyStorage[T] = new InMemoryKeyStorage[T]()

  def replaceKeysWithNewVersion(keys: Set[(KeyIdentifier, T)]): Unit = {
    val newStorage = new InMemoryKeyStorage[T]
    keys foreach {
      case (keyIdentifier, key) =>
        newStorage.addKey(keyIdentifier, key)
    }
    storage = newStorage
  }

  def getLastKey(environment: String, issuer: String): Option[VersionedKey[T]] = {
    storage.getLastKey(environment, issuer)
  }

  def getKey(keyIdentifier: KeyIdentifier): Option[T] = {
    storage.getKey(keyIdentifier)
  }
}
