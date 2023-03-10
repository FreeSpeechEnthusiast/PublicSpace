import os, itertools, json, numpy, pickle, logging
from ann_benchmarks.plotting.metrics import all_metrics as metrics
import matplotlib as mpl

mpl.use('agg')
import matplotlib.pyplot as plt

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def create_pointset(data, xn, yn):
  xm, ym = (metrics[xn], metrics[yn])
  rev = ym["worst"] < 0
  data.sort(key=lambda t: t[-1], reverse=rev)  # sort by y coordinate

  axs, ays, als = [], [], []
  # Generate Pareto frontier
  xs, ys, ls = [], [], []
  last_x = xm["worst"]
  comparator = (lambda xv, lx: xv > lx) if last_x < 0 else (lambda xv, lx: xv < lx)
  for algo, algo_name, xv, yv in data:
    if not xv or not yv:
      continue
    axs.append(xv)
    ays.append(yv)
    als.append(algo_name)
    if comparator(xv, last_x):
      last_x = xv
      xs.append(xv)
      ys.append(yv)
      ls.append(algo_name)
  return xs, ys, ls, axs, ays, als


def compute_metrics(true_nn_distances, res, metric_1, metric_2):
  all_results = {}
  for i, (definition, run) in enumerate(res):
    algo = definition.algorithm
    algo_name = run.attrs['name']
    # cache distances to avoid access to hdf5 file
    run_distances = list(run['distances'])

    metric_1_value = metrics[metric_1]['function'](true_nn_distances, run_distances, run.attrs)
    metric_2_value = metrics[metric_2]['function'](true_nn_distances, run_distances, run.attrs)

    logger.info('%3d: %80s %12.3f %12.3f' % (i, algo_name, metric_1_value, metric_2_value))

    all_results.setdefault(algo, []).append((algo, algo_name, metric_1_value, metric_2_value))

  return all_results


def compute_all_metrics(true_nn_distances, run, algo):
  algo_name = run.attrs["name"]
  logger.info('--')
  logger.info(algo_name)
  results = {}
  # cache distances to avoid access to hdf5 file
  run_distances = list(run["distances"])
  run_attrs = dict(run.attrs)
  for name, metric in metrics.items():
    v = metric["function"](true_nn_distances, run_distances, run_attrs)
    results[name] = v
    if v:
      logger.info('%s: %g' % (name, v))
  return (algo, algo_name, results)


def generate_n_colors(n):
  vs = numpy.linspace(0.4, 1.0, 7)
  colors = [(0.9, 0.4, 0.4, 1.0)]

  def euclidean(a, b):
    return sum((x - y) ** 2 for x, y in zip(a, b))

  while len(colors) < n:
    new_color = max(
      itertools.product(vs, vs, vs), key=lambda a: min(euclidean(a, b) for b in colors)
    )
    colors.append(new_color + (1.0,))
  return colors


def create_linestyles(unique_algorithms):
  colors = dict(zip(unique_algorithms, generate_n_colors(len(unique_algorithms))))
  linestyles = dict(
    (algo, ['--', '-.', '-', ':'][i % 4]) for i, algo in enumerate(unique_algorithms)
  )
  markerstyles = dict(
    (algo, ['+', '<', 'o', '*', 'x'][i % 5]) for i, algo in enumerate(unique_algorithms)
  )
  faded = dict((algo, (r, g, b, 0.3)) for algo, (r, g, b, a) in colors.items())
  return dict(
    (algo, (colors[algo], faded[algo], linestyles[algo], markerstyles[algo]))
    for algo in unique_algorithms
  )


def get_up_down(metric):
  if metric["worst"] == float("inf"):
    return "down"
  return "up"


def get_left_right(metric):
  if metric["worst"] == float("inf"):
    return "left"
  return "right"


def get_plot_label(xm, ym):
  return "%(xlabel)s-%(ylabel)s tradeoff - %(updown)s and to the %(leftright)s is better" % {
    "xlabel": xm["description"],
    "ylabel": ym["description"],
    "updown": get_up_down(ym),
    "leftright": get_left_right(xm),
  }
