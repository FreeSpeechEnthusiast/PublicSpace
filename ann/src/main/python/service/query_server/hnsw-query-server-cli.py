# checkstyle: noqa
import argparse
import os
import subprocess


AURORA_FILE_PATH = "ann/src/main/aurora/service/query_server/hnsw/query_server.aurora"
DEFAULT_METASPACE_MB = 512
DEFAULT_PACKER_ROLE = "ann-platform"
DEFAULT_PACKER_NAME = "hnsw-query-server-release"


def _run(options):
  if options.dry_run_aurora:
    _dry_run_aurora(options)
  else:
    _deploy_server(options)


def _deploy_server(options):
  ## aurora
  cmd = "aurora update start \
    --bind=profile.name={name} \
    --bind=profile.role={role} \
    --bind=profile.dimension={dimension} \
    --bind=profile.id_type={id_type} \
    --bind=profile.metric={metric} \
    --bind=profile.index_directory={index_dir} \
    --bind=profile.refreshable={refreshable} \
    --bind=profile.hadoop_cluster={hadoop_cluster} \
    --bind=profile.packer_version={packer_version} \
    --bind=profile.packer_role={packer_role} \
    --bind=profile.packer_package={packer_package} \
    --bind=profile.min_index_size_byte={min_index_size_byte} \
    --bind=profile.max_index_size_byte={max_index_size_byte} \
    {cluster}/{role}/{env}/{name} \
    {aurora_file_path}\
  ".format(
    name=options.service_name,
    role=options.service_role,
    dimension=options.dimension,
    id_type=options.id_type,
    metric=options.metric,
    index_dir=options.index_dir,
    refreshable=str(options.refreshable).lower(),
    hadoop_cluster=options.hadoop_cluster,
    cluster=options.cluster,
    env=options.env,
    aurora_file_path=AURORA_FILE_PATH,
    packer_version=options.packer_version,
    packer_role=options.packer_role,
    packer_package=options.packer_package,
    min_index_size_byte=options.min_index_size_byte,
    max_index_size_byte=options.max_index_size_byte,
  )
  print(cmd)
  return subprocess.call(cmd, shell=True, env=_get_env(options))


def _dry_run_aurora(options):
  with open(AURORA_FILE_PATH) as f:
    cfg = f.read()
    cfg = cfg.replace("{{profile.name}}", str(options.service_name))
    cfg = cfg.replace("{{profile.role}}", str(options.service_role))
    cfg = cfg.replace("{{profile.dimension}}", str(options.dimension))
    cfg = cfg.replace("{{profile.id_type}}", str(options.id_type))
    cfg = cfg.replace("{{profile.metric}}", str(options.metric))
    cfg = cfg.replace("{{profile.index_directory}}", str(options.index_dir))
    cfg = cfg.replace("{{profile.refreshable}}", str(options.refreshable).lower())
    cfg = cfg.replace("{{profile.hadoop_cluster}}", str(options.hadoop_cluster))
    cfg = cfg.replace("{{profile.packer_version}}", str(options.packer_version))
    cfg = cfg.replace("{{profile.packer_role}}", str(options.packer_role))
    cfg = cfg.replace("{{profile.packer_package}}", str(options.packer_package))
    cfg = cfg.replace("{{profile.min_index_size_byte}}", str(options.min_index_size_byte))
    cfg = cfg.replace("{{profile.max_index_size_byte}}", str(options.max_index_size_byte))
    cfg = cfg.replace("int(os.environ['CPU'])", str(options.cpu))
    cfg = cfg.replace("int(os.environ['DISK_GB'])", str(options.disk))
    cfg = cfg.replace("int(os.environ['RAM_GB'])", str(options.ram))
    cfg = cfg.replace("int(os.environ['HEAP_GB'])", str(options.heap))
    cfg = cfg.replace("int(os.environ['NEW_GEN_GB'])", str(options.new_gen))
    cfg = cfg.replace("int(os.environ['METASPACE_MB'])", str(options.metaspace))
    cfg = cfg.replace("int(os.environ['INSTANCES'])", str(options.instances))
    print(cfg)
    return 0


def _get_env(options):
  new_environ = dict(os.environ)
  new_environ.update(
    CPU=str(options.cpu),
    RAM_GB=str(options.ram),
    DISK_GB=str(options.disk),
    HEAP_GB=str(options.heap),
    NEW_GEN_GB=str(options.new_gen),
    METASPACE_MB=str(options.metaspace),
    INSTANCES=str(options.instances),
  )
  return new_environ


