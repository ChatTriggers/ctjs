#!/bin/bash

# Run
trap "exit 0;" TERM INT; httpd -v -p 8081 -h /www -f & wait
