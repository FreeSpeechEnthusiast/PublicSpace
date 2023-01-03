package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.auth.policykeeper.thriftscala.BouncerRequest
import com.twitter.auth.policykeeper.thriftscala.Code
import com.twitter.auth.policykeeper.thriftscala.Result
import com.twitter.bouncer.thriftscala._
import com.twitter.finagle.http.Request
import com.twitter.finagle.http.Response
import com.twitter.finagle.stats.StatsReceiver
import com.twitter.finagle.thrift.ClientId
import com.twitter.finatra.api11.ApiError
import com.twitter.tfe.core.decider.TfeDecider
import com.twitter.tfe.core.bouncer.clients.BouncerClient
import com.twitter.tfe.core.api.ErrorResponseUtil.createCustomApiErrorResponse
import com.twitter.util.Future

class ResultHandler(
  decider: TfeDecider,
  bouncerClient: BouncerClient,
  clientId: ClientId,
  stats: StatsReceiver) {

  private[this] val scoped = stats.scope("ResultHandlingUtils")
  private[this] val bouncerEnrollmentSuccessCounter = scoped.counter("bouncer_enrollment_success")
  private[this] val bouncerEnrollmentFailedCounter = scoped.counter("bouncer_enrollment_failed")

  private[this] val bouncerExceptionMapper =
    new TFEBounceExceptionMapper(statsReceiver = stats)

  private[this] val apiErrorMapper = ApiErrorMapper()

  def handleResult(
    request: Request,
    result: Result
  ): Option[Future[Response]] = {
    result match {
      case Result(Code.True | Code.Mixed, Some(apiCode), _) =>
        request.reader.discard()
        val apiError = apiErrorMapper.getApiErrorByCode(apiCode).getOrElse(ApiError.DefaultApiError)
        Some(
          createCustomApiErrorResponse(
            request = request,
            code = apiError.code,
            status = Some(apiError.status),
            decider = decider,
            message = apiError.message, //TODO (AUTHPLT-2254): add custom message support
            detail = None, //TODO (AUTHPLT-2254): add custom message detail support
            messageType = None //TODO (AUTHPLT-2254): add custom message type support
          ))
      case Result(Code.True | Code.Mixed, None, Some(bouncerRequest)) =>
        request.reader.discard()
        Some(
          bouncerClient
            .enrollInModules(
              makeInternalLookup(target = bouncerRequest.target),
              makeModuleEnrollmentRequest(bouncerRequest = bouncerRequest)).flatMap {
              case EnrollmentResult.Enrolled(_) =>
                bouncerEnrollmentSuccessCounter.incr()
                Future.value(
                  bouncerExceptionMapper
                    .toResponse(request = request, exp = BounceException(bouncerRequest.bounce)))
              case _ =>
                bouncerEnrollmentFailedCounter.incr()
                createCustomApiErrorResponse(
                  request = request,
                  code = ApiError.InternalError.code, //TODO: add fallback http code
                  status = Some(ApiError.InternalError.status),
                  decider = decider,
                  message = ApiError.InternalError.message,
                  detail = None,
                  messageType = None
                )
            })
      case _ => None
    }
  }

  def makeModuleEnrollmentRequest(bouncerRequest: BouncerRequest) =
    ModuleEnrollmentRequest(
      templateIds = bouncerRequest.templateIds,
      templateData = bouncerRequest.templateData,
      referringTags = bouncerRequest.referringTags
    )

  def makeInternalLookup(
    target: Target
  ): InternalLookup =
    InternalLookup(
      target,
      InternalActor.SimpleService(SimpleService(clientId.name))
    )

}
