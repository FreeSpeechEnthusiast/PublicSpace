package com.twitter.auth.passport_tools

import com.google.inject.Inject
import com.google.inject.Singleton
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.logging.Logger
import com.twitter.auth.context.AuthPasetoContext
import com.twitter.auth.pasetoheaders.models.Passports.Passport
import com.twitter.auth.pasetoheaders.passport.PassportExtractor
import com.twitter.finatra.authentication.AuthenticationPassportContext

/**
 * Class is designed as a helper for passport extraction within thrift and http(s) finatra services.
 *
 * @param logger
 * @param stats
 * @param passportExtractor Optionally, allows to re-use existing passportExtractor otherwise it will be automatically created
 */
@Singleton
class PassportRetriever @Inject() (
  logger: Logger,
  stats: StatsReceiver,
  passportExtractor: Option[PassportExtractor]) {

  private val scopedStats = stats.scope("PassportRetriever")

  /**
   * A passport extractor is not available in thrift services,
   * so we are creating it inside passport retriever
   * or using extractor from the constructor
   */
  private[passport_tools] val extractor = passportExtractor match {
    case Some(ex) => ex
    case None =>
      PassportExtractor(
        logger = Some(logger),
        stats = Some(scopedStats),
        None
      )
  }

  def passport: Option[Passport] = pasetoPassport

  private def pasetoPassport: Option[Passport] = AuthPasetoContext.getFromContexts match {
    case Some(token) =>
      AuthenticationPassportContext.getPassport(
        passportExtractor = extractor,
        authPasetoTokenString = token,
        verifyIntegrity = true)
    case None => None
  }

}
