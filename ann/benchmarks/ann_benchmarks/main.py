import argparse
import os
import random
import sys
import shutil
import traceback
import logging
import tensorflow.compat.v1 as tf
from datetime import datetime

from ann_benchmarks.datasets import get_dataset, DATASETS, fetch_dataset_on_local
from ann_benchmarks.results import cleanup_existing_local_data, move_benchmark_results_to_hdfs
from ann_benchmarks.algorithms.definitions import (
  get_definitions,
  list_algorithms,
  get_result_filename,
  algorithm_status,
  InstantiationStatus,
)
from ann_benchmarks.runner import run
from ann_benchmarks.create_website import create_website

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def positive_int(s):
  i = None
  try:
    i = int(s)
  except ValueError:
    pass
  if not i or i < 1:
    raise argparse.ArgumentTypeError("%r is not a positive integer" % s)
  return i


def main():
  parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
  parser.add_argument(
    '--dataset',
    metavar='NAME',
    help='the dataset to load training points from',
    default='random-xs-20-angular',
    choices=DATASETS.keys(),
  )
  parser.add_argument(
    "-k",
    "--count",
    default=10,
    type=positive_int,
    help="the number of near neighbours to search for",
  )
  parser.add_argument(
    '--algorithm', metavar='NAME', help='comma separated algorithms to run', default=None
  )
  parser.add_argument(
    '--list-algorithms',
    help='print the names of all known algorithms and exit',
    action='store_true',
  )
  parser.add_argument(
    '--runs',
    metavar='COUNT',
    type=positive_int,
    help='run each algorithm instance %(metavar)s times and use only the best result',
    default=3,
  )
  parser.add_argument(
    '--timeout',
    type=int,
    help='Timeout (in seconds) for each individual algorithm run, or -1 if no timeout should be set',
    default=-1,
  )
  parser.add_argument(
    '--max-n-algorithms',
    type=int,
    help='Max number of algorithms to run (just used for testing)',
    default=-1,
  )
  parser.add_argument(
    '--run-disabled', help='run algorithms that are disabled in algos.yml', action='store_true'
  )
  parser.add_argument(
    '--output_hdfs_dir',
    metavar='NAME',
    help='HDFS directory for storing benchmarking results',
    default=None,
  )
  parser.add_argument(
    '--dataset_hdfs_dir',
    metavar='NAME',
    help='HDFS directory for prepared datasets in hdf5 format',
    default=None,
  )
  parser.add_argument(
    '--local',
    action='store_true',
    help='If set, then results will be stored locally, and will not be cleanedup and moved to hdfs',
  )
  parser.add_argument(
    '--experiment_id',
    metavar='NAME',
    help='Experiment Id, Result directory will bve scoped with this id',
    default=None,
  )

  args = parser.parse_args()
  if args.timeout == -1:
    args.timeout = None

  if not args.local:
    if not args.output_hdfs_dir:
      raise Exception('HDFS storage path not specified')

    if not args.experiment_id:
      raise Exception('Experiment Id not specified')

    output_hdfs_path = os.path.join(args.output_hdfs_dir, args.dataset, args.experiment_id)
    if tf.io.gfile.exists(output_hdfs_path):
      raise Exception(
        'ExperimentId %s for dataset %s already exist, use a different one.'
        % (args.experiment_id, args.dataset)
      )

  if args.list_algorithms:
    list_algorithms(args.definitions)
    sys.exit(0)

  cleanup_existing_local_data()
  fetch_dataset_on_local(args.dataset, args.dataset_hdfs_dir)

  dataset = get_dataset(args.dataset)
  dimension = len(dataset['train'][0])  # TODO(erikbern): ugly
  point_type = 'float'  # TODO(erikbern): should look at the type of X_train
  distance = dataset.attrs['distance']

  root_dir = os.path.dirname(__file__)
  algos = os.path.join(root_dir, 'resources/algos.yaml')
  definitions = get_definitions(algos, dimension, point_type, distance, args.count)
  random.shuffle(definitions)

  if not args.algorithm:
    raise Exception('Algorithms not specified')

  logger.info('running only %s' % args.algorithm)
  algorithms = args.algorithm.split(',')
  definitions = [d for d in definitions if d.algorithm in algorithms]

  def _test(df):
    status = algorithm_status(df)
    # If the module was loaded but doesn't actually have a constructor of
    # the right name, then the definition is broken
    assert (
      status != InstantiationStatus.NO_CONSTRUCTOR
    ), """\
%s.%s(%s): error: the module '%s' does not expose the named constructor""" % (
      df.module,
      df.constructor,
      df.arguments,
      df.module,
    )
    if status == InstantiationStatus.NO_MODULE:
      # If the module couldn't be loaded (presumably because of a missing
      # dependency), print a warning and remove this definition from the
      # list of things to be run
      logger.info(
        """\
%s.%s(%s): warning: the module '%s' could not be loaded; skipping"""
        % (df.module, df.constructor, df.arguments, df.module)
      )
      return False
    else:
      return True

  definitions = [d for d in definitions if _test(d)]

  if not args.run_disabled:
    if len([d for d in definitions if d.disabled]):
      logger.info('Not running disabled algorithms: %s ' % [d for d in definitions if d.disabled])
    definitions = [d for d in definitions if not d.disabled]

  if args.max_n_algorithms >= 0:
    definitions = definitions[: args.max_n_algorithms]

  if len(definitions) == 0:
    raise Exception('Nothing to run')
  else:
    logger.info('Order: %s ' % definitions)

  for definition in definitions:
    logger.info(definition)

    try:
      run(definition, args.dataset, args.count, args.runs)
    except KeyboardInterrupt:
      break
    except Exception:
      traceback.print_exc()

  create_website()

  if not args.local:
    logger.info('Moving data to hdfs %s' % output_hdfs_path)
    move_benchmark_results_to_hdfs(output_hdfs_path)
    cleanup_existing_local_data()
