# eMOP Controller

[![Documentation Status](https://readthedocs.org/projects/emop-controller/badge/?version=latest)](https://readthedocs.org/projects/emop-controller/?badge=latest)
[![Build Status](https://travis-ci.org/Early-Modern-OCR/emop-controller.svg?branch=master)](https://travis-ci.org/Early-Modern-OCR/emop-controller)
[![Coverage Status](https://coveralls.io/repos/Early-Modern-OCR/emop-controller/badge.svg?branch=master&service=github)](https://coveralls.io/github/Early-Modern-OCR/emop-controller?branch=master)

The Brazos Cluster controller process for the eMOP workflow. For more information on the eMOP workflow, please see the [eMOP Workflow Design Description](http://emop.tamu.edu/workflows/eMOP-DD)

#### Table of Contents

1. [Install](#install)
2. [Build](#build)
3. [Setup](#setup)
4. [Usage](#usage)
  * [Query](#query)
  * [Submitting](#submitting)
  * [Uploading](#uploading)
  * [Transfer Status](#transfer-status)
  * [Transfer In](#transfer-in)
  * [Transfer Out](#transfer-out)
  * [Transfer Test](#transfer-test)
  * [Test Run](#test-run)
  * [Cron](#cron)
5. [Support](#support)
6. [Development](#development)

## Install

Clone this repository and merge in the submodules

    git clone git@github.tamu.edu:emop/emop-controller.git
    cd emop-controller
    git submodule update --init

## Build

Step #1 is specific to the Brazos cluster and can be skipped if you have maven available.

1. Load emop-build module.

        module use ./modulefiles
        module load emop-build

2. Build and install all necessary dependencies.

        make all

3. Unload the emop-build module.

        module unload emop-build

## Setup

Depends on several environment variables as well. They are:

* TESSDATA_PREFIX - Path to the Tesseract training data
* JUXTA_HOME - Root directory for JuxtaCL
* RETAS_HOME - Root directory for RETAS
* SEASR_HOME - Root directory for SEASR post-processing tools
* DENOISE_HOME - Root directory for the DeNoise post-processing tool

For multiple users to run this controller on the same file structure the umask must be set to at least 002.

Add the following to your login scripts such as ~/.bashrc

    umask 002

Rename the following configuration files and change their values as needed:

* ~~emop.properties.example to emop.properties~~
* config.ini.example to config.ini

The file `config.ini` contains all the configuration options used by the emop-controller.

~~The file `emop.properties` is legacy and currently only used by the PageCorrector post-process.~~

## Usage

All interaction with the emop-controller is done through `emopcmd.py`.  This script has a set of subcommands that determine the operations performed.

* query - form various queries against API and local files
* submit - submit jobs to the cluster
* run - run a job
* upload - upload completed job results
* transfer status - check status of ability to transfer or of specific task
* transfer in - transfer files from remote location to cluster
* transfer out - transfer files from cluster to remote location
* transfer test - transfer test file from cluster to remote location
* testrun - Reserve, run and upload results.  Intended for testing.

For full list of options execute `emopcmd.py --help` and `emopcmd.py <subcommand> --help`.

Be sure the emop module is loaded before executing emopcmd.py

    module use ./modulefiles
    module load emop

**NOTE**: The first time a the controller communicates with Globus Online a prompt will ask for your Globus Online username/password.  This will generate a GOAuth token to authenticate with Globus.  The token is good for one year.

### Query

The following is an example of querying the dashboard API for count of pending pages (job_queues)

    ./emopcmd.py query --pending-pages

This example will count pending pages (job_queues) that are part with batch_id 16

    ./emopcmd.py query --filter '{"batch_id": 16}' --pending-pages

The log files can be queried for statistics of application runtimes.

    ./emopcmd.py query --avg-runtimes

### Submitting

This is an example of submitting a single page to run in a single job:

    ./emopcmd.py submit --num-jobs 1 --pages-per-job 1

This is an example of submitting and letting the emop-controller determine the optimal
number of jobs and pages-per-job to submit:

    ./emopcmd.py submit

The `submit` subcommand can filter the jobs that get reserved via API by using the `--filter` argument.  The following example would reserve job_queues via API that match batch_id 16.

    ./emopcmd.py submit --filter '{"batch_id": 16}'

### Uploading

This example is what is used to upload data from a SLURM job

    ./emopcmd.py upload --proc-id 20141220211214811

This is an example of uploading a single file

    ./emopcmd.py upload --upload-file payload/output/completed/20141220211214811.json

This is an example of uploading an entire directory

    ./emopcmd.py upload --upload-dir payload/output/completed

### Transfer Status

To verify you are able to communicate with Globus and endpoints are currently activated for your account

    ./emopcmd.py transfer status

To check on the status of an individual transfer

    ./emopcmd.py transfer status --task-id=<some task ID>

### Transfer In

To transfer files from a remote location to the local cluster

    ./emopcmd.py transfer in --filter '{"batch_id": 16}'

The Globus Online task ID will be printed once transfer is successful received by Globus.

### Transfer Out

To transfer files from the local cluster to a remote location

    ./emopcmd.py transfer out --proc-id 20141220211214811

The Globus Online task ID will be printed once transfer is successful received by Globus.

### Transfer Test

This subcommand will send a test file from the local cluster to a remote endpoint to verify transfers are working and will wait
for success or failure.

    ./emopcmd.py transfer test --wait

### Test Run

The subcommand `testrun` is available so that small number of pages can be processed interactively
from within a cluster job.

First acquire an interactive job environment.  The following command is specific to Brazos and requests
an interactive job with 4000MB of memory.

    sintr -m 4000

Once the job starts and your on a compute node you can must load the emop modules.  These commands are also
specific to Brazos using Lmod.

    # Change to directory containing this project
    cd /path/to/emop-controller
    module use modulefiles
    module load emop

The following example will reserve 2 pages, process them and upload the results.

    ./emopcmd.py testrun --num-pages 2

You can also run `testrun` with uploading of results disabled.

    ./emopcmd.py testrun --num-pages 2 --no-upload

The same page can be reprocessed with a little bit of work.

First set the PROC_ID to the value that was output during the testrun:

    export PROC_ID=<some value>

Then use subcommand `run` with the PROC_ID of the previous run and `--force-run` to overwrite previous output.  This will read the input JSON file and allow the same page(s) to be processed

    ./emopcmd.py run --force-run --proc-id ${PROC_ID}

### Cron

To submit jobs via cron a special wrapper script is provided

Edit cron by using `crontab -e` and add something like the following:

    EMOP_HOME=/path/to/emop-controller
    0 * * * * $EMOP_HOME/scripts/cron.sh config-cron.ini ; $EMOP_HOME/scripts/cron.sh config-cron-background.ini

The above example will execute two commands every hour.  The first launches `emopcmd.py -c config-cron.ini submit` and the second launches `emopcmd.py -c config-cron-background.ini submit`.

## Support

The use of this application relies heavily on sites using the following technologies

* SLURM - cluster resource manager
* Lmod - Modules environment

Only the following versions of each dependency have been tested.

* python-2.7.8
  * requests-2.5.0
  * subprocess32-3.2.6
* maven-3.2.1
* java-1.7.0-67
* tesseract - SVN revision 889

See `modulefiles/emop.lua` and `modulefiles/emop-build.lua` for a list of all the applications used

## Development

The following Python modules are needed for development

* flake8 - lint tests
* sphinx - docs creation

Install using the following

    pip install --user --requirement .requirements.txt

### Lint tests

Running lint tests

    flake8 --config config.ini .

### Documentation

Build documentation using Sphinx

    make docs

### System tests

Verify transfers can be initiated and possibly update config to appropriate values

    ./emopcmd.py -c tests/system/config-idhmc-test.ini transfer status

To run the test using background-4g partition:

    sbatch -p idhmc tests/system/emop-ecco-test.slrm
    sbatch -p idhmc tests/system/emop-eebo-test.slrm

Check the output of `logs/test/emop-controller-test-JOBID.out` where JOBID is the value output when sbatch was executed.
