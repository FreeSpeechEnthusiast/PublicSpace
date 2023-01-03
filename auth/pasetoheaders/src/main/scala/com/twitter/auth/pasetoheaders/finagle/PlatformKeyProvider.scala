package com.twitter.auth.pasetoheaders.finagle

private trait PlatformKeyProvider {

  protected val LOGGING_SCOPE = "privatekeyprovider"

  protected def doStatementIfOptionSet[O](option: Option[O], statement: O => Unit): Unit = {
    option match {
      case Some(o) => statement(o)
      case _ =>
    }
  }

  protected def commonLogMetadata: Map[String, String] = {
    Map()
  }

}
