FROM openjdk:8

ENV SBT_VERSION 1.2.4

# @see https://github.com/coursier/coursier/issues/450

RUN mkdir -p /root/.sbt/1.0/plugins/ && \
    echo 'addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")' > /root/.sbt/1.0/plugins/coursier.sbt && \
    echo 'classpathTypes += "maven-plugin"' >> /root/.sbt/1.0/plugins/coursier.sbt

RUN curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
    dpkg -i sbt-$SBT_VERSION.deb && \
    rm sbt-$SBT_VERSION.deb && \
    apt-get update && \
    apt-get install sbt

WORKDIR /root
