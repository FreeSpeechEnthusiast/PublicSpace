package com.twitter.auth.customerauthtooling.cli.commands.converters

case class StringSetWrapper(private val underlying: Set[String]) {
  def get(): Set[String] = {
    underlying
  }
}
