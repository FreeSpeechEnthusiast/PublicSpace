package com.twitter.auth.pasetoheaders.finagle

trait ConfigBusSource {
  protected val path: String
}

/**
 * Describes relative path inside configbus for non-local environment
 * @param path
 */
private case class ConfigBusSourceRemote(override protected val path: String)
    extends ConfigBusSource

/**
 * Describes relative path inside TSS for non-local environment
 * @param path
 */
private case class ConfigBusSourceTss(override protected val path: String) extends ConfigBusSource

/**
 * Describes absolute path for local environment or unit testing
 * @param path
 */
private case class ConfigBusSourceLocal(override protected val path: String) extends ConfigBusSource

/**
 * Describes resource path for local environment or unit testing
 * @param path
 */
private case class ConfigBusSourceResource(override protected val path: String)
    extends ConfigBusSource

object ConfigBusSource {

  /**
   * Sets absolute path on machine for local environment or unit testing
   *
   * @param absolutePath
   * @return
   */
  def local(absolutePath: String): ConfigBusSource = {
    ConfigBusSourceLocal(path = "/" + absolutePath.stripSuffix("/").stripPrefix("/"))
  }

  /**
   * Sets resources path on machine for local environment or unit testing
   *
   * @param resourcePath
   * @return
   */
  def resource(resourcePath: String): ConfigBusSource = {
    ConfigBusSourceResource(path = resourcePath.stripSuffix("/").stripPrefix("/"))
  }

  /**
   * Sets relative path inside configbus for non-local environment
   *
   * @param relativePath
   * @return
   */
  def remote(relativePath: String): ConfigBusSource = {
    ConfigBusSourceRemote(path = relativePath.stripSuffix("/"))
  }

  /**
   * Sets relative path inside configbus for non-local environment
   *
   * @param relativePath
   * @return
   */
  def tss(relativePath: String): ConfigBusSource = {
    ConfigBusSourceTss(path = relativePath.stripSuffix("/"))
  }
}
