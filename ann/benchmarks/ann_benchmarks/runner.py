import argparse
import datetime
import json
import multiprocessing.pool
import numpy
import os
import requests
import sys
import time
import logging

from ann_benchmarks.datasets import get_dataset, DATASETS
from ann_benchmarks.algorithms.definitions import Definition, instantiate_algorithm
from ann_benchmarks.distance import metrics
from ann_benchmarks.results import store_results

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


def run_individual_query(algo, X_train, X_test, distance, count, run_count, use_batch_query):
  best_search_time = float('inf')
  for i in range(run_count):
    logger.info('Run %d/%d...' % (i + 1, run_count))
    n_items_processed = [0]  # a bit dumb but can't be a scalar since of Python's scoping rules

    def single_query(v):
      start = time.time()
      candidates = algo.query(v, count)
      total = time.time() - start
      candidates = [
        (int(idx), float(metrics[distance]['distance'](v, X_train[idx]))) for idx in candidates
      ]
      n_items_processed[0] += 1
      if n_items_processed[0] % 1000 == 0:
        logger.info('Processed %d/%d queries...' % (n_items_processed[0], X_test.shape[0]))
      if len(candidates) > count:
        logger.info(
          'warning: algorithm %s returned %d results, but count is only %d)'
          % (algo, len(candidates), count)
        )
      return (total, candidates)

    def batch_query(X):
      start = time.time()
      result = algo.batch_query(X, count)
      total = time.time() - start
      candidates = [
        [
          (int(idx), float(metrics[distance]['distance'](v, X_train[idx])))
          for idx in single_results
        ]
        for v, single_results in zip(X, results)
      ]
      return [(total / float(len(X)), v) for v in candidates]

    if use_batch_query:
      results = batch_query(X_test)
    else:
      results = [single_query(x) for x in X_test]

    total_time = sum(time for time, _ in results)
    total_candidates = sum(len(candidates) for _, candidates in results)
    search_time = total_time / len(X_test)
    avg_candidates = total_candidates / len(X_test)
    best_search_time = min(best_search_time, search_time)

  verbose = hasattr(algo, "query_verbose")
  attrs = {
    "batch_mode": use_batch_query,
    "best_search_time": best_search_time,
    "candidates": avg_candidates,
    "expect_extra": verbose,
    "name": str(algo),
    "run_count": run_count,
    "distance": distance,
    "count": int(count),
  }
  return (attrs, results)


def run(definition, dataset, count, run_count=3, use_batch_query=False):
  algo = instantiate_algorithm(definition)
  assert not definition.query_argument_groups or hasattr(
    algo, "set_query_arguments"
  ), """\
error: query argument groups have been specified for %s.%s(%s), but the \
algorithm instantiated from it does not implement the set_query_arguments \
function""" % (
    definition.module,
    definition.constructor,
    definition.arguments,
  )

  D = get_dataset(dataset)
  X_train = numpy.array(D['train'])
  X_test = numpy.array(D['test'])
  distance = D.attrs['distance']
  logger.info('got a train set of size (%d * %d)' % X_train.shape)
  logger.info('got %d queries' % len(X_test))

  try:
    t0 = time.time()
    index_size_before = algo.get_index_size("self")
    algo.fit(X_train)
    build_time = time.time() - t0
    index_size = algo.get_index_size("self") - index_size_before
    logger.info('Built index in %s' % build_time)
    logger.info('Index size: %s' % index_size)

    query_argument_groups = definition.query_argument_groups
    # Make sure that algorithms with no query argument groups still get run
    # once by providing them with a single, empty, harmless group
    if not query_argument_groups:
      query_argument_groups = [[]]

    for pos, query_arguments in enumerate(query_argument_groups, 1):
      logger.info("Running query argument group %d of %d..." % (pos, len(query_argument_groups)))
      if query_arguments:
        algo.set_query_arguments(*query_arguments)
      descriptor, results = run_individual_query(
        algo, X_train, X_test, distance, count, run_count, use_batch_query
      )
      descriptor["build_time"] = build_time
      descriptor["index_size"] = index_size
      descriptor["algo"] = definition.algorithm
      descriptor["dataset"] = dataset
      store_results(dataset, count, definition, query_arguments, descriptor, results)
  finally:
    algo.done()


def run_from_cmdline():
  parser = argparse.ArgumentParser()
  parser.add_argument('--dataset', choices=DATASETS.keys(), required=True)
  parser.add_argument('--algorithm', required=True)
  parser.add_argument('--module', required=True)
  parser.add_argument('--constructor', required=True)
  parser.add_argument('--count', required=True, type=int)
  parser.add_argument('build')
  parser.add_argument('queries', nargs='*', default=[])
  args = parser.parse_args()
  algo_args = json.loads(args.build)
  query_args = [json.loads(q) for q in args.queries]

  definition = Definition(
    algorithm=args.algorithm,
    docker_tag=None,  # not needed
    module=args.module,
    constructor=args.constructor,
    arguments=algo_args,
    query_argument_groups=query_args,
    disabled=False,
  )
  run(definition, args.dataset, args.count)
