import numpy
import sklearn.neighbors
from ann_benchmarks.distance import metrics as pd
from ann_benchmarks.algorithms.base import BaseANN


class BruteForce(BaseANN):
  def __init__(self, metric):
    if metric not in ('angular', 'euclidean', 'hamming'):
      raise NotImplementedError("BruteForce doesn't support metric %s" % metric)
    self._metric = metric
    self.name = 'BruteForce()'

  def fit(self, X):
    metric = {'angular': 'cosine', 'euclidean': 'l2', 'hamming': 'hamming'}[self._metric]
    self._nbrs = sklearn.neighbors.NearestNeighbors(algorithm='brute', metric=metric)
    self._nbrs.fit(X)

  def query(self, v, n):
    return list(self._nbrs.kneighbors([v], return_distance=False, n_neighbors=n)[0])

  def query_with_distances(self, v, n):
    (distances, positions) = self._nbrs.kneighbors([v], return_distance=True, n_neighbors=n)
    return zip(list(positions[0]), list(distances[0]))


class BruteForceBLAS(BaseANN):
  """kNN search that uses a linear scan = brute force."""

  def __init__(self, metric, precision=numpy.float32):
    if metric not in ('angular', 'euclidean', 'hamming', 'jaccard'):
      raise NotImplementedError("BruteForceBLAS doesn't support metric %s" % metric)
    elif metric == 'hamming' and precision != numpy.bool:
      raise NotImplementedError(
        "BruteForceBLAS doesn't support precision %s with Hamming distances" % precision
      )
    self._metric = metric
    self._precision = precision
    self.name = 'BruteForceBLAS()'

  def fit(self, X):
    """Initialize the search index."""
    if self._metric == 'angular':
      lens = (X ** 2).sum(-1)  # precompute (squared) length of each vector
      X /= numpy.sqrt(lens)[..., numpy.newaxis]  # normalize index vectors to unit length
      self.index = numpy.ascontiguousarray(X, dtype=self._precision)
    elif self._metric == 'euclidean':
      lens = (X ** 2).sum(-1)  # precompute (squared) length of each vector
      self.index = numpy.ascontiguousarray(X, dtype=self._precision)
      self.lengths = numpy.ascontiguousarray(lens, dtype=self._precision)
    elif self._metric == 'hamming':
      self.index = numpy.ascontiguousarray(map(numpy.packbits, X), dtype=numpy.uint8)
    elif self._metric == 'jaccard':
      self.index = X
    else:
      assert False, "invalid metric"  # shouldn't get past the constructor!

  def query(self, v, n):
    return [index for index, _ in self.query_with_distances(v, n)]

  popcount = []
  for i in range(256):
    popcount.append(bin(i).count("1"))

  def query_with_distances(self, v, n):
    """Find indices of `n` most similar vectors from the index to query vector `v`."""
    if self._metric == 'hamming':
      v = numpy.packbits(v)

    if self._metric != 'jaccard':
      # use same precision for query as for index
      v = numpy.ascontiguousarray(v, dtype=self.index.dtype)

    # HACK we ignore query length as that's a constant not affecting the final ordering
    if self._metric == 'angular':
      # argmax_a cossim(a, b) = argmax_a dot(a, b) / |a||b| = argmin_a -dot(a, b)
      dists = -numpy.dot(self.index, v)
    elif self._metric == 'euclidean':
      # argmin_a (a - b)^2 = argmin_a a^2 - 2ab + b^2 = argmin_a a^2 - 2ab
      dists = self.lengths - 2 * numpy.dot(self.index, v)
    elif self._metric == 'hamming':
      diff = numpy.bitwise_xor(v, self.index)
      pc = BruteForceBLAS.popcount
      den = float(len(v) * 8)
      dists = [sum([pc[part] for part in point]) / den for point in diff]
    elif self._metric == 'jaccard':
      dists = [pd[self._metric]['distance'](v, e) for e in self.index]
    else:
      assert False, "invalid metric"  # shouldn't get past the constructor!
    nearest_indices = numpy.argpartition(dists, n)[
      :n
    ]  # partition-sort by distance, get `n` closest
    indices = [idx for idx in nearest_indices if pd[self._metric]["distance_valid"](dists[idx])]

    def fix(index):
      ep = self.index[index]
      ev = v
      if self._metric == "hamming":
        ep = numpy.unpackbits(ep)
        ev = numpy.unpackbits(ev)
      return (index, pd[self._metric]['distance'](ep, ev))

    return map(fix, indices)
