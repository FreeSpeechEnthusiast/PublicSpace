package com.twitter.ann.scalding.offline

/*
Run with:
    ./bazel test --test-junit-output-mode=ALL ann/src/test/scala/com/twitter/ann/scalding/offline/...
 */

import cascading.flow.local.LocalFlowProcess

import com.twitter.ann.common.{InnerProduct, InnerProductDistance}
import com.twitter.core_workflows.user_model.thriftscala.CondensedUserState
import com.twitter.cortex.ml.embeddings.common.{GraphEdge, TweetKind, UserKind}
import com.twitter.entityembeddings.neighbors.thriftscala.{EntityKey, NearestNeighbors, Neighbor}
import com.twitter.ml.api.embedding.Embedding
import com.twitter.ml.featurestore.lib.embedding.EmbeddingWithEntity
import com.twitter.ml.featurestore.lib.{TweetId, UserId}
import com.twitter.scalding._
import com.twitter.usersource.snapshot.flat.thriftscala.FlatUser
import java.util.TimeZone
import org.scalatest.FunSuite

class KnnHelperTest extends FunSuite {
  // Set the implicits
  implicit val tz: TimeZone = TimeZone.getDefault()
  implicit val dp: DateParser = DateParser.default
  implicit val dateRange: DateRange =
    DateRange(RichDate("2017-10-03T00"), RichDate("2017-10-04T05"))
  implicit val uniqueId = UniqueID("test")
  val fp = new LocalFlowProcess(new java.util.Properties)

  type Neighborhood = (UserId, Seq[(TweetId, InnerProductDistance)])
  def assertNearestNeighour(
    actualNeighbors: Seq[Neighborhood],
    expectedNeighbors: Seq[Neighborhood]
  ): Unit = {
    assert(actualNeighbors.sortBy(_._1) == expectedNeighbors.sortBy(_._1))
  }

  test("bruteForceNearestNeighbors") {
    val consumerEmbeddings: TypedPipe[EmbeddingWithEntity[UserId]] = TypedPipe.from(
      List(
        EmbeddingWithEntity[UserId](
          entityId = UserId(1L),
          embedding = Embedding(List(0.0F, 9.0F, 5.0F).toArray)),
        EmbeddingWithEntity[UserId](
          entityId = UserId(2L),
          embedding = Embedding(List(9.0F, 5.0F, 0.0F).toArray))
      ))
    val producerEmbeddings: TypedPipe[EmbeddingWithEntity[UserId]] = TypedPipe.from(
      List(
        EmbeddingWithEntity[UserId](
          entityId = UserId(10L),
          embedding = Embedding(List(0.0F, 9.0F, 5.0F).toArray)),
        EmbeddingWithEntity[UserId](
          entityId = UserId(20L),
          embedding = Embedding(List(9.0F, 5.0F, 0.0F).toArray)),
        EmbeddingWithEntity[UserId](
          entityId = UserId(30L),
          embedding = Embedding(List(0.0F, 9.0F, 6.0F).toArray)),
        EmbeddingWithEntity[UserId](
          entityId = UserId(40L),
          embedding = Embedding(List(9.0F, 0.0F, 0.0F).toArray))
      ))

    val neighbors2: List[(Long, Set[Long])] = KnnHelper
      .bruteForceNearestNeighbors(
        consumerEmbeddings = consumerEmbeddings,
        producerEmbeddings = producerEmbeddings,
        numNeighbors = 2,
        reducers = 1)
      .map {
        case (ekey: EntityKey, nneighbors: NearestNeighbors) =>
          (ekey.id.toLong, nneighbors.neighbors.map(_.neighbor.id.toLong).toSet)
      }.toIterableExecution.waitFor(Config.default, Local(true)).get.toList

    assert(neighbors2.size == 2)
    assert(neighbors2.contains((1L, Set(10L, 30L))))
    assert(neighbors2.contains((2L, Set(40L, 20L))))

  }

  test("getDebugTable") {
    val consumerStatesDataset: Option[TypedPipe[CondensedUserState]] = Some(
      TypedPipe.from(
        List(
          CondensedUserState(1L, 0.0, "10", "", "", 0, 0, "", true),
          CondensedUserState(2L, 0.0, "20", "", "", 0, 0, "", true),
          CondensedUserState(3L, 0.0, "30", "", "", 0, 0, "", true)
        )))

    val followDataset: Option[TypedPipe[GraphEdge[UserId, UserId]]] = Some(
      TypedPipe.from(List(
        GraphEdge[UserId, UserId](consumerId = UserId(1L), itemId = UserId(10L), weight = 1F),
        GraphEdge[UserId, UserId](consumerId = UserId(1L), itemId = UserId(20L), weight = 1F),
        GraphEdge[UserId, UserId](consumerId = UserId(2L), itemId = UserId(10L), weight = 1F)
      )))

    val userDataset: Option[TypedPipe[FlatUser]] = Some(
      TypedPipe.from(List(
        FlatUser(id = Some(10L), screenName = Some("10name")),
        FlatUser(id = Some(20L), screenName = Some("20name")),
        FlatUser(id = Some(30L), screenName = Some("30name")),
        FlatUser(id = Some(40L), screenName = Some("40name"))
      )))

    val neighborsPipe: TypedPipe[(EntityKey, NearestNeighbors)] = TypedPipe.from(
      List(
        (
          EntityKey(id = "1"),
          NearestNeighbors(
            neighbors = List(Neighbor(EntityKey(id = "10")), Neighbor(EntityKey(id = "30")))
          )),
        (
          EntityKey(id = "2"),
          NearestNeighbors(
            neighbors = List(Neighbor(EntityKey(id = "10")), Neighbor(EntityKey(id = "20")))
          ))
      ))

    val table: List[(String, String, String, String)] = KnnDebug
      .getDebugTable(
        neighborsPipe = neighborsPipe,
        shards = 1,
        reducers = 1,
        minFollows = 0,
        maxFollows = 50,
        userDataset = userDataset,
        followDataset = followDataset,
        consumerStatesDataset = consumerStatesDataset
      ).toIterableExecution.waitFor(Config.default, Local(true)).get.toList

    assert(table.size == 2)
    assert(table.contains(("1", "10", "10name<f>20name", "10name<n>30name")))
    assert(table.contains(("2", "20", "10name", "10name<n>20name")))
  }

