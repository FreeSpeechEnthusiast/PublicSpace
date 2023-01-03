package com.twitter.ann.file_store

import com.twitter.ann.common.IndexOutputFile
import com.twitter.ann.common.thriftscala.FileBasedIndexIdStore
import com.twitter.bijection.Bufferable
import com.twitter.mediaservices.commons.codec.ArrayByteBufferCodec
import com.twitter.mediaservices.commons.codec.ThriftByteBufferCodec
import com.twitter.search.common.file.LocalFile
import com.twitter.util.Await
import java.io.File
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class WritableIndexIdFileStoreSpec extends FunSuite with MockitoSugar {
  test(
    "WritableIndexIdFileStoreSpec creates writable store and serialize index id mapping to thrift") {
    val store = WritableIndexIdFileStore[String](Bufferable.injectionOf[String])
    store.put((1L, Some("data1")))
    store.put((2L, Some("data2")))
    val expectedThrift = FileBasedIndexIdStore(
      Some(
        Map(
          1L -> ArrayByteBufferCodec.encode(Bufferable.injectionOf[String].apply("data1")),
          2L -> ArrayByteBufferCodec.encode(Bufferable.injectionOf[String].apply("data2"))
        )
      )
    )

    val temp = new LocalFile(File.createTempFile("test_file", null))
    store.save(new IndexOutputFile(temp))

    val codec = new ThriftByteBufferCodec(FileBasedIndexIdStore)
    val thrift = codec.decode(ArrayByteBufferCodec.encode(temp.getByteSource.read()))

    assert(thrift == expectedThrift)
    temp.delete()
  }

  test("WritableIndexIdFileStore can convert to readable store") {
    val writableStore = WritableIndexIdFileStore[String](Bufferable.injectionOf[String])
    writableStore.put((1L, Some("data1")))
    assert(Await.result(writableStore.get(1L)).get == "data1")
  }
}
