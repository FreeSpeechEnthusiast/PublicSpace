package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.customerauthtooling.thriftscala.ExperimentBucket

case class ExperimentBucketSetWrapper(private val underlying: Set[ExperimentBucket]) {
  def get(): Set[ExperimentBucket] = {
    underlying
  }
}
