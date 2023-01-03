package com.twitter.ann.file_store

import com.google.common.io.ByteSource
import com.twitter.ann.common.thriftscala.FileBasedIndexIdStore
import com.twitter.bijection.Bufferable
import com.twitter.mediaservices.commons.codec.{ArrayByteBufferCodec, ThriftByteBufferCodec}
import com.twitter.search.common.file.AbstractFile
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

@RunWith(classOf[JUnitRunner])
class ReadableIndexIdFileStoreSpec extends FunSuite with MockitoSugar {
  val indexIdMap = Map(1L -> "data1", 2L -> "data2")
  val codec = new ThriftByteBufferCodec(FileBasedIndexIdStore)
  val thriftBytes = codec.encode(
    FileBasedIndexIdStore(
      Some(
        indexIdMap
          .mapValues(value =>
            ArrayByteBufferCodec.encode(Bufferable.injectionOf[String].apply(value)))
      )
    )
  )

  test(
    "ReadableIndexIdFileStore creates readable store and deserialize index id mapping from thrift serialized file") {
    val mockAbstractFile = mock[AbstractFile]
    val mockByteSource = mock[ByteSource]
    when(mockAbstractFile.getByteSource()).thenReturn(mockByteSource)
    when(mockByteSource.read()).thenReturn(ArrayByteBufferCodec.decode(thriftBytes))

    val readableIndexIdFileStore =
      ReadableIndexIdFileStore[String](mockAbstractFile, Bufferable.injectionOf[String])

    assert(Await.result(readableIndexIdFileStore.get(1L)).get == "data1")
    assert(Await.result(readableIndexIdFileStore.get(2L)).get == "data2")
  }
}
