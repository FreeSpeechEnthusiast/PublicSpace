package com.twitter.ann.service.query_server.common

import com.twitter.finagle.stats.InMemoryStatsReceiver
import com.twitter.finagle.stats.NullStatsReceiver
import com.twitter.search.common.file.LocalFile
import java.io.File
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.scalatest.FunSuite
import org.scalatestplus.mockito.MockitoSugar

class ValidatedIndexPathProviderSpec extends FunSuite with MockitoSugar {
  private val ts_20181001 = "1538352000"
  private val ts_20181002 = "1538438400"
  private val ts_20181003 = "1538524800"
  private val tempDir = Files.createTempDirectory("test_").toFile
  tempDir.deleteOnExit()

  private val invalidDir = new File(tempDir, ts_20181001)
  private val validDir = new File(tempDir, ts_20181002)
  private val fakeFileInValidDir = new File(validDir, "fake_file")

  validDir.mkdir()
  invalidDir.mkdir()
  fakeFileInValidDir.createNewFile()

  Files.write(fakeFileInValidDir.toPath, "abc".getBytes())
  private val size = fakeFileInValidDir.length()

  test("ValidatedIndexPathProvider provides latest path") {
    // make sure validDir has _SUCCESS file
    val successFileInValidDir = new File(validDir, "_SUCCESS")
    successFileInValidDir.createNewFile()
    // copy the valid dir but with newer timestamp suffix
    val copiedValidDir = new File(tempDir, ts_20181003)
    copiedValidDir.mkdir()
    FileUtils.copyDirectory(validDir, copiedValidDir)

    val statsReceiver = new InMemoryStatsReceiver
    val p = ValidatedIndexPathProvider(0, size + 1, statsReceiver)
    val dir = p.provideIndexPath(new LocalFile(tempDir), false).get()
    // should pick copiedValidDir because it has newer timestamp than validDir
    assert(dir == new LocalFile(copiedValidDir))
    assert(statsReceiver.counters(Seq("provide_path_success")) == 1)
    assert(statsReceiver.counters(Seq("find_latest_path_fail")) == 0)
    assert(statsReceiver.counters(Seq("invalid_index")) == 0)
    assert(statsReceiver.gauges(Seq("latest_index_timestamp"))() == ts_20181003.toFloat)
    assert(statsReceiver.gauges(Seq("latest_valid_index_timestamp"))() == ts_20181003.toFloat)
    FileUtils.deleteDirectory(copiedValidDir)
    // delete _SUCCESS file
    successFileInValidDir.delete()
  }

  test("ValidatedIndexPathProvider throw if latest path has no _SUCCESS file") {
    val statsReceiver = new InMemoryStatsReceiver
    val p = ValidatedIndexPathProvider(0, size + 1, statsReceiver)
    val res = p.provideIndexPath(new LocalFile(tempDir), false)
    assert(res.isThrow)
    assert(statsReceiver.counters(Seq("provide_path_success")) == 0)
    assert(statsReceiver.counters(Seq("find_latest_path_fail")) == 1)
    assert(statsReceiver.counters(Seq("invalid_index")) == 0)
  }

  test("ValidatedIndexPathProvider throw if directory fail size checking") {
    // make sure validDir has _SUCCESS file
    val successFileInValidDir = new File(validDir, "_SUCCESS")
    successFileInValidDir.createNewFile()
    val statsReceiver = new InMemoryStatsReceiver
    // create ValidatedIndexPathProvider that expects larger index than what we have
    val p = ValidatedIndexPathProvider(size + 1, size + 3, statsReceiver)
    val res = p.provideIndexPath(new LocalFile(tempDir), false)
    assert(res.isThrow)
    assert(statsReceiver.counters(Seq("provide_path_success")) == 0)
    assert(statsReceiver.counters(Seq("find_latest_path_fail")) == 1)
    assert(statsReceiver.counters(Seq("invalid_index")) == 0)
    successFileInValidDir.delete()
  }

  test("index directory should have timestamp") {
    // fake a directory without timestamp in path
    val dir = Files.createTempDirectory("test_timestamp_").toFile
    dir.deleteOnExit()
    val indexDir = new File(dir, "index_dir")
    indexDir.mkdir()
    val successFile = new File(indexDir, "_SUCCESS")
    successFile.createNewFile()
    val fakeFile = new File(indexDir, "fake_file")
    Files.write(fakeFile.toPath, "abc".getBytes())
    val p = ValidatedIndexPathProvider(size + 1, size + 3, new NullStatsReceiver)
    val res = p.provideIndexPath(new LocalFile(indexDir), false)
    assert(res.isThrow)
  }

  test("find latest valid directory") {
    // make sure validDir has _SUCCESS file
    val successFileInValidDir = new File(validDir, "_SUCCESS")
    successFileInValidDir.createNewFile()
    // fake a dir with newer timestamp and SUCCESS file
    val latestInvalidDir = new File(tempDir, ts_20181003)
    latestInvalidDir.mkdir()
    val successFileInLatestInvalidDir = new File(latestInvalidDir, "_SUCCESS")
    successFileInLatestInvalidDir.createNewFile()
    val statsReceiver = new InMemoryStatsReceiver
    val p = ValidatedIndexPathProvider(0, size + 1, statsReceiver)
    val dir = p.provideIndexPath(new LocalFile(tempDir), false).get()
    // although latestInvalidDir has newer timestamp, but it is not valid,
    // should still return validDir
    assert(dir == new LocalFile(validDir))
    assert(statsReceiver.counters(Seq("provide_path_success")) == 1)
    assert(statsReceiver.counters(Seq("find_latest_path_fail")) == 0)
    assert(statsReceiver.counters(Seq("invalid_index")) == 1)

    assert(statsReceiver.gauges(Seq("latest_index_timestamp"))() == ts_20181003.toFloat)
    assert(statsReceiver.gauges(Seq("latest_valid_index_timestamp"))() == ts_20181002.toFloat)

    successFileInValidDir.delete()
    latestInvalidDir.delete()
  }
}
