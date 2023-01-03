import argparse
import logging

import benchmark_dataset_builder


logging.basicConfig(level=logging.INFO)


def main():
  parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
  parser.add_argument(
    '--input_tab_embedding_dir',
    help='directory for storing benchmarking that holds the tab formatted embeddings',
    required=True,
    default=None)
  parser.add_argument(
    '--output_dir',
    help='directory where output embeddings are stored',
    required=True,
    default=None)
  parser.add_argument(
    '--dataset_name',
    help='name of the dataset to that you are producing',
    required=True,
    default=None)
  parser.add_argument(
    '--distance_type',
    help='the type of distance to use',
    required=False,
    choices=['angular', 'euclidean', 'hamming', 'jaccard'],
    default='angular')
  args = parser.parse_args()
  benchmark_dataset_builder.BenchmarkDatasetBuilder().process_tab_embeddings(
    args.dataset_name, args.input_tab_embedding_dir, args.output_dir, args.distance_type
  )


main()
