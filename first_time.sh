#!/bin/bash
# do not run this with sudo, because it would create dirs with root owner and we do not want this
# also this why we have to create these directories manually, because docker-compose would create them, but with root

source .env
mkdir -p $VOLUMES_DIR
cd $VOLUMES_DIR
mkdir grafana/ influxdb/ job-mix-monitor/ util/
cd -

# https://github.com/bitnami/bitnami-docker-influxdb
# https://docs.influxdata.com/influxdb/v1.8/administration/authentication_and_authorization/#authorization

# Build and run container
docker-compose up -d --build db

# Initialase database - modifications are persistent because we mounted db path in docker-compose.yml
# - $VOLUMES_DIR/influxdb:/var/lib/influxdb
docker-compose run \
      -e INFLUXDB_DB=jobmix \
      -e INFLUXDB_ADMIN_USER=admin -e INFLUXDB_ADMIN_PASSWORD=1234 \
      -e INFLUXDB_USER=jobmix -e INFLUXDB_USER_PASSWORD=1234 -e INFLUXDB_DB=jobmix\
      -v $VOLUMES_DIR/influxdb:/var/lib/influxdb \
      db /init-influxdb.sh

docker exec  influxdb-db influx -execute 'show databases'
