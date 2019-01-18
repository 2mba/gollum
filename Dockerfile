FROM openjdk:8-jre

COPY ./build/libs/my-application.jar my-application.jar
RUN mkdir /sqlite
RUN mkdir /sqlite/db

CMD ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:MaxRAMFraction=1", "-jar", "my-application.jar"]