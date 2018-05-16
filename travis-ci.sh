#!/usr/bin/env bash

# Note that travis_ci_test db should be created by now

printf "Running travis ci script\n"

touch priv.properties
echo "TEST_DB=jdbc:postgresql:travis_ci_test" >> priv.properties
echo "TEST_DRIVER=org.postgresql.Driver" >> priv.properties
echo "TEST_DB_USER=postgres" >> priv.properties

chmod +x ./gradlew
./gradlew test