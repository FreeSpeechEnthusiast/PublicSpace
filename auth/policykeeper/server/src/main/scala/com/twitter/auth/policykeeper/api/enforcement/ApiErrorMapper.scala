package com.twitter.auth.policykeeper.api.enforcement

import com.twitter.finatra.api11.ApiError
import reflect.runtime.universe._
import com.twitter.util.Try

case class ApiErrorMapper() {
  private val reflectionApi = runtimeMirror(ApiError.getClass.getClassLoader)

  /**
   * builds hashmap of known ApiErrors in runtime
   * */
  private val apiErrorsByApiCode =
    typeOf[ApiError].companion.members.view
    // iterate through all public properties of ApiError's companion
      .filter { x => x.isPublic }.map { s =>
        // collecting values of public properties
        // public methods like apply and unapply will return an exception due to missing parameters
        // therefore they will be ignored
        Try {
          reflectionApi.reflect(ApiError).reflectField(s.asTerm).get
        }.toOption
      }.collect {
        // filtering out properties with ApiError as a value and preparing format for toMap
        case Some(value: ApiError) => (value.code, value)
      }
      // building a hashmap of ApiError.code to ApiError
      .toMap

  def getApiErrorByCode(code: Int): Option[ApiError] = {
    apiErrorsByApiCode.get(code)
  }
}
