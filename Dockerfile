FROM openjdk:8-jre

COPY ./build/libs/my-application.jar my-application.jar

CMD ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:MaxRAMFraction=1", "-jar", "my-application.jar"]