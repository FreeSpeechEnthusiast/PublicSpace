
import logging
import os
import tempfile

import ann_benchmarks.datasets as ann_datasets
import numpy as np
import tensorflow.compat.v1 as tf


class BenchmarkDatasetBuilder(object):
  def __init__(self):
    self.logger = logging.getLogger(__name__)

  def _read_tab_files(self, input_embedding_dir, file_names):
    list = []
    num_dimensions = None
    for file_name in file_names:
      full_file_name = os.path.join(input_embedding_dir, file_name)
      self.logger.info("processing file: %s" % file_name)
      with tf.io.gfile.GFile(full_file_name) as gfile:
        for line in gfile:
          number_strings = line.split("\t")
          if num_dimensions is None:
            num_dimensions = len(number_strings) - 1
          else:
            assert(num_dimensions == len(number_strings) - 1)
          # the first element is the id of the embedding. All the rest are floats
          for number_string in number_strings[1:]:
            entry = float(number_string)
            list.append(entry)
    self.logger.info("create numpy array")
    list = np.asarray(list)
    self.logger.info("reshape numpy array")
    return np.reshape(list, (-1, num_dimensions))

  def process_tab_embeddings(self, dataset_name, input_embedding_dir, output_dir, distance_type):
    tf.io.gfile.makedirs(output_dir)
    file_names = tf.io.gfile.listdir(input_embedding_dir)
    self.logger.info("Splitting dataset")
    self.logger.info("Using distance type: %s" % distance_type)
    training_set, test_set = ann_datasets.train_test_split(
      self._read_tab_files(input_embedding_dir, file_names), test_size=5000
    )
    temp_dir = tempfile.mkdtemp()
    file_name = '%s-%s.hdf5' % (dataset_name, distance_type)
    temp_output_file = os.path.join(temp_dir, file_name)
    self.logger.info("Writing dataset to file: %s" % temp_output_file)
    ann_datasets.write_output(training_set, test_set, temp_output_file, distance_type)
    output_file = os.path.join(output_dir, file_name)
    self.logger.info("Moving file to: %s" % output_file)
    tf.io.gfile.copy(temp_output_file, output_file)
    self.logger.info("Delete file: %s" % temp_output_file)
    tf.io.gfile.Remove(temp_output_file)
