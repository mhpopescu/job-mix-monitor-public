#!/bin/bash

# Can not create secrets to a custom directory, so this file has to be 
# different than the local run.sh


echo "Running util server. Logs are in \$SPIN_DIRECTORY/volumes/util"
python /app/eval_jobmix.py -s /run/secrets/db.properties -vv >> log/log 2>&1
