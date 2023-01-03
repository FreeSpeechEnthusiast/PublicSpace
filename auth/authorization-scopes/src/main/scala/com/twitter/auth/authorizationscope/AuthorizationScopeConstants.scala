package com.twitter.auth.authorizationscope

object AuthorizationScopeConstants {
  // Legacy Scopes
  private[auth] val IsWritable = "is_writable"
  private[auth] val DmRead = "dm_read"
  private[auth] val EmailAddress = "email_address"
  // TODO: Support following legacy token privileges
  private[auth] val AdsRead = "ads_read"
  private[auth] val AdsReadWrite = "ads_read_write"
  private[auth] val MultipleOAuthSession = "multiple_oauth_session"

  private[auth] val Session = "Session" // denotes a full session scope permission
  // typically for a customer with a web browser client auth
  private[auth] val DeviceAuth = "DeviceAuth" // represents the scope for Device auth requests
  private[auth] val GuestAuth = "GuestAuth" // represents the scope for guest auth requests
  private[auth] val Tia = "Tia" // represents the scope for Tia requests
  private[auth] val AppOnly = "AppOnly" // represents the scope for AppOnly requests

  /* important - below is a temp static list of new scopes.
     Actual new scopes are coming soon.
   */
  // static NEW scopes that are needed in order to map old -> new -> DPs
  private[auth] val ReadScopeName: String = "read_scope"
  private[auth] val ReadWriteScopeName: String = "read_write_scope"
  private[auth] val ReadWriteDMScopeName: String = "read_write_dm_scope"
  private[auth] val EmailScopeName: String = "email_scope"
  private[auth] val AdsReadScopeName: String = "ads_read_scope"
  private[auth] val AdsReadWriteScopeName: String = "ads_read_write_scope"
}
