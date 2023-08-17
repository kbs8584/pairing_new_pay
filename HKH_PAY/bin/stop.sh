#!/bin/sh 
kill -9 `cat < api.pid`
rm api.pid
rm -rf .vertx
