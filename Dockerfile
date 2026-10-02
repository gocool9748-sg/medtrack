FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY lib ./lib
COPY src ./src
COPY web ./web

RUN mkdir -p bin && find src -name "*.java" > sources.txt && javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt && rm sources.txt

ENV PORT=10000
EXPOSE 10000

CMD ["java", "-cp", "bin:lib/*", "com.medtrack.Main"]
