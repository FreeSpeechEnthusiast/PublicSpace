.. _load_test:

Loadtest
--------

We provide a tool to launch loadtest job which can generate traffic and send it to your ANN index.
This can give you a rough idea how much QPS your server can handle with respect to latencies p90, then you can use that to get a rough idea how many instances, runtime parameter of the index and resources such as cpu, memory etc you need to allocate for your server. 
You should monitor server's resource usage, lantencies, success rate of your server using monitoring graph you deployed above. 
For example, you should monitor the CPU usage during the test, especially if there is throttling happening which will deteriorate the latency.

Alongside this we also provide `benchmarking tool` as part of this suite to measure the recall of the index with different runtime/build parameters.

.. note::
  Aurora machines are inhomogeneous, so redeploying your server on a new instance and rerunning the load test may give different results.

.. markdowninclude:: ../src/main/scala/com/twitter/ann/service/loadtest/README.md
