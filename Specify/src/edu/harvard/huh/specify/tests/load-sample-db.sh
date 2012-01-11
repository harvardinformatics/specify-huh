#!/bin/sh

MYSQL=/Users/lchan/mysql/bin/mysql
HOSTNAME=127.0.0.1
PORT=3307
USERNAME=specify
PASSWORD=password
DATABASE=testfish
SQL_FILE=/Users/lchan/testfish.sql

$MYSQL --host=$HOSTNAME --port=$PORT -u $USERNAME -p$PASSWORD $DATABASE < $SQL_FILE