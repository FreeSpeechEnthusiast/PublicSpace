package com.twitter.ann.annoy

import com.twitter.ann.common.thriftscala.{AnnoyRuntimeParam, RuntimeParams => ServiceRuntimeParams}
import org.scalatest.FunSuite
import scala.util.Success
import org.scalatestplus.mockito.MockitoSugar

class AnnoyCommonTest extends FunSuite with MockitoSugar {
  val mockUnknown = mock[ServiceRuntimeParams.UnknownUnionField]

  test("RuntimeParamsInjection apply") {
    val result = AnnoyCommon.RuntimeParamsInjection.apply(AnnoyRuntimeParams(Some(2)))
    assert(result == ServiceRuntimeParams.AnnoyParam(AnnoyRuntimeParam(Some(2))))
  }
  test("RuntimeParamsInjection invert") {
    val result = AnnoyCommon.RuntimeParamsInjection
      .invert(ServiceRuntimeParams.AnnoyParam(AnnoyRuntimeParam(Some(2))))
    assert(result == Success(AnnoyRuntimeParams(Some(2))))
  }
  test("RuntimeParamsInjection invalid invert") {
    val result = AnnoyCommon.RuntimeParamsInjection.invert(
      mockUnknown
    )
    val exception = intercept[IllegalArgumentException](result.get)
    assert(exception.getMessage == s"Expected AnnoyRuntimeParams got $mockUnknown")
  }
}
