import argparse
import hashlib
import logging
import os
import matplotlib as mpl

mpl.use('agg')

from ann_benchmarks import results
from ann_benchmarks.datasets import get_dataset
from ann_benchmarks.plotting.metrics import all_metrics as metrics
from ann_benchmarks.plotting.plot_variants import all_plot_variants as plot_variants
from ann_benchmarks.plotting.utils import (
  compute_all_metrics,
  compute_metrics,
  create_linestyles,
  create_pointset,
  get_plot_label,
)
from jinja2 import Environment, FileSystemLoader
from ann_benchmarks.constants import LOCAL_PLOT_WEBSITE_DIR
import ann_benchmarks.plot as benchmark_plot


logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

output_dir = LOCAL_PLOT_WEBSITE_DIR + "/"

colors = [
  "rgba(166,206,227,1)",
  "rgba(31,120,180,1)",
  "rgba(178,223,138,1)",
  "rgba(51,160,44,1)",
  "rgba(251,154,153,1)",
  "rgba(227,26,28,1)",
  "rgba(253,191,111,1)",
  "rgba(255,127,0,1)",
  "rgba(202,178,214,1)",
]

point_styles = {"o": "circle", "<": "triangle", "*": "star", "x": "cross", "+": "rect"}


def convert_color(color):
  r, g, b, a = color
  return "rgba(%(r)d, %(g)d, %(b)d, %(a)d)" % {"r": r * 255, "g": g * 255, "b": b * 255, "a": a}


def convert_linestyle(ls):
  new_ls = {}
  for algo in ls.keys():
    algostyle = ls[algo]
    new_ls[algo] = (
      convert_color(algostyle[0]),
      convert_color(algostyle[1]),
      algostyle[2],
      point_styles[algostyle[3]],
    )
  return new_ls


def get_run_desc(properties):
  return "%(dataset)s_%(count)d_%(distance)s" % properties


def get_dataset_from_desc(desc):
  return desc.split("_")[0]


def get_count_from_desc(desc):
  return desc.split("_")[1]


def get_distance_from_desc(desc):
  return desc.split("_")[2]


def get_dataset_label(desc):
  return get_dataset_from_desc(desc) + " (k = " + get_count_from_desc(desc) + ")"


def directory_path(s):
  if not os.path.isdir(s):
    raise argparse.ArgumentTypeError("'%s' is not a directory" % s)
  return s + "/"


def prepare_data(data, xn, yn):
  """Change format from (algo, instance, dict) to (algo, instance, x, y)."""
  res = []
  for algo, algo_name, result in data:
    res.append((algo, algo_name, result[xn], result[yn]))
  return res


def get_lines(all_data, xn, yn, render_all_points):
  """For each algorithm run on a dataset, obtain its performance curve coords."""
  plot_data = []
  for algo in sorted(all_data.keys(), key=lambda x: x.lower()):
    xs, ys, ls, axs, ays, als = create_pointset(prepare_data(all_data[algo], xn, yn), xn, yn)
    if render_all_points:
      xs, ys, ls = axs, ays, als
    plot_data.append(
      {"name": algo, "coords": zip(xs, ys), "labels": ls, "scatter": render_all_points}
    )
  return plot_data


def create_plot(all_data, xn, yn, linestyle, j2_env, additional_label="", plottype="line"):
  xm, ym = (metrics[xn], metrics[yn])
  render_all_points = plottype == "bubble"
  plot_data = get_lines(all_data, xn, yn, render_all_points)
  latex_code = j2_env.get_template("latex.template").render(
    plot_data=plot_data,
    caption=get_plot_label(xm, ym),
    xlabel=xm["description"],
    ylabel=ym["description"],
  )
  plot_data = get_lines(all_data, xn, yn, render_all_points)
  button_label = hashlib.sha224(
    (get_plot_label(xm, ym) + additional_label).encode("utf-8")
  ).hexdigest()
  return j2_env.get_template("chartjs.template").render(
    latex_code=latex_code,
    button_label=button_label,
    data_points=plot_data,
    xlabel=xm["description"],
    ylabel=ym["description"],
    plottype=plottype,
    plot_label=get_plot_label(xm, ym),
    label=additional_label,
    linestyle=linestyle,
    render_all_points=render_all_points,
  )


