FROM openjdk:17

ARG APP_NAME=${artifactId}
ARG APP_PROFILE=prod
ARG UNAME=admin
ARG UID=2023
ARG GID=2023

ENV APP_NAME=${APP_NAME}
ENV APP_HOME=/home/${UNAME}/${APP_NAME}

RUN groupadd -g ${GID} -o ${UNAME}
RUN useradd -m -u ${UID} -g ${GID} -o ${UNAME}

COPY env/* ${APP_HOME}/

RUN mkdir ${APP_HOME}/logs && \
    chmod +x ${APP_HOME}/*.sh && \
    chown -R ${UNAME}:${UNAME} ${APP_HOME}

VOLUME ${APP_HOME}/logs

ENV APP_PROFILE=${APP_PROFILE}

COPY ${APP_NAME}.jar ${APP_HOME}/${APP_NAME}.jar

WORKDIR ${APP_HOME}

ENTRYPOINT ["/bin/bash", "entrypoint.sh"]
