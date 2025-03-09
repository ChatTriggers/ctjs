#!/bin/bash

# Install deps
sudo apt install httpd

# Run
trap "exit 0;" TERM INT; httpd -v -p 8081 -h /www -f & wait
