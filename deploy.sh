#!/bin/bash

utilVersion="v1.1.3"
appVersion="v1.2"

# cd job-mix; 
# docker image build --tag registry.spin.nersc.gov/mihaip/job-mix-parser-util:$utilVersion .
# docker image push registry.spin.nersc.gov/mihaip/job-mix-parser-util:$utilVersion
# cd ..

cd job-mix-monitor
docker image build --tag registry.spin.nersc.gov/mihaip/job-mix-monitor-app:$appVersion .
docker image push registry.spin.nersc.gov/mihaip/job-mix-monitor-app:$appVersion
cd ..