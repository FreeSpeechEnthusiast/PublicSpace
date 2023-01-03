package com.twitter.ann.common

import com.twitter.search.common.file.LocalFile
import java.nio.file.Files
import org.apache.beam.sdk.io.LocalResources
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IndexOutputFileTest extends AnyFunSuite {

  test("Test createDirectory for AbstractFile integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val temp = new IndexOutputFile(localFile)
    assert(temp.isDirectory())
    assert(temp.isAbstractFile())

    // Test creating a directory
    val inside_test = temp.createDirectory("inside_test")
    assert(inside_test.isDirectory())
    assert(localFile.getChild("inside_test").exists())
    assert(localFile.getChild("inside_test").isDirectory)
  }

  test("Test createFile and outputStream for AbstractFile integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val temp = new IndexOutputFile(localFile)
    assert(temp.isDirectory())
    assert(temp.isAbstractFile())

    // Test creating a file
    val test_file = temp.createFile("test_file")
    assert(!test_file.isDirectory())

    // Test OutputStream
    val expectedContent = "test output"
    val out = test_file.getOutputStream()
    out.write(expectedContent.getBytes())
    out.close()
    assert(localFile.getChild("test_file").exists())
    val actualContent = localFile.getChild("test_file").getByteSource.read()
    assert(actualContent === expectedContent.getBytes())

    // Test Path
    assert(temp.getPath() == localFile.getPath) // Directory
    assert(test_file.getPath() == localFile.getChild("test_file").getPath) // File
  }

  test("Test SUCCESS file for AbstractFile integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val temp = new IndexOutputFile(localFile)
    assert(temp.isDirectory())
    assert(temp.isAbstractFile())

    // Test Success file creation
    temp.createSuccessFile()
    assert(localFile.getChild("_SUCCESS").exists())
  }

  test("Test copyFrom for AbstractFile integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val temp = new IndexOutputFile(localFile)
    assert(temp.isDirectory())
    assert(temp.isAbstractFile())

    // CopyFrom
    val expectedContent = "test output"
    val src = localFile.getChild("srcFile")
    src.getByteSink.write(expectedContent.getBytes())
    val dst = temp.createFile("dstFile")
    dst.copyFrom(src.getByteSource.openStream())
    assert(localFile.getChild("dstFile").getByteSource.read() === src.getByteSource.read())
  }

  test("Test createDirectory for ResourceId integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val tmpResource = LocalResources.fromFile(tempDir, /* isDirectory */ true)

    val temp = new IndexOutputFile(tmpResource)
    assert(temp.isDirectory())
    assert(!temp.isAbstractFile())

    // Test creating a directory
    val inside_test = temp.createDirectory("inside_test")
    assert(inside_test.isDirectory())
    assert(localFile.getChild("inside_test").exists())
    assert(localFile.getChild("inside_test").isDirectory)
  }

  test("Test createFile and outputStream for ResourceId integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val tmpResource = LocalResources.fromFile(tempDir, /* isDirectory */ true)

    val temp = new IndexOutputFile(tmpResource)
    assert(temp.isDirectory())
    assert(!temp.isAbstractFile())

    // Test creating a file
    val test_file = temp.createFile("test_file")
    assert(!test_file.isDirectory())

    // Test OutputStream
    val expectedContent = "test output"
    val out = test_file.getOutputStream()
    out.write(expectedContent.getBytes())
    out.close()
    assert(localFile.getChild("test_file").exists())
    val actualContent = localFile.getChild("test_file").getByteSource.read()
    assert(actualContent === expectedContent.getBytes())

    // Test Path
    assert(temp.getPath() == (tempDir.getPath + "/")) // Directory
    assert(test_file.getPath() == localFile.getChild("test_file").getPath) // File
  }

  test("Test SUCCESS file for ResourceId integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val tmpResource = LocalResources.fromFile(tempDir, /* isDirectory */ true)

    val temp = new IndexOutputFile(tmpResource)
    assert(temp.isDirectory())
    assert(!temp.isAbstractFile())

    // Test Success file creation
    temp.createSuccessFile()
    assert(localFile.getChild("_SUCCESS").exists())
  }

  test("Test copyFrom for ResourceId integration") {
    val tempDir = Files.createTempDirectory("test").toFile
    tempDir.deleteOnExit()
    val localFile = new LocalFile(tempDir)
    val tmpResource = LocalResources.fromFile(tempDir, /* isDirectory */ true)

    val temp = new IndexOutputFile(tmpResource)
    assert(temp.isDirectory())
    assert(!temp.isAbstractFile())

    // CopyFrom
    val expectedContent = "test output"
    val src = localFile.getChild("srcFile")
    src.getByteSink.write(expectedContent.getBytes())
    val dst = temp.createFile("dstFile")
    dst.copyFrom(src.getByteSource.openStream())
    assert(localFile.getChild("dstFile").getByteSource.read() === src.getByteSource.read())
  }
}
