# eMOP Controller

The Brazos Cluster controller process for the eMOP workflow.

## Build

Step #1 is specific to the Brazos cluster and can be skipped if you have maven available.

1. Load maven module.

        module load gcc java maven

2. Build the package by running the command `mvn package`.  This generates a tar.gz file under the 'target' directory.

## Setup

Depends on .my.cnf for MySql connection parameters

Depends on several environment variables as well. They are:

* JUXTA_HOME - Root directory for JuxtaCL
* RETAS_HOME - Root directory for RETAS
* SEASR_HOME - Root directory for SEASR post-processing tools

For multiple users to run this controller on the same file structure the umask must be set to at least 002.

Add the following to your login scripts such as ~/.bashrc

    umask 002

## Support

Only the following versions of each dependency have been tested.

* gcc/4.8.2
* maven/3.2.1
* java/1.7.0_67
* tesseract
