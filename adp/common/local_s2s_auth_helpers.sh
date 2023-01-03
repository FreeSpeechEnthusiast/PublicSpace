#!/bin/bash
# Helper functions to support s2s auth in scripts that are run locally like REPLs

# Creates a developer certificate for use in local scripts that talk to our services
# in Aurora.
#
# Usage: call the function and provide a service name
# Example: create_local_s2s_auth_cert dal-repl
#          This will create a developer cert for the "dal-repl" service.
#
# In your script, you will need to create a ServiceIdentifier with the same service
# name.
# Example
# import com.twitter.finagle.mtls.authentication.ServiceIdentifier
# import com.twitter.finagle.mtls.client.MtlsStackClient._
#
# val userName = System.getProperty("user.name")
# ServiceIdentifier(
#      role = userName,
#      service = "dal-repl",
#      environment = "devel",
#      zone = "local"
#    )
create_local_s2s_auth_cert() {
    SERVICE_NAME=$1
    if [ ! -z "$SERVICE_NAME" ]; then
      CERT_PATH="/Users/$USER/.s2s/local/devel/$SERVICE_NAME/$USER"
    	if [ ! -d "$CERT_PATH" ]; then
    	  if command -v developer-cert-util &> /dev/null; then
          echo "Creating a developer cert for the $SERVICE_NAME"
          developer-cert-util --local --job "$SERVICE_NAME"
        else
          echo "The 'developer-cert-util' command was not found, so an s2s auth developer cert "\
          "could not be created. Please install the command using the instructions found here: "\
          "https://docbird.twitter.biz/service_authentication/howto/credentials.html#developer-certs"
          exit
        fi
      else
          echo "Developer cert for $SERVICE_NAME exists at $CERT_PATH. Skipping creation."
      fi
    else
        echo "Service name was not specified. Skipping creation of developer cert."
    fi
}
