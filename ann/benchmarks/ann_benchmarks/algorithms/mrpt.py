import mrpt
from ann_benchmarks.algorithms.base import BaseANN


class MRPT(BaseANN):
  def __init__(self, n_trees, depth):
    self._n_trees = n_trees
    self._depth = depth
    self._votes_required = None

  def fit(self, X):
    self._index = mrpt.MRPTIndex(X, depth=self._depth, n_trees=self._n_trees)
    self._index.build()

  def set_query_arguments(self, votes_required):
    self._votes_required = votes_required

  def query(self, v, n):
    return self._index.ann(v, n, votes_required=self._votes_required)

  def __str__(self):
    return 'MRPT(n_trees=%d, depth=%d, votes_required=%d)' % (
      self._n_trees,
      self._depth,
      self._votes_required,
    )
