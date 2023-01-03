package com.twitter.ann.featurestore

import com.twitter.ml.api.embedding.Embedding
import com.twitter.ml.api.embedding.EmbeddingSerDe
import com.twitter.ml.featurestore.lib.data.PredictionRecord
import com.twitter.ml.featurestore.lib.entity.EntityWithId
import com.twitter.ml.featurestore.lib.RawFloatTensor
import com.twitter.ml.featurestore.lib.UserId
import com.twitter.ml.featurestore.lib.feature.BoundFeature
import com.twitter.ml.featurestore.lib.online.FeatureStoreClient
import com.twitter.ml.featurestore.lib.online.FeatureStoreRequest
import com.twitter.stitch.Stitch
import com.twitter.util.Await
import com.twitter.util.Future
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar

class FeatureStoreEmbeddingProducerSpec extends AnyFunSuite with MockitoSugar {
  test("produceEmbedding") {
    val boundFeature = mock[BoundFeature[UserId, RawFloatTensor]]
    val featureStoreClient = mock[FeatureStoreClient]
    val entityWithId = mock[EntityWithId[UserId]]
    val predictionRecord = mock[PredictionRecord]
    val embedding = Embedding(Array(1.0f))
    val thriftEmbedding = EmbeddingSerDe.floatEmbeddingSerDe.toThrift(embedding)
    val tensor = RawFloatTensor(thriftEmbedding.tensor.get)

    when(predictionRecord.getFeatureValue(boundFeature)).thenReturn(Some(tensor))
    when(featureStoreClient.apply(any[FeatureStoreRequest]))
      .thenReturn(Future.value(predictionRecord))

    val producer = new FeatureStoreEmbeddingProducer(
      boundFeature,
      featureStoreClient
    )

    val result = Await.result(Stitch.run(producer.produceEmbedding(entityWithId))).get
    assert(result == embedding)
  }
}
