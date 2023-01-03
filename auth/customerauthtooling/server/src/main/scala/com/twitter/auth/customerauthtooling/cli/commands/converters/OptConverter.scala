package com.twitter.auth.customerauthtooling.cli.commands.converters

import picocli.CommandLine.ITypeConverter

trait OptConverter[T] extends ITypeConverter[Option[T]] {
  protected def handleEmpty(): Option[T] = None
}
