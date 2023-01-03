package com.twitter.ann.service.query_server.common

import com.twitter.search.common.file.AbstractFile
import java.{lang, util}
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.scalatestplus.mockito.MockitoSugar

class QueryServerUtilSpec extends FunSuite with MockitoSugar {
  test("Validation of directory size") {
    val maxSize = 20
    val minSize = 10

    val mockDir = mock[AbstractFile]
    val mockFile1 = mock[AbstractFile]
    val mockFile2 = mock[AbstractFile]

    val list = new util.ArrayList[AbstractFile]()
    list.add(mockFile1)
    list.add(mockFile2)
    val iterable = new lang.Iterable[AbstractFile] {
      override def iterator(): util.Iterator[AbstractFile] = list.iterator()
    }
    when(mockDir.listFiles(true)).thenReturn(iterable)

    // Invalid. When size of file is less than minSize
    when(mockFile1.getSizeInBytes).thenReturn(4)
    when(mockFile2.getSizeInBytes).thenReturn(5)
    assert(!QueryServerUtil.isValidIndexDirSize(mockDir, minSize, maxSize))

    // Invalid. When size of file is greater than maxSize
    when(mockFile1.getSizeInBytes).thenReturn(5)
    when(mockFile2.getSizeInBytes).thenReturn(16)
    assert(!QueryServerUtil.isValidIndexDirSize(mockDir, minSize, maxSize))

    // Invalid. When size of file is equal to minSize
    when(mockFile1.getSizeInBytes).thenReturn(minSize - 1)
    when(mockFile2.getSizeInBytes).thenReturn(1)
    assert(!QueryServerUtil.isValidIndexDirSize(mockDir, minSize, maxSize))

    // Invalid. When size of file is equal to maxSize
    when(mockFile1.getSizeInBytes).thenReturn(maxSize - 1)
    when(mockFile2.getSizeInBytes).thenReturn(1)
    assert(!QueryServerUtil.isValidIndexDirSize(mockDir, minSize, maxSize))

    // Valid. When size of file is between min and max size.
    when(mockFile1.getSizeInBytes).thenReturn(6)
    when(mockFile2.getSizeInBytes).thenReturn(9)
    assert(QueryServerUtil.isValidIndexDirSize(mockDir, minSize, maxSize))
  }
}
