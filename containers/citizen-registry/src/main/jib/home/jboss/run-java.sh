#!/bin/sh

echo starting quarkus

java \
  -Xmx128m  \
  -Dquarkus.http.host=0.0.0.0 \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  -jar quarkus-run.jar