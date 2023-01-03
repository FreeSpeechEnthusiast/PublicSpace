package com.twitter.ann.hnsw;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HnswIndexTest {
  private static final int TEST_EF_CONSTRUCTION = 100;
  private static final int TEST_MAX_M = 4;
  private static final int TEST_EXPECTED_ELEMENTS = 100;

  private static final int TEST_QUERY = 6;
  private static final int TEST_NUM_OF_NEIGHBORS = 2;
  private static final int TEST_EF = 100;

  private final DistanceFunction<Integer, Integer> distanceFunction = (a, b) -> Math.abs(a - b);

  private HnswIndex<Integer, Integer> index = null;
  private HnswIndex.RandomProvider randomProvider = mock(HnswIndex.RandomProvider.class);

  // one thread that used to grab lock on items
  private ExecutorService ex = Executors.newFixedThreadPool(1);

  @Before
  public void setUp() throws Exception {
    when(randomProvider.get()).thenReturn(new Random(123456L));
    index = new HnswIndex<>(
        distanceFunction,
        distanceFunction,
        TEST_EF_CONSTRUCTION,
        TEST_MAX_M,
        TEST_EXPECTED_ELEMENTS,
        randomProvider
    );
  }

  @After
  public void tearDown() throws Exception {
    index = null;
    ex.shutdown();
  }

  @Test
  public void testBasicInsertAndQuery() throws Exception {
    index.insert(1);
    index.insert(5);
    index.insert(8);
    index.insert(15);
    List<DistancedItem<Integer>> results = index.searchKnn(
        TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF);
    verifyResults(results, Sets.newHashSet(5, 8), Sets.newHashSet(1.0f, 2.0f));
  }

  @Test
  public void testInsertWithVeryHighLevel() throws Exception {
    Random random = mock(Random.class);
    when(random.nextDouble()).thenReturn(0.2, 0.0000001, 0.1, 0.1);
    when(randomProvider.get()).thenReturn(random);
    // Control the level generated:
    // 0.2 -> 1
    // 0.0000001 -> 11
    index.insert(1);
    index.insert(5);
    index.insert(8);
    index.insert(11);
    assertIndexEntryPointAndMaxLevel(11, Optional.of(5));
    List<DistancedItem<Integer>> results = index.searchKnn(
        TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF);
    verifyResults(results, Sets.newHashSet(5, 8), Sets.newHashSet(1.0f, 2.0f));
  }

  @Test
  public void testAddLayer() throws Exception {
    Random mockRandom = mock(Random.class);
    when(mockRandom.nextDouble()).thenReturn(0.05, 0.2, 0.01);
    when(randomProvider.get()).thenReturn(mockRandom);
    // Control the level generated:
    // 0.05 -> 2
    // 0.2 -> 1
    // 0.01 -> 3
    assertIndexEntryPointAndMaxLevel(-1, Optional.empty());
    index.insert(1);
    assertIndexEntryPointAndMaxLevel(2, Optional.of(1));
    // random level for this insertion is 1, less than max level (2)
    // entry point and max level should not change
    index.insert(3);
    assertIndexEntryPointAndMaxLevel(2, Optional.of(1));
    // random level for this insertion is 3, greater than max level (2)
    // entry point and max level should change
    index.insert(5);
    assertIndexEntryPointAndMaxLevel(3, Optional.of(5));
  }

  // Case when a insert is happening and some nodes required for insert are held by write lock.
  // Simulation: in this scenario insertion should not happen (Waiting state).
  // Successful insertion happens only after write lock is removed.
  // Result: There will not be any connections made in the graph with the node being inserted.
  @Test
  public void testInsertWhileItemIsLockedWithWriteLock() throws Exception {
    index.insert(1);
    index.insert(5);
    index.insert(8);
    index.insert(11);
    grabLockWithThread(8, true); // Write lock
    Thread.sleep(1);
    assertTrue(!index.getLocks().get(8).writeLock().tryLock()); // Cannot take write lock
    assertTrue(!index.getLocks().get(8).readLock().tryLock()); // Cannot take read lock

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<?> future = executorService.submit((Callable<Void>) () -> {
      index.insert(9);
      return null;
    });
    try {
      // insert 9 should not happen here as another thread is holding lock on item 8
      future.get(1000, TimeUnit.MILLISECONDS);
      fail("Insert should not happen");
    } catch (TimeoutException ignored) {
    }
    // should not find item 9 in the graph, no connections will be made in any layer.
    assertFalse(index.getGraph().containsKey(HnswNode.from(0, 9)));

    // After unlocking, insertion should go through
    unlockLockWithThread(8, true);
    Thread.sleep(2);

    // should find item 9 in the graph
    assertTrue(index.getGraph().containsKey(HnswNode.from(0, 9)));

    executorService.shutdown();
  }

  // Case when a insert is happening and some nodes required for insert are held by read lock.
  // Simulation:In this scenario partial insertion should happen,
  // complete insertion should fail (Waiting state)
  // Result: Node held by write lock should not make any directed connection with new node.
  // i.e NO write_lock_node -> new_node
  // New node should make connection with all neighbors.
  @Test
  public void testInsertWhileItemIsLockedWithReadLock() throws Exception {
    index.insert(1);
    index.insert(5);
    index.insert(8);
    grabLockWithThread(8, false); // Read lock
    Thread.sleep(1);

    assertTrue(!index.getLocks().get(8).writeLock().tryLock()); // Cannot take write lock
    assertTrue(index.getLocks().get(8).readLock().tryLock()); // Can take read lock

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<?> future = executorService.submit((Callable<Void>) () -> {
      index.insert(9);
      return null;
    });
    try {
      // insert 9 should not happen here as another thread is holding lock on item 8
      future.get(1000, TimeUnit.MILLISECONDS);
      fail("Insert should not happen");
    } catch (TimeoutException ignored) {
    }

    final HnswNode<Integer> node = HnswNode.from(0, 9);
    // On base layer new node should be present
    assertTrue(index.getGraph().containsKey(node));
    // New node should make one way connection to all neighbors
    List<Integer> neighbors = index.getGraph().get(node);
    assertTrue(neighbors.size() == 3);
    assertTrue(neighbors.contains(1) && neighbors.contains(5) && neighbors.contains(8));

    // Nodes except the locked node should make one way connection with new node.
    assertTrue(index.getGraph().get(HnswNode.from(0, 1)).contains(9));
    assertTrue(index.getGraph().get(HnswNode.from(0, 5)).contains(9));

    // Locked node should not make one way connection with new node.
    assertFalse(index.getGraph().get(HnswNode.from(0, 8)).contains(9));

    executorService.shutdown();
  }

  @Test
  public void testSearchWhileItemLockedWithWriteLock() throws Exception {
    index.insert(1);
    index.insert(5);
    index.insert(8);
    index.insert(11);
    // Take write lock, as if insertion is happening or elements being connected mutually.
    grabLockWithThread(8, true);
    Thread.sleep(1);

    assertTrue(!index.getLocks().get(8).writeLock().tryLock()); // Cannot take write lock
    assertTrue(!index.getLocks().get(8).readLock().tryLock()); // Cannot take read lock

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<?> future =
        executorService.submit(() -> index.searchKnn(TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF));
    try {
      // insert 9 should not happen here as another thread is holding lock on item 8
      future.get(1000, TimeUnit.MILLISECONDS);
      fail("Query should not happen");
    } catch (TimeoutException ignored) {
    }

    // After unlocking the lock, query should work.
    unlockLockWithThread(8, true);
    Future<List<DistancedItem<Integer>>> future2 =
        executorService.submit(() ->
        index.searchKnn(TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF));
    List<DistancedItem<Integer>> results = future2.get();
    verifyResults(results, Sets.newHashSet(5, 8), Sets.newHashSet(1.0f, 2.0f));

    executorService.shutdown();
  }

  // Precheck failed exception if update being performed graph with 0 elements
  @Test(expected = IllegalStateException.class)
  public void testUpdateExceptionCase1() throws Exception {
    index.reInsert(1);
  }

  // Pre check failed exception if the element to be updated is not present in 0th layer
  @Test(expected = IllegalStateException.class)
  public void testUpdateExceptionCase2() throws Exception {
    index.insert(1);
    index.getGraph().put(HnswNode.from(1, 2), ImmutableList.of());
    index.reInsert(2);
  }

  // Test of update for basic sanity purpose. It is hard to test update in this small scale test.
  // Fully functional test with cosiderable number of high dimensional embeddings
  // indexed into the index, and then measuring the recall would be a better choice for it.
  @Test
  public void testBasicUpdate() throws Exception {
    final Map<Integer, Integer> embMap = new HashMap<>();
    final DistanceFunction<Integer, Integer> distFn =
        (a, b) -> Math.abs(embMap.get(a) - embMap.get(b));
    final HnswIndex<Integer, Integer> annIndex = new HnswIndex<>(
        distFn,
        distFn,
        TEST_EF_CONSTRUCTION,
        TEST_MAX_M,
        TEST_EXPECTED_ELEMENTS,
        randomProvider
    );

    embMap.put(1, 1);
    embMap.put(5, 5);
    embMap.put(8, 8);
    embMap.put(TEST_QUERY, TEST_QUERY);
    annIndex.insert(1);
    annIndex.insert(5);
    annIndex.insert(8);

    List<DistancedItem<Integer>> results = annIndex.searchKnn(
        TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF_CONSTRUCTION);
    verifyResults(results, Sets.newHashSet(5, 8), Sets.newHashSet(1.0f, 2.0f));

    // update the embedding of entity 5 to big number
    embMap.put(5, 1000);
    annIndex.reInsert(5);

    results = annIndex.searchKnn(
        TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF);
    verifyResults(results, Sets.newHashSet(8, 1), Sets.newHashSet(2.0f, 5.0f));
  }

  @Test
  public void testSearchWhileItemLockedWithReadLock() throws Exception {
    index.insert(1);
    index.insert(5);
    index.insert(8);
    index.insert(11);
    grabLockWithThread(8, false); // Take read lock
    Thread.sleep(1);

    assertTrue(!index.getLocks().get(8).writeLock().tryLock()); // Cannot take write lock
    assertTrue(index.getLocks().get(8).readLock().tryLock()); // Can take read lock

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<List<DistancedItem<Integer>>> future = executorService.submit(() ->
        index.searchKnn(TEST_QUERY, TEST_NUM_OF_NEIGHBORS, TEST_EF));
    List<DistancedItem<Integer>> results = future.get();
    verifyResults(results, Sets.newHashSet(5, 8), Sets.newHashSet(1.0f, 2.0f));
  }

  @Test
  public void testSelectNearestNeighboursByHeuristic() {
    DistancedItemQueue<Integer, Integer> queue = new DistancedItemQueue<>(
        7,
        Lists.newArrayList(1, 4, 5),
        false,
        distanceFunction
    );
    Set<Integer> res = Sets.newHashSet(index.selectNearestNeighboursByHeuristic(queue, 2));
    // we ask for 2 items, but since distance between 4 and 5 is smaller than the distance
    // from 4 to 7, so it is left out in result set. And also distance between 1 and 5 is less than
    // distance between 1 and 7.
    assertEquals(Sets.newHashSet(5), res);
  }

  @Test(expected = IllegalDuplicateInsertException.class)
  public void testDuplicatedInsertionNotSupportWithOneItem() throws Exception {
    index.insert(1);
    index.insert(1);
  }

  @Test(expected = IllegalDuplicateInsertException.class)
  public void testDuplicatedInsertionNotSupport() throws Exception {
    index.insert(1);
    index.insert(5);
    index.insert(5);
  }

  private void assertIndexEntryPointAndMaxLevel(
      int expectedMaxLevel,
      Optional<Integer> expectedEntryPoint
  ) {
    HnswMeta<Integer> expectedMeta = new HnswMeta<>(expectedMaxLevel, expectedEntryPoint);
    assertEquals(expectedMeta, index.getGraphMeta().get());
  }

  private void verifyResults(
      List<DistancedItem<Integer>> results,
      Set<Integer> expectedResults,
      Set<Float> expectedDistance) {
    Set<Integer> resultSet =
        results.stream().map(DistancedItem::getItem).collect(Collectors.toSet());
    Set<Float> distancesSet =
        results.stream().map(DistancedItem::getDistance).collect(Collectors.toSet());
    assertEquals(expectedResults, resultSet);
    assertEquals(expectedDistance, distancesSet);
  }

  private void grabLockWithThread(Integer item, boolean takeWriteLock) {
    if (takeWriteLock) {
      ex.submit(() -> index.getLocks().get(item).writeLock().lock());
    } else {
      ex.submit(() -> index.getLocks().get(item).readLock().lock());
    }
  }

  private void unlockLockWithThread(Integer item, boolean writeLock) {
    if (writeLock) {
      ex.submit(() -> index.getLocks().get(item).writeLock().unlock());
    } else {
      ex.submit(() -> index.getLocks().get(item).readLock().unlock());
    }
  }
}
