# DAL

# Running a dev DAL instance

Too run a local instance with an in-memory DB, do the following:

    adp/dal_v2/server/deploy.sh local

This starts two databases, DAL v2 and DAL v1, respectively. The ports for both will be in the logs.
Connect to v2 and v1 like this as follows, respectively. DAL v1 creates a db named ```test_[random_num]```

    mysql -h 127.0.0.1 -P [port1] -u testuser --password=testpassword dal_devel_db
    mysql -h 127.0.0.1 -P [port2] -u testuser --password=testpassword

# Connecting to a local DAL service via the repl

    ./adp/dal_v2/server/repl/start.sh

    val client = LocalClient()

    val app = BatchApp(name = "test-app-name", domain = "test-domain", role = "test-role", environment = Environment.Dev)
    val datasetName = DatasetName(role = "dataset-role", environment = Environment.Dev, name = "dataset-name")
    val endTime = Time.now.floor(1.hour)
    val startTime = endTime - 1.hour
    val interval = TemporalInterval(startIncluded = Some(Timestamp(startTime.inMillis)),
                                    endExcluded   = Some(Timestamp(endTime.inMillis)))
    val physicalLocation = PhysicalLocation(locationType = Constants.HadoopCluster, name = "bb-smf1")

    val context = PhysicalRunContext(appComponent = app, range = interval, runId = 1,
                    appType = "scalding", deploymentLocation = physicalLocation)

    val appContext = ApplicationContext(appComponent = app, appType = "testAppType", deploymentLocation = physicalLocation)

    val createResult = Await.result(client.createLogicalDataset(appContext, datasetName, Schema.Unknown(UnknownSchema(None))))
    val getResult    = Await.result(client.getLogicalDataset(datasetName)).logicalDataset

    val auditResult  = Await.result(client.auditInputSegments(context, datasetName, interval))

  #TODO: add seed data, or support for creating input segments so above doesn't throw LogicalDatasetNotFoundException

# Connecting to a remote DAL service from your laptop

You can connect via a sock proxy (http://go/socks) like below and then use serversets.

    $ ./adp/dal_v2/server/repl/start.sh

Once the repl has started, you may use one of the following:

    val client = ThriftMux.client.newIface[Dal.MethodPerEndpoint]("replUser=/cluster/local/dal-staging/staging/dal")
    val client = ThriftMux.client.newIface[Dal.MethodPerEndpoint]("replUser=/s/dal/dal")
    val dalClient = DALClientImpl(ThriftDALClientConfig("/s/dal/dal"), dalContext)

# Fetching a schema from Prod using the DALClient

    val env = Environment.Prod
    val dalContext = Context.ApplicationContext(ApplicationContext(
      appComponent = BatchApp(
        name = "repl test", role = System.getenv("USER"), environment = env, domain = "testingrepl"
      ),
      appType = "test-app",
      deploymentLocation = PhysicalLocation(com.twitter.dal.thriftscala.Constants.DataCenter, "smf1")
    ))

    val dalClient = DALClientImpl(ThriftDALClientConfig("/s/dal/dal"), dalContext)

    val datasetName = DatasetName(role = "coremetrics", environment = env, name = "user_audit_final_parquet")
    val location = PhysicalLocation(locationType = Constants.HadoopCluster, name = "dw2-smf1")
    val interval = TemporalInterval(Some(Timestamp(1456617600000L)),Some(Timestamp(1456704000000L)))

    val inputSegments = Await.result(dalClient.findInputSegments(
      datasetName.name,
      datasetName.role,
      Some(location),
      interval,
      Set(SegmentPropertyKey.Schema)
    ))

    val schema = inputSegments.segments.headOption.map { segment =>
      segment.properties(SegmentPropertyKey.Schema) match {
        case SegmentPropertyValue.Schema(schema) =>
          schema
        case ns =>
          throw new RuntimeException(
            s"Expected to find schema for dataset in findInputSegments response for datasetName, but found $ns")
      }
    }

    # to see the hive schema, add interactive-query/dalv2-sync/src/main/scala/com/twitter/dalsync to the targets
    # in repl/start.sh and do the following:

    import com.twitter.dalsync.SchemaUtils
    val fields: java.util.List[org.apache.hadoop.hive.metastore.api.FieldSchema] = SchemaUtils.parseSchema(schema.get)
