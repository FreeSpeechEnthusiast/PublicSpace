package com.twitter.ann.common

import com.twitter.search.common.file.AbstractFile.Filter
import com.twitter.search.common.file.AbstractFile
import java.util
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ShardedSerializationSpec extends AnyFunSuite with MockitoSugar {
  test(
    "ShardedSerialization successfully serializes shards to directories"
  ) {
    val shards = 4
    val serializations = Seq.fill(shards)(
      mock[Serialization]
    )

    val dir = mock[AbstractFile]
    val mockShardDirs = serializations.indices.map { id =>
      val mockShardDir = mock[AbstractFile]
      when(dir.getChild(s"${ShardConstants.ShardPrefix}$id")).thenReturn(mockShardDir)
      when(mockShardDir.exists()).thenReturn(false)
      when(mockShardDir.mkdirs()).thenReturn(true)
      mockShardDir
    }

    val shardedSerialization = new ShardedSerialization(serializations)
    shardedSerialization.toDirectory(dir)

    mockShardDirs.indices.foreach { id =>
      val serialization = serializations(id)
      val mockShardDir = mockShardDirs(id)
      verify(dir, times(1)).getChild(s"${ShardConstants.ShardPrefix}$id")
      verify(serialization, times(1)).toDirectory(mockShardDir)
      verify(mockShardDir, times(1)).mkdirs()
    }
  }

  test(
    "ComposedQueryableDeserialization successfully deserializes shards to indices"
  ) {
    val shardDirs = new util.ArrayList[AbstractFile]()
    val shardDir1 = mock[AbstractFile]
    val shardDir2 = mock[AbstractFile]
    shardDirs.add(shardDir1)
    shardDirs.add(shardDir2)

    val dir = mock[AbstractFile]
    when(dir.listFiles(any[Filter])).thenReturn(shardDirs)

    val mockFn = mock[(AbstractFile) => Queryable[Int, RuntimeParams, L2Distance]]
    when(mockFn(shardDir1)).thenReturn(mock[Queryable[Int, RuntimeParams, L2Distance]])
    when(mockFn(shardDir2)).thenReturn(mock[Queryable[Int, RuntimeParams, L2Distance]])

    val deserialization = new ComposedQueryableDeserialization(mockFn)
    val queryable = deserialization.fromDirectory(dir)

    assert(queryable.isInstanceOf[ComposedQueryable[Int, RuntimeParams, L2Distance]])
    verify(mockFn, times(1)).apply(shardDir1)
    verify(mockFn, times(1)).apply(shardDir2)
  }
}
