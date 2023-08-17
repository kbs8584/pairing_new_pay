#!/bin/sh

unset LANG

# LANG
##########################
LANG=ko_KR.utf8
export LANG

# SET LIBRARY
for i in ../lib/*.jar; do
    CP=$CP:$i
done
CP=`echo $CP | cut -c2-`


# JVM_ARGS for VM
##########################
JVM_ARGS="-Du=HKH_PAY -DCP_CONF=../conf -Dfile.encoding=utf-8 -Dlogback.configurationFile=../conf/logback.xml"
JVM_ARGS="$JVM_ARGS -Dorg.vertx.logger-delegate-factory-class-name=org.vertx.java.core.logging.impl.SLF4JLogDelegateFactory "
JVM_ARGS="$JVM_ARGS -Xss512k -Xms256m -Xmx512m "
JVM_ARGS="$JVM_ARGS -cp $CP:../war"
java $JVM_ARGS com.pgmate.pay.main.C3Runner &
echo $!>api.pid
