package com.twitter.ann.faiss.selftest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import scala.Int;

import com.twitter.ann.faiss.FloatVector;
import com.twitter.ann.faiss.Index;
import com.twitter.ann.faiss.IndexIDMap;
import com.twitter.ann.faiss.IndexShards;
import com.twitter.ann.faiss.LongVector;
import com.twitter.ann.faiss.MetricType;
import com.twitter.ann.faiss.ParameterSpace;
import com.twitter.ann.faiss.swigfaiss;

public final class SelfTest {
  /**
   * This program verify that native library is loadable and is able to perform basic tasks.
   */
  public static void main(String[] args) {
    String arch = System.getProperty("os.arch");
    System.out.println("Your architecture is " + arch);
    if ("x86_64".equals(arch)) {
      System.out.println("This binary is expected to fail under rosetta2 emulator");
    }
    testIndexSharding();
    testParameterSpace("OPQ50,IVF2048_HNSW32,PQ50", "nprobe=64,quantizer_efSearch=512,ht=256");
    testParameterSpace("OPQ50,IVF2048(IVF512,PQ50x4fs,RFlat),PQ50",
        "nprobe=256,quantizer_k_factor_rf=8,quantizer_nprobe=64,ht=240");
    testLargeIVFPQ();
    System.out.println("It's alive (selftest was successfull)");
  }

  private SelfTest() {
  }

  static FloatVector makeRandomVector(long size) {
    FloatVector xb = new FloatVector();
    xb.reserve(size);

    Random rand = new Random();

    for (int i = 0; i < size; i++) {
        float val = rand.nextFloat();
        xb.push_back(val);
    }

    return xb;
  }

  static Index makeRandomIndex(long d, long size, FloatVector values, long idRangeStart) {
    LongVector indices = new LongVector();
    indices.reserve(size);
    for (int i = 0; i < size; i++) {
      indices.push_back(idRangeStart + i);
    }

    Index rawIndex = swigfaiss.index_factory((int) d, "IVF2048,PQ8x4fs", MetricType.METRIC_L2);
    IndexIDMap index = new IndexIDMap(rawIndex);
    index.train(size, values.data());
    index.add_with_ids(size, values.data(), indices);

    return index;
  }

  static void testIndexSharding() {
    long d = 200;
    long vectorSize = 8192;

    IndexShards shards = new IndexShards((int) d, false, false);
    List<FloatVector> values = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      values.add(makeRandomVector(vectorSize * d));
      shards.add_shard(makeRandomIndex(d, vectorSize, values.get(i), vectorSize * i));
    }

    FloatVector query = new FloatVector();
    query.reserve(5 * d);
    for (int i = 0; i < 5; i++) {
      FloatVector shard = values.get(i);
      for (int j = 0; j < d; j++) {
        query.push_back(shard.at(j));
      }
    }

    long k = 5;
    LongVector outIndices = new LongVector();
    outIndices.resize(k * 5);
    FloatVector distances = new FloatVector();
    distances.resize(k * 5);

    Index index = swigfaiss.upcast_IndexShards(shards);
    index.search(5, query.data(), k, distances.data(), outIndices);
    System.out.printf("Distances:\n%s\n", show(distances, 5, 5));
    System.out.printf("indices:\n%s\n", show(outIndices, 5, 5));
  }

  static void testLargeIVFPQ() {
    long d = 200; // dimension
    long k = 4;
    int m = 8;
    long nb = (((long) Int.MaxValue() + d) / d) + 1; // smallest size which won't fit into Int
    long trainingSize = 65535;

    FloatVector xb = makeRandomVector(nb * d);

    LongVector indices = new LongVector();
    indices.reserve(nb);
    for (int i = 0; i < nb; i++) {
      indices.push_back(-i);
    }

    Index rawIndex = swigfaiss.index_factory((int) d, "IVF2048,PQ8x4fs", MetricType.METRIC_L2);
    IndexIDMap index = new IndexIDMap(rawIndex);
    System.out.printf("is_trained = %b\n", index.getIs_trained());
    index.train(trainingSize, xb.data());

    System.out.printf("Finish training");
    index.add_with_ids(nb, xb.data(), indices);
    System.out.printf("is_trained = %b\n", index.getIs_trained());
    System.out.printf("ntotal = %d\n", index.getNtotal());

    LongVector outIndices = new LongVector();
    outIndices.resize(k * 5);
    FloatVector distances = new FloatVector();
    distances.resize(k * 5);

    System.out.printf("search 5 first vector of xb");
    index.search(5L, xb.data(), 4L, distances.data(), outIndices);
    System.out.printf("Distances:\n%s\n", show(distances, 5, 4));
    System.out.printf("indices:\n%s\n", show(outIndices, 5, 4));
  }

  static void testParameterSpace(String factoryString, String parameterString) {
    long d = 200; // dimension
    long k = 4;
    int m = 8;
    long nb = 65535;
    long trainingSize = 8192;

    FloatVector xb = makeRandomVector(nb * d);

    LongVector indices = new LongVector();
    indices.reserve(nb);
    for (int i = 0; i < nb; i++) {
      indices.push_back(-i);
    }

    Index rawIndex = swigfaiss.index_factory((int) d, factoryString, MetricType.METRIC_L2);
    IndexIDMap index = new IndexIDMap(rawIndex);
    System.out.printf("is_trained = %b\n", index.getIs_trained());
    index.train(trainingSize, xb.data());

    System.out.printf("Finish training");
    index.add_with_ids(nb, xb.data(), indices);
    System.out.printf("is_trained = %b\n", index.getIs_trained());
    System.out.printf("ntotal = %d\n", index.getNtotal());

    LongVector outIndices = new LongVector();
    outIndices.resize(k * 5);
    FloatVector distances = new FloatVector();
    distances.resize(k * 5);

    System.out.printf("search 5 first vector of xb");
    new ParameterSpace().set_index_parameters(index, parameterString);
    index.search(5L, xb.data(), 4L, distances.data(), outIndices);
    System.out.printf("Distances:\n%s\n", show(distances, 5, 4));
    System.out.printf("indices:\n%s\n", show(outIndices, 5, 4));
  }

  static String show(LongVector a, int rows, int cols) {
    StringBuilder sb = new StringBuilder();
    for (long i = 0; i < rows; i++) {
      sb.append(i).append('\t').append('|');
      for (long j = 0; j < cols; j++) {
        sb.append(String.format("%5d ", a.at(i * cols + j)));
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  static String show(FloatVector a, long rows, long cols) {
    StringBuilder sb = new StringBuilder();
    for (long i = 0; i < rows; i++) {
      sb.append(i).append('\t').append('|');
      for (long j = 0; j < cols; j++) {
        sb.append(String.format("%7g ", a.at(i * cols + j)));
      }
      sb.append("\n");
    }
    return sb.toString();
  }
}
