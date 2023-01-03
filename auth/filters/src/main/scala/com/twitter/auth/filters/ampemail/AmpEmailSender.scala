package com.twitter.auth.filters.ampemail

abstract class AmpEmailSender(email: String)

final case class NotAuthorizedAmpEmailSender(email: String) extends AmpEmailSender(email = email)
final case class AuthorizedAmpEmailSender(email: String) extends AmpEmailSender(email = email)
