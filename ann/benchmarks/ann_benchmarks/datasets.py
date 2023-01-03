import logging
import os

from ann_benchmarks.constants import LOCAL_DATASET_DIR
import h5py
import numpy
import tensorflow.compat.v1 as tf


logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


def get_local_dataset_fn(dataset):
  return os.path.join(LOCAL_DATASET_DIR, '%s.hdf5' % dataset)


def get_hdfs_dataset_fn(dataset, hdfs_path):
  return os.path.join(hdfs_path, '%s.hdf5' % dataset)


def get_dataset(dataset):
  local_dataset_path = get_local_dataset_fn(dataset)
  if not os.path.exists(local_dataset_path):
    raise Exception('%s dataset does not exist on local %s' % (dataset, local_dataset_path))
  else:
    return h5py.File(local_dataset_path)


def fetch_dataset_on_local(dataset, hdfs_path=None):
  if dataset in DUMMY_DATASETS:
    create_dummy_dataset(dataset)
  else:
    fetch_dataset_from_hdfs(dataset, hdfs_path)


def create_dummy_dataset(dataset):
  if not os.path.exists(LOCAL_DATASET_DIR):
    os.mkdir(LOCAL_DATASET_DIR)

  local_dataset_path = get_local_dataset_fn(dataset)
  if not os.path.exists(local_dataset_path):
    logging.info('Creating dummy dataset %s ...' % dataset)
    DATASETS[dataset](local_dataset_path)


def fetch_dataset_from_hdfs(dataset, hdfs_path):
  if not hdfs_path:
    raise Exception('Parent hdfs path of dataset not defined.')

  if not os.path.exists(LOCAL_DATASET_DIR):
    os.mkdir(LOCAL_DATASET_DIR)

  hdfs_dataset_path = get_hdfs_dataset_fn(dataset, hdfs_path)
  if not tf.io.gfile.exists(hdfs_dataset_path):
    raise Exception('%s dataset not found in hdfs %s ' % (dataset, hdfs_dataset_path))

  logging.info('Copying %s dataset from hdfs %s to local ...' % (dataset, hdfs_dataset_path))
  local_dataset_path = get_local_dataset_fn(dataset)
  tf.io.gfile.copy(hdfs_dataset_path, local_dataset_path)


def write_output(train, test, fn, distance, count=100):
  from ann_benchmarks.algorithms.bruteforce import BruteForceBLAS

  n = 0
  f = h5py.File(fn, 'w')
  f.attrs['distance'] = distance
  logger.info('train size: %9d * %4d' % train.shape)
  logger.info('test size:  %9d * %4d' % test.shape)
  f.create_dataset('train', (len(train), len(train[0])), dtype='f')[:] = train
  f.create_dataset('test', (len(test), len(test[0])), dtype='f')[:] = test
  neighbors = f.create_dataset('neighbors', (len(test), count), dtype='i')
  distances = f.create_dataset('distances', (len(test), count), dtype='f')
  bf = BruteForceBLAS(distance, precision=numpy.float32)
  bf.fit(train)
  queries = []
  for i, x in enumerate(test):
    if i % 1000 == 0:
      logger.info('%d/%d...' % (i, test.shape[0]))
    res = list(bf.query_with_distances(x, count))
    res.sort(key=lambda t: t[-1])
    neighbors[i] = [j for j, _ in res]
    distances[i] = [d for _, d in res]
  f.close()


def train_test_split(X, test_size=10000):
  import sklearn.model_selection

  logger.info('Splitting %d*%d into train/test' % X.shape)
  return sklearn.model_selection.train_test_split(X, test_size=test_size, random_state=1)


def random(out_fn, n_dims, n_samples, centers, distance):
  import sklearn.datasets

  X, _ = sklearn.datasets.make_blobs(
    n_samples=n_samples, n_features=n_dims, centers=centers, random_state=1
  )
  X_train, X_test = train_test_split(X, test_size=0.1)
  write_output(X_train, X_test, out_fn, distance)


def dataset_creation_not_supported(out_fn):
  raise Exception(
    'Dataset creation not supported for this. Use the processed version available on hdfs'
  )


DATASETS = {
  # Dummy datasets
  'random-xs-20-euclidean': lambda out_fn: random(out_fn, 20, 10000, 100, 'euclidean'),
  'random-s-100-euclidean': lambda out_fn: random(out_fn, 100, 100000, 1000, 'euclidean'),
  'random-xs-20-angular': lambda out_fn: random(out_fn, 20, 10000, 100, 'angular'),
  'random-s-100-angular': lambda out_fn: random(out_fn, 100, 100000, 1000, 'angular'),
  # Prepared datasets already available in hdfs in hdf5 format
  'fashion-mnist-784-euclidean': dataset_creation_not_supported,
  'gist-960-euclidean': dataset_creation_not_supported,
  'glove-25-angular': dataset_creation_not_supported,
  'glove-50-angular': dataset_creation_not_supported,
  'glove-100-angular': dataset_creation_not_supported,
  'glove-200-angular': dataset_creation_not_supported,
  'mnist-784-euclidean': dataset_creation_not_supported,
  'sift-128-euclidean': dataset_creation_not_supported,
  # Twitter datasets
  'canonical-english-sum-angular': dataset_creation_not_supported,
}

DUMMY_DATASETS = [
  'random-xs-20-euclidean' 'random-s-100-euclidean',
  'random-xs-20-angular',
  'random-s-100-angular',
]
