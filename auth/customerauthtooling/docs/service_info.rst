∆Service Name∆ Service
======================

.. admonition:: ∆ Template Instructions
  :class: template

   **Using Templates**

   Before publishing this page:

   + Replace the italicized text with your own content.
   + Remove the template instruction boxes.

   The **∆** symbol indicates that you should replace or remove text.

   **Service Guidelines**

   Use the sections in this template as a basic guideline to create documentation for your backend service. Remove any sections that don’t apply to you.

   **Template Resources**

   If you have questions about this template, contact the `#techdocs <http://go/slack/techdocs>`_ channel on Slack.

   For more information about best practices for service pages, see the examples listed below and `go/techdocs-style-guide <http://go/techdocs-style-guide>`_.
   
   * `TimelineService <http://go/docbird/timelineservice>`_
   * `Tweetypie <http://go/tweetypie>`_
   * `Gizmoduck <http://go/gizmoduck>`_
   * `Abuse Triage Service <http://go/docbird/abuse_triage_service>`_

.. note::

  This page can be reached at `go/∆service name∆ <http://go/%E2%88%86service-name%E2%88%86>`_. 

Client
------

.. admonition:: ∆ Template Instructions
  :class: template

  If other engineers can interact with your service using a client, tell them how in this section. Perhaps start with telling users how to instantiate a client. See example below.

.. code-block:: scala

   val wilyPath = "/s/customerauthtooling/customerauthtooling"
   val client = ThriftMux.newClient(wilyPath)

API
---

.. admonition:: ∆ Template Instructions
  :class: template

  What kind of API does your service expose? Is it Thrift? HTTP? Both? Make sure to describe your service's API, include the Thrift service definition if it's a Thrift service, provide `REST API docs <http://go/docbird-rest-api>`_.

Architectural Diagram
---------------------

.. admonition:: ∆ Template Instructions
  :class: template

  Many services interact with lots of other services in very complex ways. An architectural diagram for your service may help. See below for a basic example.

.. code-block:: graphviz

   .. graphviz::

   digraph customerauthtooling {
     Tweetypie -> customerauthtooling -> Manhattan;
     customerauthtooling -> MySQL;
   }

Observability and Monitoring
----------------------------

.. admonition:: ∆ Template Instructions
  :class: template

  What does your service use for monitoring and observability? If there's a dashboard that users can access, add that info to this section.

Runbook
-------

The runbook for the *∆Service Name∆* Service is located at: `go/∆service-name∆-runbook <http://go/∆service-name∆-runbook>`_.

.. admonition:: ∆ Template Instructions
  :class: template
   
  If your service has a runbook that's separate from this service documentation, link to it here. If your service has a simple runbook, include it as part of this document.