if __name__ == "__main__":
  parser = argparse.ArgumentParser(description="Hnsw query server")
  parser.add_argument("--dimension", type=int, help="Dimension of vector", required=True)
  parser.add_argument(
    "--metric",
    type=str,
    choices=["L2", "Cosine", "InnerProduct"],
    help="Distance metric",
    required=True,
  )
  parser.add_argument(
    "--id_type",
    type=str,
    choices=["int", "long", "string", "word", "user", "tweet", "tfwId"],
    help="Entity Id type",
    required=True,
  )
  parser.add_argument(
    "--env", type=str, choices=["prod", "staging", "devel"], help="Environment", required=True
  )
  parser.add_argument("--instances", type=int, help="# of instances", required=True)
  parser.add_argument("--service_role", type=str, help="Service role", required=True)
  parser.add_argument("--service_name", type=str, help="Service name", required=True)
  parser.add_argument(
    "--cluster", type=str, choices=["smf1", "atla", "pdxa"], help="Cluster", required=True
  )
  parser.add_argument(
    "--hadoop_cluster",
    type=str,
    choices=["dw2-smf1", "proc-atla", "proc2-atla", "proc3-atla", "rt2-pdxa"],
    help="HDFS cluster",
    required=True,
  )
  parser.add_argument(
    "--disk", type=int, help="Disk space in GB, usage: supply 8 for 8GB", required=True
  )
  parser.add_argument("--cpu", type=int, help="Number of cpu", required=True)
  parser.add_argument("--ram", type=int, help="Ram in GB, usage: supply 32 for 32GB", required=True)
  parser.add_argument(
    "--heap", type=int, help="Heap in GB, usage: supply 30 for 30GB", required=True
  )
  parser.add_argument(
    "--new_gen", type=int, help="New gen in GB, usage: supply 4 for 4GB", required=True
  )
  parser.add_argument(
    "--metaspace",
    type=int,
    help="Metaspace in MB, usage: supply 512 for 512 MB",
    default=DEFAULT_METASPACE_MB,
  )
  parser.add_argument(
    "--index_dir",
    type=str,
    help="HDFS path of HNSW Index. Do not prefix with hdfs://",
    required=True,
  )
  parser.add_argument(
    "--refreshable",
    dest="refreshable",
    action="store_true",
    help="If set index will be refreshable by looking for the latest timestamp directory under index_dir provided; otherwise use index_dir as absolute path for the index",
    default=False,
  )
  parser.add_argument(
    "--packer_version",
    type=str,
    help="Packer version of hnsw query server. latest or version number",
    required=True,
  )
  parser.add_argument(
    "--packer_role",
    type=str,
    help="Packer role for hnsw query server artifact",
    default=DEFAULT_PACKER_ROLE,
    required=False,
  )
  parser.add_argument(
    "--packer_package",
    type=str,
    help="Packer name for hnsw query server artifact",
    default=DEFAULT_PACKER_NAME,
    required=False,
  )
  parser.add_argument(
    "--dry_run_aurora",
    action="store_true",
    default=False,
    help="Print aurora file. Usage --dry_run_aurora > index.aurora",
  )
  parser.add_argument(
    "--min_index_size_byte",
    type=int,
    help="Minimum size of ann index on hdfs in bytes to allow for serving",
    default=0,
  )
  parser.add_argument(
    "--max_index_size_byte",
    type=int,
    help="Maximum size of ann index on hdfs in bytes to allow for serving",
    default=1000000000000,
  )
  args = parser.parse_args()

  ram = args.ram * 1024
  heap = args.heap * 1024
  if ram < heap + args.metaspace:
    raise ValueError(
      "Ram %s should be greater than equal to heap %s + metaspace %s" % (ram, heap, args.metaspace)
    )

  min_index_size_byte = args.min_index_size_byte
  max_index_size_byte = args.max_index_size_byte
  if min_index_size_byte < 0 or min_index_size_byte > max_index_size_byte:
    raise ValueError(
      "max_index_size_byte %s should be greater than or equal to min_index_size_byte %s. min_index_size_byte cannot be less than 0"
      % (max_index_size_byte, min_index_size_byte)
    )
  _run(args)
