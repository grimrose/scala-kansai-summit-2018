# clean
> sbt clean

    sbt clean

# clean-identity
> clean identity http target

    sbt identityHttp/clean

# clean-message
> clean message http target

    sbt messageHttp/clean

# fmt
> scalafmt

    sbt scalafmtSbt scalafmt test:scalafmt

# updates
> show a list of project dependencies that can be updated

    sbt dependencyUpdates

# compile
> compile

    sbt compile

# test-compile
> compile test classes

    sbt test:compile

# build-testbed
> build testbed

    docker-compose -f docker-compose.test.yml build

# test
> testing

    saku build-testbed
    saku test-migrate
    docker-compose -f docker-compose.test.yml run --rm testbed sbt "testOnly *"

# test-identity
> testing identity modules

    docker-compose -f docker-compose.test.yml run --rm testbed sbt "identity/testOnly *"

# test-message
> testing message modules

    saku test-migrate
    docker-compose -f docker-compose.test.yml run --rm testbed sbt "message/testOnly *"

# test-migrate
> migrate test database

    docker-compose -f docker-compose.test.yml run --rm flyway migrate

# test-down
> shutdown test containers

    docker-compose -f docker-compose.test.yml down --remove-orphans -v

# sbt
> develop in docker

    docker-compose -f docker-compose.test.yml run --rm testbed sbt

# coverage
> coverage

    saku clean
    saku test-migrate
    docker-compose -f docker-compose.test.yml run --rm testbed sbt coverage "testOnly *" coverageReport
    docker-compose -f docker-compose.test.yml run --rm testbed sbt coverageAggregate


# stage-identity
> stage identity http

    sbt identityHttp/docker:stage

# stage-identity-cli
> stage identity cli

    sbt identityCli/stage

# stage-message
> stage message app

    sbt messageHttp/docker:stage

# stage-message-cli
> stage message cli

    sbt messageCli/stage

# stage
> stage akka app for docker

    saku stage-identity
    saku stage-message

# build-identity
> build identity service docker images

    saku stage-identity
    docker-compose build identity

# build-message
> build message service docker images

    saku stage-message
    docker-compose build message

# docker-compose-config
> validate docker-compose.yml

    docker-compose config

# docker-compose-build
> build docker images

    docker-compose build

# build
> build apps and images

    saku stage
    saku docker-compose-build

# up
> up

    docker-compose up

# start
> start all services

    saku build
    saku up

# identity
> start identity service

    saku build-identity
    saku up-identity

# up-identity
> up identity service

    docker-compose up identity

# message
> start message service

    saku build-message
    docker-compose up message

# postgres
> start postgres

    docker-compose up -d postgres

# test-pull
> pull

    docker-compose -f docker-compose.test.yml pull

# pull
> pull

    saku test-pull
    docker-compose pull

# logs
> logging

    docker-compose logs

# fluentd
> fluentd logs

    docker-compose logs fluentd

# ps
> ps

    docker-compose ps

# stop
> stop

    docker-compose stop

# up-nginx
> up nginx

    docker-compose up nginx

# nginx-conf

    docker-compose run --rm nginx nginx -t -c /etc/nginx/nginx.conf

# nginx-status

    pipenv run http :8080/stub_status

# down
> shutdown all services

    docker-compose down --remove-orphans -v
    saku test-down

# flyway-info
> show flyway info

    docker-compose run --rm flyway info

# migrate
> migrate database

    docker-compose run --rm flyway migrate
    saku flyway-info

# ping-identity

    pipenv run http :9001

# ping-message

    pipenv run http :9002

# post-message

    pipenv run http POST :8080/messages

# show-messages

    pipenv run http GET :8080/messages

# get-identity

    pipenv run http :8080/identity

# not-found

    pipenv run http :8080/hoge

# jaeger-logs

    docker-compose logs jaeger
