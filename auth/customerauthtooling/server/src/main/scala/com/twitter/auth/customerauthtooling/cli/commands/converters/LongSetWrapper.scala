package com.twitter.auth.customerauthtooling.cli.commands.converters

case class LongSetWrapper(private val underlying: Set[Long]) {
  def get(): Set[Long] = {
    underlying
  }
}
