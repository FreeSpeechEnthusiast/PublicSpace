∆Service Name∆ Service
======================

.. tip::

   If you have questions about this template, feel free to visit the `#techdocs
   <http://go/slack/techdocs>`_ channel on Slack.

.. admonition:: For service documentation authors

   Please use the sections in this template as a basic guideline to create
   documentation for your backend service. Feel free to remove any sections that
   don't apply to you

   Here are some example service docs that may be helpful along the way:

   * `TimelineService <http://go/docbird/timelineservice>`_
   * `Tweetypie <http://go/tweetypie>`_
   * `Gizmoduck <http://go/gizmoduck>`_
   * `Abuse Triage Service <http://go/docbird/abuse_triage_service>`_

Client
------

**∆** *If other engineers can react with your service using a client, tell them how in
this section. Perhaps start with telling users how to instantiate a client*:

.. code-block:: scala

   val wilyPath = "/s/policykeeper/policykeeper"
   val client = ThriftMux.newClient(wilyPath)

API
---

**∆** *What kind of API does your service expose? Is it Thrift? HTTP? Both? Make sure
to describe your service's API, include the Thrift service definition if it's a
Thrift service, provide `REST API docs <http://go/docbird-rest-api>`_, etc.*

Architectural Diagram
---------------------

**∆** *Many services interact with lots of other services in very complex ways. An
architectural diagram for your service may help. Here's a basic example*:

.. graphviz::

   digraph policykeeper {
     Tweetypie -> policykeeper -> Manhattan;
     policykeeper -> MySQL;
   }

Observability and Monitoring
----------------------------

**∆** *What does your service use for monitoring and observability? Is there a
dashboard that users can access?*

Runbook
-------

The runbook for the *∆Service Name∆* Service is located at: `go/∆service-name∆-runbook <http://go/∆service-name∆-runbook>`_.

**∆** *If your service has a runbook that's separate from this service documentation,
link to it here.*

*If your service has a simple runbook, include it as part of this document.*