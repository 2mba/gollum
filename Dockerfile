FROM openjdk:8-jre

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 9DA31620334BD75D9DCB49F368818C72E52529D4
RUN echo "deb http://repo.mongodb.org/apt/debian stretch/mongodb-org/4.0 main" | tee /etc/apt/sources.list.d/mongodb-org-4.0.list
RUN apt-get update
RUN apt-get install -y mongodb-org
RUN mkdir -p /mongodb/db

RUN echo 'mongod --fork --dbpath=/mongodb/db --logpath=/mongodb/mongo.log --wiredTigerCacheSizeGB=0.5 &' >> start_mongo.sh
RUN chmod a+x start_mongo.sh

COPY script.sh script.sh
RUN chmod a+x script.sh

COPY ./build/libs/my-application.jar my-application.jar

RUN ls -l
CMD ./script.sh