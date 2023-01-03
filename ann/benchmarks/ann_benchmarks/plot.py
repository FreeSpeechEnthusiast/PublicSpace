import argparse
import logging
import os
import matplotlib as mpl

mpl.use('agg')
import matplotlib.pyplot as plt

from ann_benchmarks.algorithms.definitions import get_definitions, get_unique_algorithms
from ann_benchmarks.datasets import get_dataset
from ann_benchmarks.plotting.metrics import all_metrics as metrics
from ann_benchmarks.plotting.utils import (
  compute_metrics,
  create_linestyles,
  create_pointset,
  get_plot_label,
)
from ann_benchmarks.results import load_results

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


def create_plot(all_data, raw, x_log, y_log, xn, yn, fn_out, linestyles):
  xm, ym = (metrics[xn], metrics[yn])
  # Now generate each plot
  handles = []
  labels = []
  plt.figure(figsize=(12, 9))
  for algo in sorted(all_data.keys(), key=lambda x: x.lower()):
    xs, ys, ls, axs, ays, als = create_pointset(all_data[algo], xn, yn)
    color, faded, linestyle, marker = linestyles[algo]
    (handle,) = plt.plot(
      xs, ys, '-', label=algo, color=color, ms=7, mew=3, lw=3, linestyle=linestyle, marker=marker
    )
    handles.append(handle)
    if raw:
      (handle2,) = plt.plot(
        axs,
        ays,
        '-',
        label=algo,
        color=faded,
        ms=5,
        mew=2,
        lw=2,
        linestyle=linestyle,
        marker=marker,
      )
    labels.append(algo)

  if x_log:
    plt.gca().set_xscale('log')
  if y_log:
    plt.gca().set_yscale('log')
  plt.gca().set_title(get_plot_label(xm, ym))
  plt.gca().set_ylabel(ym['description'])
  plt.gca().set_xlabel(xm['description'])
  box = plt.gca().get_position()
  # plt.gca().set_position([box.x0, box.y0, box.width * 0.8, box.height])
  plt.gca().legend(handles, labels, loc='center left', bbox_to_anchor=(1, 0.5), prop={'size': 9})
  plt.grid(b=True, which='major', color='0.65', linestyle='-')
  if 'lim' in xm:
    plt.xlim(xm['lim'])
  if 'lim' in ym:
    plt.ylim(ym['lim'])
  plt.savefig(fn_out, bbox_inches='tight')
  plt.close()


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('--dataset', metavar="DATASET", default='glove-100-angular')
  parser.add_argument('--count', default=10)
  parser.add_argument('--limit', default=-1)
  parser.add_argument('-o', '--output')
  parser.add_argument(
    '-x',
    '--x-axis',
    help='Which metric to use on the X-axis',
    choices=metrics.keys(),
    default="k-nn",
  )
  parser.add_argument(
    '-y',
    '--y-axis',
    help='Which metric to use on the Y-axis',
    choices=metrics.keys(),
    default="qps",
  )
  parser.add_argument(
    '-X', '--x-log', help='Draw the X-axis using a logarithmic scale', action='store_true'
  )
  parser.add_argument(
    '-Y', '--y-log', help='Draw the Y-axis using a logarithmic scale', action='store_true'
  )
  parser.add_argument(
    '--raw',
    help='Show raw results (not just Pareto frontier) in faded colours',
    action='store_true',
  )
  args = parser.parse_args()

  if not args.output:
    args.output = 'results/%s.png' % args.dataset
    logger.info('writing output to %s' % args.output)

  dataset = get_dataset(args.dataset)
  dimension = len(dataset['train'][0])  # TODO(erikbern): ugly
  point_type = 'float'  # TODO(erikbern): should look at the type of X_train
  distance = dataset.attrs['distance']
  count = int(args.count)

  root_dir = os.path.dirname(__file__)
  algos_file = os.path.join(root_dir, 'resources/algos.yaml')

  definitions = get_definitions(algos_file, dimension, point_type, distance, count)
  unique_algorithms = get_unique_algorithms(algos_file)
  linestyles = create_linestyles(unique_algorithms)
  results = load_results(args.dataset, count, definitions)
  runs = compute_metrics(list(dataset["distances"]), results, args.x_axis, args.y_axis)
  if not runs:
    raise Exception('Nothing to plot')

  create_plot(
    runs, args.raw, args.x_log, args.y_log, args.x_axis, args.y_axis, args.output, linestyles
  )
