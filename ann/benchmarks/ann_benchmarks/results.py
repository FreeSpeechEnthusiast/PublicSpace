import h5py
import os
import shutil
import tensorflow.compat.v1 as tf
import logging

from ann_benchmarks.algorithms.definitions import get_result_filename
from ann_benchmarks.constants import (
  INDEX_DIR,
  LOCAL_DATASET_DIR,
  LOCAL_RESULT_DIR,
  LOCAL_PLOT_WEBSITE_DIR,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def store_results(dataset, count, definition, query_arguments, attrs, results):
  fn = get_result_filename(dataset, count, definition, query_arguments)
  head, tail = os.path.split(fn)
  if not os.path.isdir(head):
    os.makedirs(head)
  f = h5py.File(fn, 'w')
  for k, v in attrs.items():
    f.attrs[k] = v
  times = f.create_dataset('times', (len(results),), 'f')
  neighbors = f.create_dataset('neighbors', (len(results), count), 'i')
  distances = f.create_dataset('distances', (len(results), count), 'f')
  for i, (time, ds) in enumerate(results):
    times[i] = time
    neighbors[i] = [n for n, d in ds] + [-1] * (count - len(ds))
    distances[i] = [d for n, d in ds] + [float('inf')] * (count - len(ds))
  f.close()


def load_results(dataset, count, definitions):
  for definition in definitions:
    query_argument_groups = definition.query_argument_groups
    if not query_argument_groups:
      query_argument_groups = [[]]
    for query_arguments in query_argument_groups:
      fn = get_result_filename(dataset, count, definition, query_arguments)
      if os.path.exists(fn):
        f = h5py.File(fn)
        yield definition, f
        f.close()


def load_all_results():
  for root, _, files in os.walk(LOCAL_RESULT_DIR + "/"):
    for fn in files:
      try:
        f = h5py.File(os.path.join(root, fn))
        yield f
        f.close()
      except Exception:
        pass


def move_benchmark_results_to_hdfs(hdfs_dir):
  logger.info("Moving %s to hdfs" % LOCAL_RESULT_DIR)
  move_benchmark_local_directory_to_hdfs(LOCAL_RESULT_DIR, hdfs_dir)
  logger.info("Moving %s to hdfs" % LOCAL_PLOT_WEBSITE_DIR)
  move_benchmark_local_directory_to_hdfs(LOCAL_PLOT_WEBSITE_DIR, hdfs_dir)


def move_benchmark_local_directory_to_hdfs(local_dir, hdfs_dir):
  walks = list(tf.io.gfile.walk(local_dir))
  for data in walks:
    top_level_dir = data[0]
    files = data[2]
    curr_hdfs_dir = os.path.join(hdfs_dir, top_level_dir)

    if not tf.io.gfile.exists(curr_hdfs_dir):
      tf.io.gfile.makedirs(curr_hdfs_dir)

    for file in files:
      old_local_path = os.path.join(top_level_dir, file)
      new_hdfs_path = os.path.join(curr_hdfs_dir, file)
      logger.info("Copying file from local %s to hdfs %s" % (old_local_path, new_hdfs_path))
      tf.io.gfile.copy(old_local_path, new_hdfs_path)


def cleanup_existing_local_data():
  logger.info("Cleaning up the local directories...")
  if os.path.exists(INDEX_DIR):
    shutil.rmtree(INDEX_DIR)

  if os.path.exists(LOCAL_DATASET_DIR):
    shutil.rmtree(LOCAL_DATASET_DIR)

  if os.path.exists(LOCAL_RESULT_DIR):
    shutil.rmtree(LOCAL_RESULT_DIR)

  if os.path.exists(LOCAL_PLOT_WEBSITE_DIR):
    shutil.rmtree(LOCAL_PLOT_WEBSITE_DIR)
