### Job Mix Monitor

**Purpose:**
Monitor the mix of jobs types(_sim_, _train_, _reco_, _other_) running on one or more identified grid sites, tabulate average usage for _rss_ and _vmem_.

This repository is intended to be used easily both locally with docker and on spin using rancher (based on docker).

#### Implementation
Rancher wants services to be named app, util, web, db.

There are 4 containers:
* app - job-mix-monitor
Simple MLclient code (src/JobMixClient.java) registers with sites and queries a specific set of information. That information is periodically updated. This client gets data from multiple sites. All the data from one site is send as a post request to util container to be parsed.
* util - job-mix 
Python script to receive data from job-mix-monitor. This is a flask server.
This service would parse data from job-mix-monitor and send it to db
* db
Influx db database to store the data from job-mix
* web
Grafana server

#### Running locally
```
./first_time.sh
```
Run this script only once after you clone this repo. It would create `~/spin` directory where logs, db, and grafana configuration is saved. It would also initialise the database with a db name, user and password. These values should be identical with the ones from `util/db.properties`. Also a similar initialisation is in the file `db/influx.iql`, but that is not currently used

##### Run whole stack with:
```docker-compose up -d```

If you modify any source files that are not mounted as volumes you have to manually specify docker to rebuild the images:
```docker-compose up -d --build```

To set up grafana log in on `localhost:3000` with `admin:admin`. Go to Data Sources -> Add Data Source and fill URL with `db:8086` and database, user and password. Then go to Create (plus symbol below search) -> Import -> Upload JSON -> select `Plots.json` from current directory

To check for errors: \
`docker-compose logs` \
To go to the influxdb terminal: \
`docker exec  influxdb-db influx`

#### Running on spin
https://docs.nersc.gov/services/spin/getting_started/lesson-1/
```
cd job-mix; 
docker image build --tag registry.spin.nersc.gov/mihaip/job-mix-parser-util:$utilVersion .  # build image
docker image push registry.spin.nersc.gov/mihaip/job-mix-parser-util:$utilVersion    # push image

```

```
source env.settings
mkdir -p $SPIN_VOL/grafana $SPIN_VOL/influxdb $SPIN_VOL/job-mix-monitor $SPIN_VOL/util
find $SPIN_VOL -type d -exec chmod o+x {} +
rancher up -d --file docker-compose.yaml
rancher ps
```

#### Logs
Log files are located in "log" directory.

Code will run in background. MLclient and python scripts will be run. If everything is right the output in every file should be:
* [eval_jobmix] app started
* [eval_jobmix] wait for data

And after a while, when JobMixClient starts to write data:
* [eval_jobmix] started reading

If something wrong happens, the script will write the line written and the stacktrace in its log file.

JobMixClient log file is 
* log/JobMixClient for monalisa logs
* app-`date` for what you would like to log from you app

####About data
jobmix, sim=69.8 train=26.0 daq=0.1 other=4.1

jobrss, sim_rss=1.00 train_rss=1.05 daq_rss=0.00 other_rss=0.55 all_rss=0.99

jobvmem, sim_vmem=2.38 train_vmem=2.59 daq_vmem=0.00 other_vmem=1.59 all_vmem=2.40

This says, 69.8% of jobs are simulation, with average rss of 1.0GB and vmem of 2.38GB.  And so one. _other_ are all individual users.

* User jobs data taken from logs
```
FINEST:  -->    mgagliar         Site_UserJobs_Summary   LBL_HPCS        Time = [ 1603400771416/Thu Oct 22 21:06:11 UTC 2020 ]     * rss =      0.0     * virtualmem =  0.0     * cpu_time =    0.0     * run_time =    62.0     * count =      1.0     * workdir_size =        3.71875
Oct 22, 2020 9:06:11 PM JobMixClient$MyDataReceiver addResult
FINEST:  -->    prchakra         Site_UserJobs_Summary   LBL_HPCS        Time = [ 1603400771416/Thu Oct 22 21:06:11 UTC 2020 ]     * rss =      1.0155752E7     * virtualmem =  2.3690504E7     * cpu_time =    1429.0     * run_time =         1471.0     * count =    13.0     * workdir_size =       16.59375
```

* Eos data taken from logs
```
FINEST:  -->    eos-fst-storage8.grid.pub.ro     ALICE::UPB::EOS_xrootd_Nodes    UPB     Time = [ 1603401380871/Thu Oct 22 21:16:20 UTC 2020 ]     * TOTAL_NET_IN =     1753.5404915679496     * TOTAL_NET_OUT = 
       4133.739569444011
```