def build_detail_site(data, label_func, j2_env):
  for (name, runs) in data.items():
    logger.info("Building '%s'" % name)
    all_runs = runs.keys()
    linestyles = convert_linestyle(create_linestyles(all_runs))
    label = label_func(name)
    data = {"normal": [], "scatter": []}

    for plottype in plot_variants.keys():
      xn, yn = plot_variants[plottype]
      data["normal"].append(create_plot(runs, xn, yn, linestyles, j2_env))
      data["scatter"].append(
        create_plot(runs, xn, yn, linestyles, j2_env, "Scatterplot ", "bubble")
      )

    # create png plot for summary page
    data_for_plot = {}
    for k in runs.keys():
      data_for_plot[k] = prepare_data(runs[k], 'k-nn', 'qps')
    benchmark_plot.create_plot(
      data_for_plot,
      False,
      False,
      True,
      'k-nn',
      'qps',
      output_dir + name + ".png",
      create_linestyles(all_runs),
    )
    with open(output_dir + name + ".html", "w") as text_file:
      text_file.write(j2_env.get_template("detail_page.html").render(title=label, plot_data=data))


def build_index_site(datasets, algorithms, j2_env, file_name):
  distance_measures = sorted(set([get_distance_from_desc(e) for e in datasets.keys()]))
  sorted_datasets = sorted(set([get_dataset_from_desc(e) for e in datasets.keys()]))

  dataset_data = []

  for dm in distance_measures:
    d = {"name": dm.capitalize(), "entries": []}
    for ds in sorted_datasets:
      matching_datasets = [
        e
        for e in datasets.keys()
        if get_dataset_from_desc(e) == ds and get_distance_from_desc(e) == dm
      ]
      sorted_matches = sorted(matching_datasets, key=lambda e: int(get_count_from_desc(e)))
      for idd in sorted_matches:
        d["entries"].append({"name": idd, "desc": get_dataset_label(idd)})
    dataset_data.append(d)

  with open(output_dir + "index.html", "w") as text_file:
    text_file.write(
      j2_env.get_template("summary.html").render(
        title="ANN-Benchmarks", dataset_with_distances=dataset_data, algorithms=algorithms.keys()
      )
    )


def load_all_results():
  """Read all result files and compute all metrics"""
  all_runs_by_dataset = {}
  all_runs_by_algorithm = {}
  cached_true_dist = []
  old_sdn = None
  for f in results.load_all_results():
    properties = dict(f.attrs)
    # TODO Fix this properly. Sometimes the hdf5 file returns bytes
    # This converts these bytes to strings before we work with them
    for k in properties.keys():
      try:
        properties[k] = properties[k].decode()
      except BaseException:
        pass
    sdn = get_run_desc(properties)
    if sdn != old_sdn:
      dataset = get_dataset(properties["dataset"])
      cached_true_dist = list(dataset["distances"])
      old_sdn = sdn
    algo = properties["algo"]
    ms = compute_all_metrics(cached_true_dist, f, properties["algo"])
    algo_ds = get_dataset_label(sdn)

    all_runs_by_algorithm.setdefault(algo, {}).setdefault(algo_ds, []).append(ms)
    all_runs_by_dataset.setdefault(sdn, {}).setdefault(algo, []).append(ms)
  return (all_runs_by_dataset, all_runs_by_algorithm)


def create_website():
  logger.info("Creating website....")

  os.mkdir(output_dir)

  runs_by_ds, runs_by_algo = load_all_results()
  root_dir = os.path.dirname(__file__)
  templates_dir = os.path.join(root_dir, 'resources/templates/')
  j2_env = Environment(loader=FileSystemLoader(templates_dir), trim_blocks=True)
  j2_env.globals.update(zip=zip)
  build_detail_site(runs_by_ds, lambda label: get_dataset_label(label), j2_env)
  build_detail_site(runs_by_algo, lambda x: x, j2_env)
  build_index_site(runs_by_ds, runs_by_algo, j2_env, "index.html")
