package com.twitter.auth.sso.client

import com.twitter.auth.sso.models.{SsoId, SsoProvider, UserId}
import com.twitter.auth.sso.thriftscala.{SsoInfo => TSsoInfo}
import com.twitter.auth.sso.util.Monitor
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.stitch.{NotFound, Stitch, Timeout}
import com.twitter.strato.catalog.Scan.Slice
import com.twitter.strato.response.Err
import com.twitter.strato.generated.client.auth.sso.{
  SsoInfoBySsoIdClientColumn,
  SsoInfoByUserIdClientColumn
}

/**
 * These exceptions will be used to wrap downstream client exceptions
 */
case class ClientDataNotFoundException(message: String, cause: Throwable)
    extends Exception(message, cause)

case class ClientTimeoutException(message: String, cause: Throwable)
    extends Exception(message, cause)

case class ClientUnexpectedException(message: String, cause: Throwable)
    extends Exception(message, cause)

trait WithMonitoring {
  val statsReceiver: StatsReceiver
  val name: String = this.getClass.getSimpleName.stripSuffix("$")
  val monitor: Monitor = Monitor(statsReceiver.scope(name))
}

class StratoSsoInfoWriter(
  column: SsoInfoByUserIdClientColumn,
  val statsReceiver: StatsReceiver = NullStatsReceiver)
    extends WithMonitoring {
  private val putter = column.putter

  def write(userId: UserId, provider: SsoProvider, info: TSsoInfo): Stitch[Unit] = {
    monitor
      .trackStitch(
        putter.put((userId, SsoProvider.toThrift(provider)), info)
      ).rescue {
        case Timeout => Stitch.exception(ClientTimeoutException(s"$name: Timeout", Timeout))
        case e: Err =>
          Stitch.exception(
            ClientUnexpectedException(s"Strato Error: ${e.code} w/ reason = ${e.reason}", e))
        case e @ _ =>
          Stitch.exception(ClientUnexpectedException(s"$name: ${e.getClass.getSimpleName}", e))
      }
  }
}

class StratoSsoInfoDeleter(
  column: SsoInfoByUserIdClientColumn,
  val statsReceiver: StatsReceiver = NullStatsReceiver)
    extends WithMonitoring {
  private val deleter = column.deleter

  def delete(userId: UserId, provider: SsoProvider): Stitch[Unit] = {
    monitor
      .trackStitch(
        deleter.delete((userId, SsoProvider.toThrift(provider)))
      ).rescue {
        case Timeout => Stitch.exception(ClientTimeoutException(s"$name: Timeout", Timeout))
        case e: Err =>
          Stitch.exception(
            ClientUnexpectedException(s"Strato Error: ${e.code} w/ reason = ${e.reason}", e))
        case e @ _ =>
          Stitch.exception(ClientUnexpectedException(s"$name: ${e.getClass.getSimpleName}", e))
      }
  }
}

class StratoSsoInfoFetcher(
  column: SsoInfoByUserIdClientColumn,
  val statsReceiver: StatsReceiver = NullStatsReceiver)
    extends WithMonitoring {
  private val fetcher = column.fetcher

  def fetch(userId: UserId, provider: SsoProvider): Stitch[Option[TSsoInfo]] = {
    monitor
      .trackStitch(
        fetcher.fetch((userId, SsoProvider.toThrift(provider))).map(_.v)
      ).rescue {
        case NotFound =>
          Stitch.exception(ClientDataNotFoundException(s"$name: Not Found", NotFound))
        case Timeout => Stitch.exception(ClientTimeoutException(s"$name: Timeout", Timeout))
        case e: Err =>
          Stitch.exception(
            ClientUnexpectedException(s"Strato Error: ${e.code} w/ reason = ${e.reason}", e))
        case e @ _ =>
          Stitch.exception(ClientUnexpectedException(s"$name: ${e.getClass.getSimpleName}", e))
      }
  }
}

class StratoSsoUsersForSsoIdScanner(
  column: SsoInfoBySsoIdClientColumn,
  val statsReceiver: StatsReceiver = NullStatsReceiver)
    extends WithMonitoring {
  private val scanner = column.scanner
  def scan(ssoId: SsoId): Stitch[Seq[UserId]] = {
    monitor
      .trackStitch(
        scanner
          .scan((ssoId, Slice.all)).map(_.map {
            case ((_, (userId, _)), _) => userId
          })
      ).rescue {
        case Timeout => Stitch.exception(ClientTimeoutException(s"$name: Timeout", Timeout))
        case e: Err =>
          Stitch.exception(
            ClientUnexpectedException(s"Strato Error: ${e.code} w/ reason = ${e.reason}", e))
        case e @ _ =>
          Stitch.exception(ClientUnexpectedException(s"$name: ${e.getClass.getSimpleName}", e))
      }
  }
}

class StratoSsoInfoForUserScanner(
  column: SsoInfoByUserIdClientColumn,
  val statsReceiver: StatsReceiver = NullStatsReceiver)
    extends WithMonitoring {
  private val scanner = column.scanner

  def scan(userId: UserId): Stitch[Seq[TSsoInfo]] = {
    monitor
      .trackStitch(
        scanner
          .scan((userId, Slice.all)).map(_.map {
            case (_, v) => v
          })
      ).rescue {
        case Timeout => Stitch.exception(ClientTimeoutException(s"$name: Timeout", Timeout))
        case e: Err =>
          Stitch.exception(
            ClientUnexpectedException(s"Strato Error: ${e.code} w/ reason = ${e.reason}", e))
        case e @ _ =>
          Stitch.exception(ClientUnexpectedException(s"$name: ${e.getClass.getSimpleName}", e))
      }
  }
}
