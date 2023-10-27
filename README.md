# Flexi-Backend-Clojure

## Introduction

Flexi-Backend-Clojure houses a back-end server made with clojure that contains API endpoints and an authentication system designed to seamlessly integrate with the Flexi Apps stack. For a comprehensive guide on the entire Flexi Apps ecosystem, please refer to the [Flexi Apps Documentation](https://github.com/felipearmat/flexi-apps).

This project was created using [kit-clj](https://kit-clj.github.io/).

## Requirements

Before you begin, make sure you have the following software installed on your computer:

- [Docker](https://docs.docker.com/engine/install/)
- [Postgres](https://www.postgresql.org/download/)

## Application Stack

This project comprises the following components:

- **resources**: The "resources" folder contains migration files and the base system configuration in the system.edn file.

- **src**: The "src" folder contains the source code of the project.

- **env**: The "env" folder contains code and configuration specific to each environment.

- **test**: The "test" folder stores unit and/or integration tests for the project.

- **Dockerfile**: The Dockerfile includes instructions for building a Docker container for this project.

### Database Setup

To launch this application, you will need the Postgres service running with a minimum database schema. Execute the following SQL commands on your Postgres server:

```sql
CREATE USER sample_app WITH PASSWORD 'sample_app';

CREATE DATABASE sample_app;
CREATE DATABASE sample_app_dev;
CREATE DATABASE sample_app_test;

GRANT ALL PRIVILEGES ON DATABASE sample_app TO sample_app;
GRANT ALL PRIVILEGES ON DATABASE sample_app_dev TO sample_app;
GRANT ALL PRIVILEGES ON DATABASE sample_app_test TO sample_app;
```

### Starting the container

To run this application without the need for the front-end and proxy services, execute the following commands:

```bash
docker build . -t flexi-backend-clojure

docker run --rm -d \
  -p 3000:3000 \
  -v ./:/app \
  -e DB_USER="sample_app" \
  -e DB_PASSWORD="sample_app" \
  -e JDBC_URL="jdbc:postgresql://localhost/sample_app" \
  -e JDBC_DEV_URL="jdbc:postgresql://localhost/sample_app_dev" \
  -e JDBC_TEST_URL="jdbc:postgresql://localhost/sample_app_test" \
  --name flexi-backend-clojure \
  flexi-backend-clojure
```

This will start a React container in the background.
Once you access the container, you can [Start a REPL](#start-a-repl) or make requests to its backend endpoints.

### Stoping the container

To stop the container, execute the following command:

```bash
docker stop flexi-backend-clojure
```

### Accessing the container

Once the container is running, you can access it with:

```bash
docker exec -it flexi-backend-clojure /bin/sh
```

Once on the container terminal, you can [Start a REPL](#start-a-repl).

## Start a REPL

After accessing the shell terminal of this project, you need to run the following command to start a REPL:

```sh
clojure -M:dev
# Clojure 1.11.1
# user=>
```

Now you have a Clojure REPL for interacting with the app. The REPL starts without a database connection. To initiate it, run:

```clojure
(dev-prep!)
;; #object[user$dev_prep_BANG_$fn__39701 0x40ba4a5d "user$dev_prep_BANG_$fn__39701@40ba4a5d"]

(prep)
;; 2023-10-18 11:50:11,368 [main] INFO  kit.config - Reading config system.edn
;; :prepped

(use-system :db.sql/connection)
;; 2023-10-18 11:50:17,122 [main] INFO  kit.config - Reading config system.edn
;; #object[com.zaxxer.hikari.HikariDataSource 0x7d6548b4 "HikariDataSource (HikariPool-1)"]
```

To simplify these commands, you can use the following shortcut:

```clojure
(start-repl)
;; 2023-10-16 12:28:29,527 [main] INFO  kit.config - Reading config system.edn
;; 2023-10-16 12:28:29,537 [main] INFO  kit.config - Reading config system.edn
;; #object[kit.edge.db.sql.conman$eval16250$fn__16252$fn__16254 0x483f0877 "kit.edge.db.sql.conman$eval16250$fn__16252$fn__16254@483f0877"]

```

For more information, refer to [Deps and CLI Guide](https://clojure.org/guides/deps_and_cli) for REPL based development and [Integrant-REPL](https://github.com/weavejester/integrant-repl) for state/config and state/system.

### Populating the database

For development purposes, it's a good practice to populate your database with sample data. To do this, access the back-end REPL and run:

```clojure
(seeds)
;; Seeds completed!

```

Please note that this function is only accessible in the **development** REPL.
