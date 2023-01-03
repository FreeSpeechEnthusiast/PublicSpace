package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{RequestMethod => TRequestMethod}

object RequestMethod extends Enumeration {
  type RequestMethod = Value
  val Get, Post, Patch, Update, Delete, Head, Options = Value

  def toThrift(value: RequestMethod): TRequestMethod = {
    value match {
      case Get => TRequestMethod.Get
      case Post => TRequestMethod.Post
      case Patch => TRequestMethod.Patch
      case Update => TRequestMethod.Update
      case Delete => TRequestMethod.Delete
      case Head => TRequestMethod.Head
      case Options => TRequestMethod.Options
      // map unrecognized methods to get
      case _ => TRequestMethod.Get
    }
  }

  def fromThrift(thrift: TRequestMethod): RequestMethod = {
    thrift match {
      case TRequestMethod.Get => RequestMethod.Get
      case TRequestMethod.Post => RequestMethod.Post
      case TRequestMethod.Patch => RequestMethod.Patch
      case TRequestMethod.Update => RequestMethod.Update
      case TRequestMethod.Delete => RequestMethod.Delete
      case TRequestMethod.Head => RequestMethod.Head
      case TRequestMethod.Options => RequestMethod.Options
      // map unrecognized methods to get
      case _ => RequestMethod.Get
    }
  }
}
