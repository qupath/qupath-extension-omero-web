#!/bin/sh

cd /
tar -xvkf /tmp/OMERO.tar.gz

export JAVA_HOME=/usr/lib/jvm/jre
cd /resources/omero-ms-pixel-buffer/
bin/omero-ms-pixel-buffer &