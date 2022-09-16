#!/bin/sh

if [ "$#" -ne 1 ]
then
	echo "Please specify the server name as the argument"
	exit -1
fi

scp ../../../../target/yb*.jar yugabyte@$1:/home/yugabyte/demo

