ANN-Benchmarks

This project is pulled from open source project https://github.com/erikbern/ann-benchmarks.
We will be making code changes in it to use it inside Twitter on aurora to perform Approximate neareast neigbours benchmarking with different techniques like annoy, faiss with Twitter datasets.

################# Preprocessed datasets available for testing #################

Global Datasets (Not for local, will work when benchmarking deployed on aurora):
fashion-mnist-784-euclidean
gist-960-euclidean
glove-25-angular
glove-50-angular
glove-100-angular
glove-200-angular
mnist-784-euclidean
sift-128-euclidean

Dummy Datasets:
random-xs-20-euclidean
random-s-100-euclidean
random-xs-20-angular
random-s-100-angular

################# Run Instructions for ANN Benchmarking #################

To run locally :
./bazel bundle ann/benchmarks:ann_benchmark
./dist/ann_benchmark.pex --dataset random-xs-20-angular --algorithm "annoy,bruteforce,kd" --local

To run on aurora:
./bazel bundle ann/benchmarks:ann_benchmark

packer add_version --cluster=smf1 cortex ann_benchmark dist/ann_benchmark.pex

ALGORITHM=annoy,bruteforce,kd,hnsw(faiss),faiss-ivf,faiss-lsh
DATASET=canonical-english-sum
EXPERIMENT_ID=test_run_1
aurora job create smf1/cortex/prod/ann_benchmark \
  ann/benchmarks/benchmark.aurora \
  --bind=job_algorithm=$ALGORITHM \
  --bind=job_dataset=$DATASET \
  --bind=job_experiment_id=$EXPERIMENT_ID

################# Current plots and metrics as part of benchmarking #################

METRICS
-ann/benchmarks/ann_benchmarks/plotting/metrics.py

Recall
Epsilon 0.01 Recall
Epsilon 0.1 Recall
Relative Error
Queries per second (1/s)
Build time (s)
Index size (kB)
Candidates Generated
Query Size (Index size (kB)/Queries per second (s))


PLOTS
-ann/benchmarks/ann_benchmarks/plotting/plot_variants.py

recall/qps
recall/buildtime
recall/index size
Relative error/qps
recall/candidates
recall/query-size
Epsilon 0.01 recall/qps
Epsilon 0.1 recall/qps

################# Downloading benchmarking PLOTS #################

Script for downloading and opening the plots/results for particular experimentId and dataset after benchmark job has completed:

sh ./ann/benchmarks/download_plots.sh <devel|stage|prod> <dataset> <experimentId>

Plots will be stored in ann_benchmark_results directory. can directly view the ann benchmarking results as interactive plots by opening index.html in the above folder.

Raw Benchmark results will be stored in hdfs directory : 
hdfs://user/cortex/ann_benchmarks/runs/<env>/<dataset>/<experiment_id>/ann_benchmark_result/....

Interactive Website/plots will be store in hdfs directory: 
hdfs://user/cortex/ann_benchmarks/runs/<env>/<dataset>/<experiment_id>/ann_benchmark_plots/....


################# Code changes compared to open source project #################
Reason for code changes:
1) Docker based images not supported inside Twitter
2) For benchmarking Twitter datasets, datasets need to be pulled from Twitter HDFS cluster
3) Benchmarking results will again be store in HDFS
4) Custom scripts need to be written to parse and create Twitter datasets in HDF5 format.
5) Build scripts and aurora scripts to be added to run in Twitter DC

Some main changes: 
-ann/benchmarks/ann_benchmarks/datasets.py
Modified to read different datasets from hdfs cluster.

ann/benchmarks/ann_benchmarks/main.py
Docker based code and dependencies removed.
More running options added to support hdfs read and write path.
Code modifications to generate plots as part of this run.

-ann/benchmarks/ann_benchmarks/results.py
Modified to read and write results from local path as per the pex

-ann/benchmarks/ann_benchmarks/results.py
Modified to read and write results from local path as per the pex

-ann/benchmarks/ann_benchmarks/create_website.py
Modified to be called from main.py instead of running the whole file as python script

-Yaml, website templates moved/modified (Removed some of the checks) to ann/benchmarks/ann_benchmarks/resources

