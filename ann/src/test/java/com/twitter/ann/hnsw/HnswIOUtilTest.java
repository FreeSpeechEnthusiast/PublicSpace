package com.twitter.ann.hnsw;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import com.twitter.ann.common.thriftjava.HnswInternalIndexMetadata;
import com.twitter.bijection.Injection$;
import com.twitter.mediaservices.commons.codec.ArrayByteBufferCodec;
import com.twitter.search.common.file.AbstractFile;
import com.twitter.search.common.file.LocalFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HnswIOUtilTest {
  @Test
  public void testSerDeGraphEntries() throws Exception {
    final Map<HnswNode<String>, ImmutableList<String>> graph = new ConcurrentHashMap<>();
    graph.put(HnswNode.from(1, "1"), ImmutableList.of("1"));
    graph.put(HnswNode.from(2, "2"), ImmutableList.of("2"));
    graph.put(HnswNode.from(3, "3"), ImmutableList.of());
    final AbstractFile temp = createTempFile();

    final int nodes = HnswIndexIOUtil
        .saveHnswGraphEntries(graph, temp.getByteSink().openStream(), Injection$.MODULE$.utf8());
    assertEquals(nodes, 3);

    final Map<HnswNode<String>, ImmutableList<String>> deserializedGraph =
        HnswIndexIOUtil.loadHnswGraph(temp, Injection$.MODULE$.utf8(), nodes);

    assertEquals(graph.entrySet(), deserializedGraph.entrySet());
  }

  @Test
  public void testSerDeEmptyGraphEntries() throws Exception {
    final Map<HnswNode<String>, ImmutableList<String>> graph = ImmutableMap.of();
    final AbstractFile temp = createTempFile();
    final int nodes = HnswIndexIOUtil
        .saveHnswGraphEntries(graph, temp.getByteSink().openStream(), Injection$.MODULE$.utf8());
    assertEquals(nodes, 0);

    final Map<HnswNode<String>, ImmutableList<String>> deserializedGraph =
        HnswIndexIOUtil.loadHnswGraph(temp, Injection$.MODULE$.utf8(), nodes);

    assertTrue(deserializedGraph.isEmpty());
  }

  @Test
  public void testSerDeGraphMetadataWithEntryPoint() throws Exception {
    final int maxLevel = 1;
    final String entryPoint = "entry";
    final int maxM = 16;
    final int efConstruction = 200;
    final int numElements = 2;
    final HnswMeta<String> meta = new HnswMeta<>(maxLevel, Optional.of(entryPoint));
    final AbstractFile temp = createTempFile();

    HnswIndexIOUtil.saveMetadata(meta, efConstruction, maxM, numElements,
        Injection$.MODULE$.utf8(), temp.getByteSink().openStream());
    final HnswInternalIndexMetadata indexMetadata = HnswIndexIOUtil.loadMetadata(temp);
    assertEquals(indexMetadata.maxLevel, maxLevel);
    assertEquals(Injection$.MODULE$.utf8().invert(ArrayByteBufferCodec
        .decode(indexMetadata.entryPoint)).get(), entryPoint);
    assertEquals(indexMetadata.efConstruction, efConstruction);
    assertEquals(indexMetadata.maxM, maxM);
    assertEquals(indexMetadata.numElements, numElements);
  }

  @Test
  public void testSerDeGraphMetadataWithoutEntryPoint() throws Exception {
    final int maxLevel = 1;
    final int maxM = 16;
    final int efConstruction = 200;
    final int numElements = 2;
    final HnswMeta<String> meta = new HnswMeta<>(maxLevel, Optional.empty());

    final AbstractFile temp = createTempFile();
    HnswIndexIOUtil.saveMetadata(meta, efConstruction, maxM,
        numElements, Injection$.MODULE$.utf8(), temp.getByteSink().openStream());
    final HnswInternalIndexMetadata indexMetadata = HnswIndexIOUtil.loadMetadata(temp);

    assertEquals(indexMetadata.maxLevel, maxLevel);
    assertNull(indexMetadata.entryPoint);
    assertEquals(indexMetadata.efConstruction, efConstruction);
    assertEquals(indexMetadata.maxM, maxM);
    assertEquals(indexMetadata.numElements, numElements);
  }

  private AbstractFile createTempFile() throws IOException {
    final File tempFile = File.createTempFile("test", null);
    tempFile.deleteOnExit();
    return new LocalFile(tempFile);
  }
}
