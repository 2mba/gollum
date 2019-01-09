FROM openjdk:8-jre

ENV MYSQL_USER=mysql \
    MYSQL_VERSION=8.0.13 \
    MYSQL_DATA_DIR=/var/lib/mysql \
    MYSQL_RUN_DIR=/run/mysqld \
    MYSQL_LOG_DIR=/var/log/mysql

RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server=${MYSQL_VERSION}* \
 && rm -rf ${MYSQL_DATA_DIR} \
 && rm -rf /var/lib/apt/lists/*

COPY entrypoint.sh /sbin/entrypoint.sh
RUN chmod a+x /sbin/entrypoint.sh

COPY script.sh script.sh
RUN chmod a+x script.sh

COPY ./build/libs/my-application.jar my-application.jar

CMD ./script.sh