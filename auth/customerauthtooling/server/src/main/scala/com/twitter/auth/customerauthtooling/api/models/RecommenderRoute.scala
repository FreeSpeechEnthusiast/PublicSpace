package com.twitter.auth.customerauthtooling.api.models

case class RecommenderRoute(
  path: String,
  method: String,
  dataPermissionIds: Set[Long],
  dataClassifications: Seq[String],
  cluster: Option[String],
  timeout: Int,
)
