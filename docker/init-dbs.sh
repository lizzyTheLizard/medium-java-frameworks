#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	CREATE DATABASE keycloak;
	GRANT ALL PRIVILEGES ON DATABASE keycloak TO "$POSTGRES_USER";
	CREATE DATABASE application;
	GRANT ALL PRIVILEGES ON DATABASE application TO "$POSTGRES_USER";
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "application" <<-EOSQL
	CREATE TABLE author (
    id varchar(256) NOT NULL,
    firstname varchar(256) NOT NULL,
    lastname varchar(256) NOT NULL,
    PRIMARY KEY (id)
	);

	CREATE TABLE post (
    id varchar(256) NOT NULL,
    author_id varchar(256) NOT NULL,
    content text NOT NULL,
    summary varchar(100) NOT NULL,
    title varchar(256) NOT NULL,
    updated timestamp NOT NULL,
    created timestamp NOT NULL,
    PRIMARY KEY (id),
 	  CONSTRAINT fk_author FOREIGN KEY(author_id) REFERENCES author(id)
 	);
EOSQL

