#!/bin/sh


cd /home/hkh/HKH_PAY/bin

pid_file="api.pid"

now=$(date +"%Y-%m-%d:%H:%M:%S");

value=`cat $pid_file`

if [ -f "$pid_file" ] 
then

        if ps -p $value > /dev/null
                then
                echo "$now : $pid_file $value is running"
        else
                echo "$now : $pid_file $value was stop "
                ./start.sh
        fi
else
        echo "$now : $pid_file is not exist"
        ./start.sh
fi
