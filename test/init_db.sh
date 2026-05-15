#!/bin/bash
# test/init_db.sh - Initialize the database for integration tests

echo "Initializing database..."
# The sql command should be in the PATH from the previous step in CI
sql -L -s system/password@127.0.0.1:1521/FREEPDB1 < test/init_db.sql

if [ $? -eq 0 ]; then
    echo "Database initialized successfully."
else
    echo "Error: Database initialization failed."
    exit 1
fi
