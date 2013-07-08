#!/bin/bash

# this script lives in the root directory of the emop controller
# be sure to cd to this directory no matter how the script was 
# launched 
cd $(dirname $0)

APP_NAME="emop_controller"
USER_NAME=$(whoami)
CMD="qstat -u${USER_NAME}"
Q_STATUS=$($CMD)

if [[ "$Q_STATUS" == *$APP_NAME* ]]
then
  echo "An instance of ${APP_NAME} is already running."
  exit 1
else
  qsub emop.pbs
fi

exit 0
