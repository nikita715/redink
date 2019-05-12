FROM alpine:3.7 as init

ENV GITPLAG_HOME=/opt/gitplag

WORKDIR $GITPLAG_HOME

RUN apk add --update openjdk8 \
    && rm -rf /var/cache/apk/*

COPY / $GITPLAG_HOME

RUN ./gradlew bootJar

FROM alpine:3.7 as prod

ENV GITPLAG_HOME=/opt/gitplag
ENV GITPLAG_SOLUTIONS_DIR=/mnt/gitplag/solutions
ENV GITPLAG_JPLAG_REPORT_DIR=/mnt/gitplag/jplagreports
ENV GITPLAG_ANALYSIS_FILES_DIR=/mnt/gitplag/analysisfiles

WORKDIR $GITPLAG_HOME

RUN apk add --update openjdk8 \
    && rm -rf /var/cache/apk/*

COPY --from=init $GITPLAG_HOME/core/build/libs/core.jar .

CMD java -jar $GITPLAG_HOME/core.jar