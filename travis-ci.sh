#!/usr/bin/env bash

# Note that travis_ci_test db should be created by now

printf "Running travis ci script\n"

psql -c "ALTER USER travis WITH PASSWORD 'travis';"

touch priv.properties
echo "TEST_DB=jdbc:postgresql:travis" >> priv.properties
echo "TEST_DRIVER=org.postgresql.Driver" >> priv.properties
echo "TEST_DB_USER=travis" >> priv.properties
echo "TEST_DB_PASSWORD=travis" >> priv.properties

printf "Running gradle test\n"

chmod +x ./gradlew
./gradlew test