# Benchmark Dataset Builder Job
## Overview
This job is intended to convert twitter datasets into the format used by the ann
benchmarking job. It can read directly from HDFS and write to HDFS.

There is only one dataset supported right now.

The word embedding dataset: canonical-english-sum-angular.hdf5

## Running the job
### Running Locally
You need to get the word embeddings on your local machine for this to work.

./bazel run ann/benchmark_dataset_builder:main -- \
  --input_tab_embedding_dir test_input_dir \
  --output_dir /Users/mhansmire/workspace/source/built_ouput
  --dataset_name canonical-english-sum

### Running in Aurora
The locations of the data are hard coded into the aurora file. The defaults should be correct.

./bazel bundle ann/benchmark_dataset_builder:main

packer add_version --cluster=smf1 cortex benchmark_dataset_builder dist/main.pex

INPUT_TAB_EMBEDDING_DIR=hdfs://default/user/cortex/embeddings/word/canonical_english/2018/06/01/sum_embeddings
DATASET_NAME=canonical-english-sum
OUTPUT_DIR=hdfs://default/user/cortex/ann_benchmarks/truth_datasets
aurora job create smf1/cortex/devel/benchmark_dataset_builder \
  ann/benchmark_dataset_builder/benchmark_dataset_builder.aurora \
    --bind=output_dir_binding=$OUTPUT_DIR \
    --bind=dataset_name_binding=$DATASET_NAME \
    --bind=input_tab_embedding_dir_binding=$INPUT_TAB_EMBEDDING_DIR

