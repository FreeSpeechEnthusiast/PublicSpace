∆Product Name∆ Runbook
======================

.. admonition:: Delete this admonition before publishing

  Runbook title format: ∆Product Name∆ Runbook.

  If a section does not apply, keep the heading and write **Not Applicable**
  underneath it.

  `TCC (Twitter Command Center) <http://go/tcc>`_ must review a product
  runbook to approve its `Production Readiness Review <http://go/prr>`_.
  Please follow the format to improve PRR quality.

  `go/modelrunbook <http://go/modelrunbook>`_ and `go/runbookstyleguide
  <http://go/runbookstyleguide>`_ provide information on writing runbooks.

  If you have questions about this template, feel free to visit the `#techdocs
  <http://go/slack/techdocs>`_ channel on Slack.

..note::

   This document is available at http://go/∆product-name∆-runbook.

**∆** *Briefly describe the features and functions of the product.*

**∆** *Briefly describe the system architecture.*



Contact
-------

:PagerDuty: `∆product-name∆@twitter.com <∆product-name∆@twitter.com>`_
:Slack: `#∆product-name∆ <http://go/slack/∆product-name∆>`_ channel
:Jira Project: `∆PRODUCTNAME∆ <http://go/jira/∆PRODUCTNAME>`_
:Docs: `go/docbird/∆product-name∆ <http://go/docbird/∆product-name∆>`_
:Team: `go/∆team-name∆ <http://go/∆team-name∆>`_
:Email: `∆product-name∆@twitter.com <∆product-name∆@twitter.com>`_

Information
-----------

:Kite: `go/kite/∆service-name∆ <http://go/kite/∆service-name∆>`_
:Design Doc: `go/∆service-name∆-tdd <http://go/∆service-name∆-tdd>`_
:Git: `∆Product Name∆ Source Code <http://go/code/∆product-name∆/src/...>`_
:Deploy Branch: **∆** *Link to Git deploy branch*
:Jenkins: **∆** *Link to Jenkins build*

Architecture
------------

**∆** *A high level overview of your product's architecture, including your service and dependencies. Include a diagram, if available.*

Dependencies
------------

**∆** *Create a subheading (heading-3 ``~``) for each service your service depends on to function properly. Under each heading, briefly describe the dependency, and mention failure modes - what will happen if the dependency were to fail.*

Example dependency: Manhattan
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This service uses Manhattan to:

- foobars: creates baz. 200 write max, 100 pkey read max, and 1000 lkey read max.

Failure Modes
^^^^^^^^^^^^^

- Feature baz will stop working.

Dependents
----------

**∆** *Create a subheading (heading-3 ``~``) for each service that depends on your service. Under each heading, briefly describe that dependent, and mention failure modes - what will happen if your service fails.*

Capacity
--------

Thresholds
~~~~~~~~~~

**∆** *List out capacity thresholds in queries per second (QPS), and corresponding service behavior if thresholds are passed.*

Management
~~~~~~~~~~

**∆** *Describe how capacity is managed and how more can be added.*

SLA
---

.. csv-table::
   :header: Description, SLA

   Success rate, > .997

Expected Impact When Service in Degradation or Outage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**∆** *Describe the impact to users, partners, and revenue when service is running in degraded mode or down entirely.*

Recent Deployments
------------------

**∆** *Link to a JIRA filter for the product's Production Change Management (PCM) component.*

Aurora Clusters
~~~~~~~~~~~~~~~

.. Don't be scared to edit this.
   The empty lines create breaks in a row.
   Otherwise it's simply one line of code per row!

.. csv-table::
   :header: Cluster, Environment, Job, Deployments, Loglens
   :widths: 3, 5, 7, 10, 10

   nDX, Prod, service, "Aurora ATL Link

   Aurora SMF Link", "Loglens ATL Link

   Loglens SMF Link"
   nDC, Staging, service, "Aurora ATL Link

   Aurora SMF Link", "Loglens ATL Link

   Loglens SMF Link"
   nDC, Devel, service, "Aurora ATL Link

   Aurora SMF Link", "Loglens ATL Link

   Loglens SMF Link"

Storage
~~~~~~~

.. csv-table::
   :header: Cluster Environment, Manhattan

   Prod, "MH Application Link

   MH Alert Link

   MH Dashboard Link"
   Staging, "MH Application Link

   MH Alert Link

   MH Dashboard Link"
   Dev, "MH Application Link

   MH Alert Link

   MH Dashboard Link"

Dashboards
----------

