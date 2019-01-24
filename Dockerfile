FROM openjdk:8-jre

COPY ./build/libs/my-application.jar my-application.jar
RUN mkdir /sqlite
RUN mkdir /sqlite/db

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=1", "-XX:MinRAMFraction=1", "-XX:MaxRAMFraction=1","-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "my-application.jar"]