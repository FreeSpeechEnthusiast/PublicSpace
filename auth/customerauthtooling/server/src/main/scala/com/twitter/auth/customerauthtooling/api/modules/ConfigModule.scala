package com.twitter.auth.customerauthtooling.api.modules

import com.twitter.inject.TwitterModule

object ConfigModule extends TwitterModule {
  flag[String](
    "dp.recommender.file.path",
    "auth/customerauthtooling/server/src/main/resources/dp_recommender_output.csv",
    "path for csv file of recommender"
  )

  flag[String](
    "dps.list.file.path",
    "auth/customerauthtooling/server/src/main/resources/dps.csv",
    "path for csv file of recommender"
  )

  flag[String](
    "scope.to.dps.file.path",
    "auth/customerauthtooling/server/src/main/resources/scope_to_dps.csv",
    "path for scope to dp csv"
  )
}
