package com.twitter.ann.scalding.offline

import com.twitter.scalding._
import com.twitter.scalding.source.TypedText
import java.util.TimeZone
import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar

class KnnTruthSetGeneratorTest extends FunSuite with MockitoSugar with Matchers {

  // Set the implicits
  implicit val tz: TimeZone = TimeZone.getDefault
  implicit val dp: DateParser = DateParser.default
  implicit val dateRange: DateRange =
    DateRange(RichDate("2017-10-03T00"), RichDate("2017-10-04T05"))

  test("with query arguments") {
    val embeddingFormat = "tab"
    val indexEmbeddings = List(
      "0" -> "10\t0.0\t0.0\t5.0",
      "1" -> "20\t9.0\t0.0\t0.0"
    )
    val queryEmbeddings = List(
      "0" -> "30\t9.0\t0.0\t5.0"
    )

    JobTest(ExecutionTestJob(KnnTruthSetGenerator.job)(_))
      .arg("index.embedding_path", "fake_index_embeddings")
      .arg("index.embedding_format", embeddingFormat)
      .arg("query.embedding_path", "fake_query_embeddings")
      .arg("query.embedding_format", embeddingFormat)
      .arg("query_set_output.embedding_path", "fake_query_set_output")
      .arg("query_set_output.embedding_format", embeddingFormat)
      .arg("index_set_output.embedding_path", "fake_index_set_output")
      .arg("index_set_output.embedding_format", embeddingFormat)
      .arg("truth_set_output_path", "fake_truth_set_output")
      .arg("index_entity_kind", "tweet")
      .arg("query_entity_kind", "user")
      .arg("query_sample_percent", "100")
      .arg("index_sample_percent", "100")
      .arg("metric", "L2")
      .arg("reducers", "1")
      .arg("mappers", "1")
      .arg("neighbors", "1")
      .source(TextLine("fake_index_embeddings"), indexEmbeddings)
      .source(TextLine("fake_query_embeddings"), queryEmbeddings)
      .typedSink(TypedTsv[String]("fake_query_set_output")) { query =>
        assert(query.size == 1)
        assert(query.head == "30\t9.0\t0.0\t5.0")
      }
      .typedSink(TypedTsv[String]("fake_index_set_output")) { index =>
        assert(index.size == 2)
        // everything in indexEmbeddings should be included
        assert(index(0) == "10\t0.0\t0.0\t5.0")
        assert(index(1) == "20\t9.0\t0.0\t0.0")
      }
      .typedSink(TypedText.tsv[String]("fake_truth_set_output")) { truth =>
        assert(truth.size == 1)
        assert(truth.head == "30\t20:5.0")
      }
      .run
      .finish()
  }
}

case class ExecutionTestJob[T](exec: Execution[T])(args: Args) extends ExecutionJob[T](args) {
  override def execution: Execution[T] = exec
}
