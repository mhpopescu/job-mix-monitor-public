#!/bin/bash

echo "Running util server. Logs are in \$SPIN_DIRECTORY/volumes/util"
python eval_jobmix.py -s db.properties -vv >> log/log 2>&1
