FROM openjdk:8-jre

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
RUN echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/10gen.list
RUN dpkg-divert --local --rename --add /sbin/initctl
RUN apt-get update
RUN apt-get install -y mongodb-10gen
RUN mkdir -p /mongodb/db
RUN echo 'mongod --fork --dbpath=/mongodb/db --logpath=/mongodb/mongo.log &'

ENV APPLICATION_USER ktor
RUN adduser $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./build/libs/my-application.jar /app/my-application.jar
WORKDIR /app



CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-XX:ParallelGCThreads=8", "-jar", "my-application.jar"]
