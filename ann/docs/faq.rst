.. _faq:

Frequently asked questions
===========================

Have a question? If you can't find answer here, reach us via Slack at #mlx-support. We are happy to help!


* **How to use HDFS directory for storing/retrieving the ANN index**

  The ANN library supports serializing and reading index to/from HDFS. The ANN query service also supports deploying an index which is serialized to HDFS. For HDFS based file handling we leverage `AbstractFile <https://cgit.twitter.biz/source/tree/src/java/com/twitter/search/common/file/AbstractFile.java>`_ . For serializing the index to hdfs from library you should append `hdfs://` before the actual directory for creating the file handle using `FileUtils <https://cgit.twitter.biz/source/tree/src/java/com/twitter/search/common/file/FileUtils.java>`_.
  When using the ANN library as part of aurora job, you can use JVM binary in the BUILD and use `HadoopProcess` in aurora file to specify the hadoop conf directory for atla/smf1 cluster. The index will automatically access the namenode config and use the same for serializing/deserializing the index.
  Sample usage in aurora file : `Aurora <https://cgit.twitter.biz/source/tree/ann/src/main/aurora/service/query_server/hnsw/query_server.aurora#n39>`_ . Sample JVM binary : `BUILD <https://cgit.twitter.biz/source/tree/ann/src/main/aurora/service/query_server/hnsw/query_server.aurora#n39>`_ .

  .. code-block:: scala

    // This creates hdfs based file handle and take up the configuration as specified in HadoopProcess in aurora file.
    val directory = FileUtils.getFileHandle("hdfs:///user/something/index_directory")

    // This creates hdfs based file handle by specifying hadoop config explicitly.
    val directory = FileUtils.getHdfsFileHandleFromConfig("hdfs:///user/something/index_directory", FileUtils.getHdfsFileHandleFromConfig("dw2-smf1"))
