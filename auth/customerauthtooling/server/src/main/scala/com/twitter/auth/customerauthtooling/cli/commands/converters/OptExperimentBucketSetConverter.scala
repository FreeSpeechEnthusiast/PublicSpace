package com.twitter.auth.customerauthtooling.cli.commands.converters

import com.twitter.auth.customerauthtooling.thriftscala.ExperimentBucket
import scala.util.matching.Regex

class OptExperimentBucketSetConverter extends OptConverter[ExperimentBucketSetWrapper] {
  override protected def handleEmpty(): Option[ExperimentBucketSetWrapper] = Some(
    ExperimentBucketSetWrapper(Set()))

  private val experimentBucketSetPattern: Regex = "^((\\w+):(\\w+),)*(\\w+):(\\w+)$".r

  /**
   * Converts string like bucket1:experiment1,bucket2:experiment2 to optional set of experiment buckets
   *
   * @param value
   * @return
   */
  def convert(value: String): Option[ExperimentBucketSetWrapper] = {
    value match {
      case experimentBucketSetPattern(_*) =>
        val experimentBuckets = value
          .split(",")
          .map(_.split(":"))
          .collect {
            case Array(bucket, key) => ExperimentBucket(key = key, bucket = bucket)
          }
        Some(ExperimentBucketSetWrapper(experimentBuckets.toSet))
      case "" => handleEmpty()
      case _ =>
        throw InvalidInputException(s"Unrecognized comma separated experiment bucket set: $value")
    }
  }
}