  test("findNearestNeighbours") {
    val metric = InnerProduct
    val neighbors = 2
    val queryEntityKind = UserKind
    val queryEmbeddings: TypedPipe[EmbeddingWithEntity[UserId]] = TypedPipe.from(
      List(
        EmbeddingWithEntity[UserId](entityId = UserId(1L), embedding = Embedding(List(2F).toArray)),
        EmbeddingWithEntity[UserId](entityId = UserId(2L), embedding = Embedding(List(1F).toArray))
      ))
    val searchSpaceEmbeddings: TypedPipe[EmbeddingWithEntity[TweetId]] = TypedPipe.from(
      List(
        EmbeddingWithEntity[TweetId](
          entityId = TweetId(10L),
          embedding = Embedding(List(3F).toArray)),
        EmbeddingWithEntity[TweetId](
          entityId = TweetId(20L),
          embedding = Embedding(List(2F).toArray)),
        EmbeddingWithEntity[TweetId](
          entityId = TweetId(30L),
          embedding = Embedding(List(1F).toArray))
      ) ++ List.fill(12)(
        EmbeddingWithEntity[TweetId](
          entityId = TweetId(40L),
          embedding = Embedding(List(1F).toArray)))
    )

    val firstNeighbor: Neighborhood = (
      UserId(1L),
      Seq((TweetId(10L), InnerProductDistance(-5F)), (TweetId(20L), InnerProductDistance(-3.0F))))
    val secondNeighbor: Neighborhood = (
      UserId(2L),
      Seq((TweetId(10L), InnerProductDistance(-2F)), (TweetId(20L), InnerProductDistance(-1.0F))))

    // Find neighbours without queryId filter
    assertNearestNeighour(
      actualNeighbors = KnnHelper
        .findNearestNeighbours(
          queryEmbeddings,
          searchSpaceEmbeddings,
          metric,
          neighbors,
          useCounters = false
        )(queryEntityKind.ordering, uniqueId).toIterableExecution.waitFor(
          Config.default,
          Local(true)).get.toList,
      expectedNeighbors = Seq(firstNeighbor, secondNeighbor)
    )

    // Find neighbours without queryId filter and search space is smaller
    assertNearestNeighour(
      actualNeighbors = KnnHelper
        .findNearestNeighbours(
          queryEmbeddings,
          searchSpaceEmbeddings,
          metric,
          neighbors,
          isSearchSpaceLarger = false,
          useCounters = false
        )(queryEntityKind.ordering, uniqueId).toIterableExecution.waitFor(
          Config.default,
          Local(true)).get.toList,
      expectedNeighbors = Seq(firstNeighbor, secondNeighbor)
    )

    // Find neighbours using search groups.
    assertNearestNeighour(
      actualNeighbors = KnnHelper
        .findNearestNeighbours(
          queryEmbeddings,
          searchSpaceEmbeddings,
          metric,
          neighbors,
          numOfSearchGroups = 3,
          numReplicas = 3,
          useCounters = false
        )(queryEntityKind.ordering, uniqueId).toIterableExecution.waitFor(
          Config.default,
          Local(true)).get.toList,
      expectedNeighbors = Seq(firstNeighbor, secondNeighbor)
    )

    // Find neighbours with queryId filter
    val queryIdsFilter = TypedPipe.from(List(UserId(1L)))
    assertNearestNeighour(
      actualNeighbors = KnnHelper
        .findNearestNeighbours(
          queryEmbeddings,
          searchSpaceEmbeddings,
          metric,
          neighbors,
          Some(queryIdsFilter),
          useCounters = false
        )(queryEntityKind.ordering, uniqueId).toIterableExecution.waitFor(
          Config.default,
          Local(true)).get.toList,
      expectedNeighbors = Seq(firstNeighbor)
    )
  }

  test("nearestNeighborsToString") {
    val nearestNeighbors = (
      UserId(1L),
      Seq((TweetId(2L), InnerProductDistance(1.0F)), (TweetId(3L), InnerProductDistance(-2.0F)))
    )

    val queryEntityKind = UserKind
    val neighborsEntityKind = TweetKind

    // Nearest neighbor string with default format
    val nnString = KnnHelper.nearestNeighborsToString(
      nearestNeighbors,
      queryEntityKind,
      neighborsEntityKind
    )

    assert(nnString == "1\t2:1.0\t3:-2.0")

    // Nearest neighbor string with custom format
    val nnStringCustom = KnnHelper.nearestNeighborsToString(
      nearestNeighbors,
      queryEntityKind,
      neighborsEntityKind,
      idDistanceSeparator = "#",
      neighborSeparator = ":"
    )

    assert(nnStringCustom == "1:2#1.0:3#-2.0")
  }
}
