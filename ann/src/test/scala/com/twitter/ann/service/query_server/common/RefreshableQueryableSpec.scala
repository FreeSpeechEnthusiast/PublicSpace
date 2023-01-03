package com.twitter.ann.service.query_server.common

import com.twitter.ann.common.L2Distance
import com.twitter.ann.common.Queryable
import com.twitter.ann.common.RuntimeParams
import com.twitter.conversions.DurationOps._
import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.search.common.file.AbstractFile
import com.twitter.util.Try
import java.io.IOException
import org.mockito.Mockito._
import org.scalatest.FunSuite
import scala.util.Random
import org.scalatestplus.mockito.MockitoSugar

class RefreshableQueryableSpec extends FunSuite with MockitoSugar {

  private val mockIndexDir = mock[AbstractFile]
  private val mockRootDir = mock[AbstractFile]
  private val mockQueryableProvider = mock[QueryableProvider[Int, RuntimeParams, L2Distance]]
  private val mockQueryable = mock[Queryable[Int, RuntimeParams, L2Distance]]
  private val timestamp = 1542750089L
  private val groupName = "123"
  private val mockGroupDir = mock[AbstractFile]

  when(mockIndexDir.getName).thenReturn(timestamp.toString)
  when(mockGroupDir.getName).thenReturn(groupName)
  when(mockQueryableProvider.provideQueryable(mockIndexDir)).thenReturn(mockQueryable)
  when(mockQueryableProvider.provideQueryable(mockGroupDir)).thenReturn(mockQueryable)

  test("Update reference correctly") {
    val statsReceiver = new InMemoryStatsReceiver
    val indexPathProvider = new IndexPathProvider {
      override def provideIndexPath(rootPath: AbstractFile, group: Boolean): Try[AbstractFile] =
        Try(mockIndexDir)

      override def provideIndexPathWithGroups(rootPath: AbstractFile): Try[Seq[AbstractFile]] =
        Try(Seq(mockGroupDir))
    }
    val q = new RefreshableQueryable[Int, RuntimeParams, L2Distance](
      false,
      mockRootDir,
      mockQueryableProvider,
      indexPathProvider,
      statsReceiver
    )
    assert(q.queryableRef.get() == Map(None -> mockQueryable))
    assert(q.indexPathRef.get() == mockIndexDir)
    assert(statsReceiver.counters(Seq("load")) == 0)
    assert(statsReceiver.counters(Seq("new_index")) == 0)
    assert(statsReceiver.counters(Seq("load_error")) == 0)
    assert(statsReceiver.gauges(Seq("serving_index_timestamp"))() == timestamp.toFloat)
  }

  test("Update reference correctly for groups") {
    val statsReceiver = new InMemoryStatsReceiver
    val indexPathProvider = new IndexPathProvider {
      override def provideIndexPath(rootPath: AbstractFile, group: Boolean): Try[AbstractFile] =
        Try(mockIndexDir)

      override def provideIndexPathWithGroups(rootPath: AbstractFile): Try[Seq[AbstractFile]] =
        Try(Seq(mockGroupDir))
    }
    val q = new RefreshableQueryable[Int, RuntimeParams, L2Distance](
      true,
      mockRootDir,
      mockQueryableProvider,
      indexPathProvider,
      statsReceiver
    )
    assert(q.queryableRef.get() == Map(Some(groupName) -> mockQueryable))
    assert(q.indexPathRef.get() == mockIndexDir)
    assert(statsReceiver.counters(Seq("load")) == 0)
    assert(statsReceiver.counters(Seq("new_index")) == 0)
    assert(statsReceiver.counters(Seq("load_error")) == 0)
    assert(statsReceiver.gauges(Seq("serving_index_timestamp"))() == timestamp.toFloat)
  }

  test("Reference does not get updated if already up to date") {
    val statsReceiver = new InMemoryStatsReceiver
    val indexPathProvider = new IndexPathProvider {
      override def provideIndexPath(rootPath: AbstractFile, group: Boolean): Try[AbstractFile] =
        Try(mockIndexDir)
      override def provideIndexPathWithGroups(rootPath: AbstractFile): Try[Seq[AbstractFile]] =
        Try(Seq(mockGroupDir))
    }
    val q = new RefreshableQueryable[Int, RuntimeParams, L2Distance](
      false,
      mockRootDir,
      mockQueryableProvider,
      indexPathProvider,
      statsReceiver
    )
    // call innerLoad with same index directory should not update atomic references
    q.innerLoad()
    assert(q.queryableRef.get() == Map(None -> mockQueryable))
    assert(q.indexPathRef.get() == mockIndexDir)
    assert(statsReceiver.counters(Seq("load")) == 1)
    assert(statsReceiver.counters(Seq("new_index")) == 0)
    assert(statsReceiver.counters(Seq("load_error")) == 0)
    assert(statsReceiver.gauges(Seq("serving_index_timestamp"))() == timestamp.toFloat)
  }

  test("Exception in load") {
    // fake some IOException in load
    val indexPathProvider = new IndexPathProvider {
      override def provideIndexPath(rootPath: AbstractFile, group: Boolean): Try[AbstractFile] =
        Try(throw new IOException())
      override def provideIndexPathWithGroups(rootPath: AbstractFile): Try[Seq[AbstractFile]] =
        Try(throw new IOException())
    }
    val statsReceiver = new InMemoryStatsReceiver
    intercept[IOException] {
      new RefreshableQueryable[Int, RuntimeParams, L2Distance](
        false,
        mockRootDir,
        mockQueryableProvider,
        indexPathProvider,
        statsReceiver
      )
    }
  }

  test("compute update interval properly") {
    val indexPathProvider = new IndexPathProvider {
      override def provideIndexPath(rootPath: AbstractFile, group: Boolean): Try[AbstractFile] =
        Try(mockIndexDir)
      override def provideIndexPathWithGroups(rootPath: AbstractFile): Try[Seq[AbstractFile]] =
        Try(throw new IOException())
    }
    val mockRandom = mock[Random]
    when(mockRandom.nextInt(300)).thenReturn(10)
    val q = new RefreshableQueryable[Int, RuntimeParams, L2Distance](
      false,
      mockRootDir,
      mockQueryableProvider,
      indexPathProvider,
      new NullStatsReceiver,
      1.minute
    ) {
      override protected val random: Random = mockRandom
    }
    assert(q.computeRandomInitDelay().inSeconds == 70)
  }
}
