package com.twitter.auth.customerauthtooling.api.models

case class RouteStats(
  var id: String,
  var isHighRiskRoute: Boolean,
  var enforcedDPs: Boolean,
  var annotatedFPs: Int,
  var anyFailOpenFlags: Boolean,
  var annotatedDPs: Int,
  var annotatedDPIds: Seq[Long],
  var missingDpsAsPerRecommender: Seq[Long],
  var failOpenFlags: Set[String],
  var accessibleByScopes: Set[String],
  var supportedAuthTypes: Set[String])
