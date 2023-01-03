#!/usr/bin/env bash

NEST=hadoopnest1

echo "Using $NEST as bouncing point."

echo "Getting plots...."

ENV=$1
DATASET=$2
EXPERIMENT_ID=$3

ssh $NEST bash -c "'
  rm -f plots.zip
  rm -rf ann_benchmark_plots
  hadoop fs -get viewfs:///user/cortex/ann_benchmarks/runs/$ENV/$DATASET/$EXPERIMENT_ID/ann_benchmark_plots
  zip -r plots.zip ann_benchmark_plots
'"

rm -f plots.zip
rm -rf ./ann_benchmark_plots

ssh $NEST 'cat plots.zip' > plots.zip
unzip plots.zip
rm plots.zip

open ann_benchmark_plots/index.html



