∆Product Name∆ Runbook
======================

.. admonition:: ∆ Template Instructions
  :class: template

   **Using Templates**

   Before publishing this page:

   + Replace the italicized text with your own content.
   + Remove the template instruction boxes.

   The **∆** symbol indicates that you should replace or remove text.

   **Runbook Guidelines**

   A runbook provides a set of procedures for accomplishing specific tasks or troubleshooting particular issues.

   `TCC (Twitter Command Center) <http://go/tcc>`_ must review a product runbook to approve its `Production Readiness Review <http://go/prr>`_. Follow the format to improve PRR quality.

   **Template Resources**

   If you have questions about this template, contact the `#techdocs <http://go/slack/techdocs>`_ channel on Slack.

   For more information about best practices for service pages, see `go/modelrunbook <http://go/modelrunbook>`_, `go/runbookstyleguide <http://go/runbookstyleguide>`_, and `go/techdocs-style-guide <http://go/techdocs-style-guide>`_.

.. note::

  This page can be reached at `go/∆product-name-runbook∆ <http://go/%E2%88%86product-name-runbook%E2%88%86>`_. 
 

.. admonition:: ∆ Template Instructions
  :class: template

  Briefly describe the features and functions of the product. Briefly describe the system architecture.



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

.. admonition:: ∆ Template Instructions
  :class: template

  Create a high level overview of your product's architecture, including your service and dependencies. Include a diagram, if available.

Dependencies
------------

.. admonition:: ∆ Template Instructions
  :class: template

  Create a subheading (heading-3 ``~``) for each service your service depends on to function properly. Under each heading, briefly describe the dependency, and mention failure modes - what will happen if the dependency were to fail.

Example dependency: Manhattan
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This service uses Manhattan to:

- foobars: creates baz. 200 write max, 100 pkey read max, and 1000 lkey read max.

Failure Modes
^^^^^^^^^^^^^

- Feature baz will stop working.

Dependents
----------

.. admonition:: ∆ Template Instructions
  :class: template

  Create a subheading (heading-3 ``~``) for each service that depends on your service. Under each heading, briefly describe that dependent, and mention failure modes - what will happen if your service fails.

Capacity
--------

Thresholds
~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  List capacity thresholds and corresponding service behavior if thresholds are passed. In the Manhattan example, list thresholds in queries per second (QPS). 

Management
~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Describe how capacity is managed and how more can be added.

SLA
---

.. csv-table::
   :header: Description, SLA

   ∆ *Success rate, > .997* ∆ 

Expected Impact When Service in Degradation or Outage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Describe the impact to users, partners, and revenue when the service is running in degraded mode or is down.

Recent Deployments
------------------

.. admonition:: ∆ Template Instructions
  :class: template

  Link to a JIRA filter for the product's Production Change Management (PCM) component.

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

.. admonition:: ∆ Template Instructions
  :class: template

  Link to your dashboard. You should only have ONE OFFICIAL dashboard that is displaying muti-DC charts. For details, see `Monitoring 2.0 <http://go.twitter.biz/docs/mon/getting-started>`_.

Alerts
------

.. admonition:: ∆ Template Instructions
  :class: template

  Link to alerts. Ideally you should have one OFFICIAL alerts dash to show ALL your critical alerts. For details, see `Monitoring 2.0 <http://go.twitter.biz/docs/mon/getting-started>`_.

Remediations
------------

.. admonition:: ∆ Template Instructions
  :class: template

  Create a subheading (heading-3 ``~``) for each alert, and explain the problem, its impact, possible causes, remediations, and investigation detail.

Example Remediation: Free Heap Space is Low on Multiple Machines
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Problem**:

.. admonition:: ∆ Template Instructions
  :class: template

  Provide a summary of the problem indicated by the alert.

**Impact**:

.. admonition:: ∆ Template Instructions
  :class: template

  Provide an explanation of the problem's impact.

**Possible Causes**:

.. admonition:: ∆ Template Instructions
  :class: template

  Provide a list of possible causes.

- Symptom X is caused by...
- Symptom Y is caused by...

**Remediations**:

.. admonition:: ∆ Template Instructions
  :class: template

  Provide a list of potential remediations.

- Immediate remediations: Restart the affected instances, recommended batch size 5 at a time
- Long term remediations: Investigate code performance (can be done during business hours).

**Investigation**:

.. admonition:: ∆ Template Instructions
  :class: template

  Provide a list of steps to take to investigate the problem.
- Check recent code changes for this service or its dependencies for correlation
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

.. admonition:: ∆ Template Instructions
  :class: template
   
  Add Aurora/Mesos configuration and quota. For more details, see the `Aurora docs <http://go/aurora>`_.

Zookeeper
~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Describe the namespaces. For more details, see `Wily <http://go/wily>`_ and `Zookeeper Client Best Practices <https://docbird.twitter.biz/zookeeper_client_best_practices/index.html>`_.

Wily
~~~~

.. admonition:: ∆ Template Instructions
  :class: template
   
  Add Wily name space. For more details, see `Wily <http://go/wily>`_

Cache Cluster
~~~~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  You should not be using a self-service shared cluster. If you are, talk to the cache team to get allocation of a dedicated cache cluster instead. State your dedicated cache cluster info here.

Proxy Settings
~~~~~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template
   
  If your service has egress traffic, using shared proxy servers, talk to the Core Infrastructure Systems SRE (CISS) team to get yourself a dedicated cluster of proxy for your service. State your dedicated proxy setting and account related info here.

Deployment
----------

Build
~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Procedure for building your dev, staging and production environment.

Deploy
~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Procedure for deploying, staging and production environment.

Rollback
~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Fast rollback instruction.

Restart
~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Procedure for restarting your dev, staging and production environment.

Troubleshooting
---------------

.. admonition:: ∆ Template Instructions
  :class: template

  Common failure cases, identifying issues with dependencies. Consider linking to a troubleshooting guide or a FAQ.

Logs
~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Log location and format.

LogLens
~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  LogLens filter if applicable. For more details, see `LogLens <http://go/loglens>`_.

Performance Debugging
~~~~~~~~~~~~~~~~~~~~~

Memory
^^^^^^

.. admonition:: ∆ Template Instructions
  :class: template

  Describe how to debug memory leaks using `Aurora Heap Dump Utility <http://go/heapdump>`_, YourKit, or similar, for your service.

GC Logs
^^^^^^^

.. admonition:: ∆ Template Instructions
  :class: template

  Describe how to get to the GC logs for your service.

Resiliency
----------

.. admonition:: ∆ Template Instructions
  :class: template

  Explain the resiliency mechanisms in your service. For example:
   - *Does your server shutdown gracefully?*
   - *Have you tested your multi-DC failover logic?*
   - *Is there rack diversity among the machines or shards allocated to your service?*
   - *Does your service have a "safe mode" or graceful feature degradation capabilities?*
   - *Are you using `Resolver` which has `StabilizingAddr` with `newZk` for Zookeeper?*
   - *Have you enabled Zookeeper read-only support to protect your app and Zookeeper?*

Backoff/Retry Logic for Dependency or Network Failure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Describe your backoff/retry logic in your service that accounts for dependency or network failures.

Back-Pressure Signal Handling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. admonition:: ∆ Template Instructions
  :class: template

  Describe how your service gracefully handles back-pressure signals.

Additional Notes
----------------

.. admonition:: ∆ Template Instructions
  :class: template

  Enter any additional information you consider useful. For example, if you have other alerts besides Nagios, list them here.