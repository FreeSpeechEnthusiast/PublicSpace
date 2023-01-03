package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{KeyIdentifier, VersionedKey}

trait UpdatableKeyStorage[T] {
  def replaceKeysWithNewVersion(keys: Set[(KeyIdentifier, T)]): Unit
  def getLastKey(environment: String, issuer: String): Option[VersionedKey[T]]
  def getKey(keyIdentifier: KeyIdentifier): Option[T]
}
