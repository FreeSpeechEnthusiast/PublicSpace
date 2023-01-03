package com.twitter.auth.customerauthtooling.cli.commands

trait DefaultValue {

  /**
   * The macro is supported by Picocli framework
   */
  protected final val defaultValueMacro = "${DEFAULT" + "-VALUE}"
}
