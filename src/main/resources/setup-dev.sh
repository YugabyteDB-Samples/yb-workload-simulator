#!/bin/sh

if [ "$EUID" -ne 0 ]
then
	echo "Please run this script as root"
	exit 1
fi

if which yugabyted > /dev/null 2>&1; then
	echo "Yugabyted found..."
else
	echo "No yugabyted found in path, please add it"
	exit 1
fi


# configure additional nodes on the loopback adapter
echo "-- Configure loopback adapter to have multiple addresses --"
sudo ifconfig lo0 alias 127.0.0.2
sudo ifconfig lo0 alias 127.0.0.3

echo "-- Starting yugabyted with master port of 9998 --"
yugabyted start --master_webserver_port=9998 --listen=127.0.0.1 --base_dir=/tmp/ybd1
if [ "$?" -ne 0 ] 
then
	echo "Starting yugabyted seems to have failed"
	exit 1
fi
yugabyted start --listen=127.0.0.2 --base_dir=/tmp/ybd2 --join=127.0.0.1
if [ "$?" -ne 0 ] 
then
	echo "Starting yugabyted on 127.0.0.2 seems to have failed"
	exit 1
fi
yugabyted start --listen=127.0.0.3 --base_dir=/tmp/ybd3 --join=127.0.0.1
if [ "$?" -ne 0 ] 
then
	echo "Starting yugabyted on 127.0.0.3 seems to have failed"
	exit 1
fi



