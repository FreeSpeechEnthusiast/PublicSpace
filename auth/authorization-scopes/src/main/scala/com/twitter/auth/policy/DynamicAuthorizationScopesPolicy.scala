package com.twitter.auth.policy

import com.twitter.auth.authorizationscope.AuthorizationScopeConstants.{
  AdsRead,
  AdsReadScopeName,
  AppOnly,
  DeviceAuth,
  DmRead,
  EmailAddress,
  GuestAuth,
  IsWritable,
  Session,
  Tia
}
import com.twitter.auth.authorizationscope.{
  AuthorizationScope,
  AuthorizationScopeConstants,
  AuthorizationScopesMap
}
import com.twitter.util.Var

/**
 * WARNING: this class is ONLY used during implementation of PDP Customer Auth, and it's NOT the
 * right place for configuring policies for services running in Production.
 */
object DynamicAuthorizationScopesPolicy {
  val LegacyScopeInternalGroup = "legacy"

  // read only scope is a fake scope
  val ReadOnlyScope = Set(AuthorizationScopeConstants.ReadScopeName)

  private[auth] val mapOldScopesToNewScopes = Map[String, Set[String]](
    // From auth/authentication/src/main/scala/com/twitter/auth/authentication/utils/AccessTokenUtils.scala#L26
    // legacy scopes could have:
    // 1. IsWritable = "is_writable"
    // 2. DmRead = "dm_read"
    // 3. EmailAddress = "email_address"
    // 4. AdsRead = "ads_read"
    // 5. AdsReadWrite = "ads_read_write"
    IsWritable -> Set(
      AuthorizationScopeConstants.ReadWriteScopeName,
      AuthorizationScopeConstants.ReadScopeName),
    DmRead -> Set(
      AuthorizationScopeConstants.ReadWriteScopeName,
      AuthorizationScopeConstants.ReadScopeName,
      AuthorizationScopeConstants.ReadWriteDMScopeName
    ),
    EmailAddress -> Set(
      AuthorizationScopeConstants.EmailScopeName,
      AuthorizationScopeConstants.ReadScopeName
    ),
    AdsRead -> Set(
      AuthorizationScopeConstants.AdsReadScopeName,
      AuthorizationScopeConstants.ReadScopeName
    ),
    AdsReadScopeName -> Set(
      AuthorizationScopeConstants.AdsReadScopeName,
      AuthorizationScopeConstants.AdsReadWriteScopeName,
      AuthorizationScopeConstants.ReadScopeName
    ),
    // Currently session requests have all permissions supported in the system grant as all these actions should
    // be supported for web session requests. We might want to define what permissions will be supported for
    // session auth scope in future
    Session -> Set(
      AuthorizationScopeConstants.ReadWriteScopeName,
      AuthorizationScopeConstants.ReadScopeName,
      AuthorizationScopeConstants.ReadWriteDMScopeName,
      AuthorizationScopeConstants.EmailScopeName
    ),
    // Device auth requests can read and post tweet, so both read and write permissions are grant to device auth type
    DeviceAuth -> Set(
      AuthorizationScopeConstants.ReadWriteScopeName,
      AuthorizationScopeConstants.ReadScopeName),
    // Grant read permission to guest auth requests since it has the permission to read
    GuestAuth -> Set(AuthorizationScopeConstants.ReadScopeName),
    // Tia requests need legacy write (including read and write) permission to be able to hit write endpoints
    Tia -> Set(
      AuthorizationScopeConstants.ReadWriteScopeName,
      AuthorizationScopeConstants.ReadScopeName),
    // AppOnly requests has read permission only
    AppOnly -> Set(AuthorizationScopeConstants.ReadScopeName)
  )
}

class DynamicAuthorizationScopesPolicy(
  authorizationScopesConfigVar: Var[AuthorizationScopesMap])
    extends AuthorizationScopesPolicy {
  import DynamicAuthorizationScopesPolicy._

  override def authorizationScope(
    tokenPrivilege: String
  ): Option[AuthorizationScope] = {
    authorizationScopesConfigVar.sample().getScope(tokenPrivilege) match {
      case s: AuthorizationScope => Some(s)
      case _ => None
    }
  }

  override def authorizationScopes(tokenPrivileges: Set[String]): Set[String] = {
    // For token with Fine-Grained Scopes, verify them against the scope repo
    // https://config-git.twitter.biz/cgit/config/tree/auth/oauth2/scopes.json
    val finedGrainedScopes = tokenPrivileges
      .flatMap { privilege =>
        authorizationScope(privilege)
      }.filter(_.internalGroup != LegacyScopeInternalGroup).map(_.name).toSet

    if (finedGrainedScopes.nonEmpty) {
      // It should never happen that a token gives with both fine-grained and legacy scopes. If it
      // does ignore the legacy scopes and apply fine-grained scopes only.
      finedGrainedScopes
    } else {
      // For token with legacy token privileges, map them to legacy scopes
      var legacyScopes = mapOldScopesToNewScopes.keySet
        .foldLeft(Set.empty[String]) { (accumulator, keyInMapping) =>
          if (tokenPrivileges.contains(keyInMapping))
            accumulator.union(mapOldScopesToNewScopes(keyInMapping))
          else accumulator
        }

      // So far GuestAuth, AppOnly and "empty" all have read-only scope.
      // In the future, we might want to have dedicated scopes for guest and app-only
      if (legacyScopes.isEmpty)
        legacyScopes = legacyScopes.union(ReadOnlyScope)

      legacyScopes
    }
  }
}
