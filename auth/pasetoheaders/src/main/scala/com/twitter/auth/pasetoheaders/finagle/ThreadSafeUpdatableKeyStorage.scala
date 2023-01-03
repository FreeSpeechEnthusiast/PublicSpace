package com.twitter.auth.pasetoheaders.finagle

import com.twitter.auth.pasetoheaders.encryption.{InMemoryKeyStorage, KeyIdentifier, VersionedKey}
import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable
import com.twitter.auth.pasetoheaders.javahelpers.OptionConv._
import com.twitter.util.ScheduledThreadPoolTimer
import java.security.Key
import scala.annotation.tailrec
import com.twitter.conversions.DurationOps._

/**
 * Class provides thread safe updatable key storage.
 * Warning! Experimental project, use at your own risk
 */
case class ThreadSafeUpdatableKeyStorage[T <: Key]() extends UpdatableKeyStorage[T] {
  private val currentUpdateTransactionId: AtomicLong = new AtomicLong(0L)
  private val updateTransactionStatuses: mutable.HashMap[
    Long,
    TransactionStatus.TransactionStatus
  ] =
    mutable.HashMap()
  private val storage: mutable.HashMap[Long, InMemoryKeyStorage[T]] = mutable.HashMap()
  private val cleaningInterval = 30.seconds

  /**
   * how many last versions except the last one we want to keep in memory
   * by default set to 1, meaning that we keep the last one plus one more
   * making sure that readers can finish their operations
   */
  private val keepVersionsCount = 1

  // old version cleanup
  new ScheduledThreadPoolTimer(1)
    .schedule(period = cleaningInterval)(
      cleanOldVersion
    )

  private def cleanOldVersion(): Unit = {
    val cv: Long = currentUpdateTransactionId.get()
    storage foreach {

      /**
       * Cleaning up all versions below current minus keepVersionsCount
       */
      case (version, _) if version < cv - keepVersionsCount =>
        storage.remove(version)
    }
  }

  def replaceKeysWithNewVersion(keys: Set[(KeyIdentifier, T)]): Unit = {
    val nextUpdateTransactionId = currentUpdateTransactionId.incrementAndGet();
    updateTransactionStatuses += (nextUpdateTransactionId -> TransactionStatus.transactionStarted)
    storage += (nextUpdateTransactionId -> new InMemoryKeyStorage[T]())
    keys foreach {
      case (keyIdentifier, key) =>
        storage(nextUpdateTransactionId).addKey(keyIdentifier, key)
    }
    updateTransactionStatuses += (nextUpdateTransactionId -> TransactionStatus.transactionFinished)
  }

  @tailrec
  private def waitForTransactionAndThen[C](doWhenReady: Long => Option[C]): Option[C] = {
    val cv: Long = currentUpdateTransactionId.get()
    cv match {
      case 0L =>
        // key storage is empty
        Option.empty[C]
      case transactionId =>
        // wait for pending transaction completion
        if (storage.contains(transactionId) &&
          updateTransactionStatuses(transactionId) == TransactionStatus.transactionFinished) {
          doWhenReady(transactionId)
        } else {
          waitForTransactionAndThen(transactionId => doWhenReady(transactionId))
        }
    }
  }

  def getLastKey(environment: String, issuer: String): Option[VersionedKey[T]] = {
    waitForTransactionAndThen(transactionId =>
      storage(transactionId).getLastKey(environment, issuer))
  }

  def getKey(keyIdentifier: KeyIdentifier): Option[T] = {
    waitForTransactionAndThen(transactionId => storage(transactionId).getKey(keyIdentifier))
  }
}

object TransactionStatus extends Enumeration {
  type TransactionStatus = Value

  val transactionStarted = Value("Started")
  val transactionFinished = Value("Finished")
}