**∆** *Link to your dashboard. You should only have ONE OFFICIAL dashboard that is
displaying muti-DC charts.*

*For details, see `Monitoring 2.0 <http://go.twitter.biz/docs/mon/getting-started>`_.*

Alerts
------

**∆** *Link to alerts. Ideally you should have one OFFICIAL alerts dash to show ALL your critical alerts.*

*For details, see `Monitoring 2.0 <http://go.twitter.biz/docs/mon/getting-started>`_.*

Remediations
------------

**∆** *Create a subheading (heading-3 ``~``) for each alert, and explain the problem, its impact, possible causes, remediations, and investigation detail.*

"Free Heap Space is Low on Multiple Machines"
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Problem**:

**∆** *A summary of the problem indicated by the alert.*

**Impact**:

**∆** *An explanation of the problem's impact.*

**Possible Causes**:

**∆** *A list of possible causes.*

- Symptom X is caused by...
- Symptom Y is caused by...

**Remediations**:
**∆** *A list of potential remediations.*

- Immediate remediations: Restart the affected instances, recommended batch size 5 at a time
- Long term remediations: Investigate code performance (can be done during business hours).

**Investigation**:

**∆** *A list of steps to take to investigate the problem.*

- Check recent code changes for this service or it's dependencies for correlation
- Profile code
- Tune GC settings and test changes via a canary

Deciders
--------

.. csv-table::
   :header: Decider name, Decider type, LDAP Owner, Description

   failover_my_service_1, failover, service-ldap, Expected user impact
   tfe_my_service_1_maintenance, load shedding, service-ldap, Expected user impact

Configuration
-------------

Aurora/Mesos
~~~~~~~~~~~~

**∆** *Add Aurora/Mesos configuration and quota.*

*For more details, see the `Aurora docs <http://go/aurora>`_.*

Zookeeper
~~~~~~~~~

**∆** *Describe the namespaces.*

*For more details, see `Wily <http://go/wily>`_ and `Zookeeper Client Best Practices
<https://docbird.twitter.biz/zookeeper_client_best_practices/index.html>`_.*

Wily
~~~~

**∆** *Add Wily name space.*

*For more details, see `Wily <http://go/wily>`_*

Cache Cluster
~~~~~~~~~~~~~

**∆** *If you are using self-service shared cache cluster STOP! You should talk to the cache team to get allocation of dedicated cache cluster instead. State your dedicated cache cluster info here.*

Proxy Settings
~~~~~~~~~~~~~~

**∆** *If your service has egress traffic using shared proxy servers STOP! You should talk to CISS team to get yourself a dedicated cluster of proxy for your service. State your dedicated proxy setting and account related info here.*

Deployment
----------

Build
~~~~~

**∆** *Procedure for building your dev, staging and production environment.*

Deploy
~~~~~~

**∆** *Procedure for building your dev, staging and production environment.*

Rollback
~~~~~~~~

**∆** *Fast roll back instruction.*

Restart
~~~~~~~

**∆** *Procedure for restarting your dev, staging and production environment.*

Troubleshooting
---------------

**∆** *Common failure cases, identifying issues with dependencies. Consider linking to a troubleshooting guide or a FAQ.*

Logs
~~~~

**∆** *Log location and format.*

LogLens
~~~~~~~

**∆** *LogLens filter if applicable.*

*For more details, see `LogLens <http://go/loglens>`_.*

Performance Debugging
~~~~~~~~~~~~~~~~~~~~~

Memory
^^^^^^

http://go/heapdump, Yourkit, or similar.

GC Logs
^^^^^^^

**∆** *Describe how to get to the GC logs for your service.*

Resiliency
----------

**∆** *Explain the resiliency mechanisms in your service. For example:*

- *Does your server shutdown gracefully?*
- *Have you tested your multi-DC failover logic?*
- *Is there rack diversity among the machines or shards allocated to your service?*
- *Does your service have a "safe mode" or greaceful feature degradation capabilities?*
- *Are you using `Resolver` which has `StabilizingAddr` with `newZk` for Zookeeper?*
- *Have you enabled Zookeeper read-only support to protect your app and Zookeeper?*

Backoff/Retry Logic for Dependency or Network Failure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**∆** *Describe your backoff/retry logic in your service that accounts dependency or network failures.*

Back-Pressure Signal Handling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**∆** *Describe how your service gracefully handle back pressure signals.*

Additional Notes
----------------

**∆** *Enter any additional information you consider useful. For example, if you have other alerts besides Nagios, list them here.*