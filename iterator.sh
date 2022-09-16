#!/bin/sh

for ((i=0; i<=1200;i+=50));
do
	echo "Running with delay set to $i"
	delay=$((100000/(i+1)))
	if [ $i -eq 0 ]
	then
		delay = 10000
	fi

	echo "$i - $delay"
	java -Dspring.datasource.hikari.data-source-properties.serverName=127.0.0.1 -Dspring.datasource.hikari.maximumPoolSize=1 -Dspring.workload="NewFormatWorkload" -DworkloadType="RUN_SIMULATION" -Dparams="$delay,$i,1" -DloggingDir="/tmp/logs" -jar target/yb-workload-simu-app.jar
done

