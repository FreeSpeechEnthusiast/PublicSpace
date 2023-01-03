package com.twitter.auth.customerauthtooling.api.models

import com.twitter.auth.customerauthtooling.thriftscala.{BatchRouteDraft => TBatchRouteDraft}

case class BatchRouteDraft(
  updated: Integer,
  inserted: Integer,
  ignoredInvalid: Integer,
  ignoredDueToErrors: Integer,
  unchanged: Integer,
  routeDrafts: Option[Set[RouteDraft]],
  wasStopped: Boolean,
  errors: Option[Seq[String]],
  warnings: Option[Seq[String]],
  messages: Option[Seq[String]]) {
  def toThrift: TBatchRouteDraft = {
    TBatchRouteDraft(
      updated = updated,
      inserted = inserted,
      ignoredInvalid = ignoredInvalid,
      ignoredDueToErrors = ignoredDueToErrors,
      unchanged = unchanged,
      routeDrafts = routeDrafts.map(_.map(_.toThrift)),
      wasStopped = wasStopped,
      errors = errors,
      warnings = warnings,
      messages = messages
    )
  }
}

object BatchRouteDraft {
  final val Empty = BatchRouteDraft(
    updated = 0,
    inserted = 0,
    ignoredInvalid = 0,
    ignoredDueToErrors = 0,
    unchanged = 0,
    routeDrafts = None,
    wasStopped = false,
    errors = None,
    warnings = None,
    messages = None
  )
}
