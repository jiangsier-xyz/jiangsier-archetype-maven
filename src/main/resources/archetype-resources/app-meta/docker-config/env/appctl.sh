#!/usr/bin/env bash

# chkconfig:   2345 90 10
# description:  http://gitlab.alibaba-inc.com/IoT-DSD/iotx-data-open-catcher

PROG_NAME=$0
ACTION=${1:-restart}

JAR_FILE=${APP_NAME}.jar
JAR_PATH=${APP_HOME}/${APP_NAME}.jar

SERVICE_NAME=${APP_NAME}

usage() {
    echo "Usage: ${PROG_NAME} {start|stop|restart|status}"
    exit 2 # bad usage
}

do_start() {
    if [[ -z "${SERVER_PORT}" ]]; then
        SERVER_PORT=8080
    fi

    if [[ "${JPDA_ENABLE}" == "true" ]]; then
        if [[ -z "${JPDA_PORT}" ]]; then
            JPDA_PORT=5005
        fi
        JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${JPDA_PORT}"
    fi

    JAVA_OPTS="${JAVA_OPTS} -server"

    local memTotal=`cat /proc/meminfo | grep MemTotal | awk '{printf "%d", $2/1024 }'`
    echo "INFO: OS total memory: "${memTotal}"M"

    JAVA_OPTS="${JAVA_OPTS} -Djava.awt.headless=true"
    JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${APP_PROFILE}"

    SERVER_OPTS="${SERVER_OPTS} --server.tomcat.uri-encoding=ISO-8859-1 --server.tomcat.max-threads=400 --server.port=${SERVER_PORT}"

    eval exec "java" ${JPDA_OPTS} "-jar" ${JAVA_OPTS} ${JAR_PATH} ${SERVER_OPTS} "&"
}

get_pid() {
    ls -l /proc/[0-9]*/exe | grep "java" | awk -F/ '{print $3}'
    # ps -ef | grep "java" | grep "spring.profiles.active=${APP_PROFILE}" | awk '{print $2}'
}

start() {
    echo "INFO: ${APP_NAME} try to start..."
    do_start
    local s=0
    local pid=$(get_pid)
    while [[ -z "${pid}" && ${s} -le 60 ]]; do
        let s+=1
        sleep 1
        echo "INFO: Waiting for ${s} s..."
        pid=$(get_pid)
    done

    if [[ -n "${pid}" ]]; then
        echo "INFO: ${APP_NAME} start successfully, pid=${pid}"
    else
        echo "INFO: ${APP_NAME} start failed"
    fi
}

stop() {
    echo "INFO: ${APP_NAME} try to stop..."
    local pid=$(get_pid)
    if [[ -n "${pid}" ]]; then
        kill -15 ${pid}
        sleep 1
        local s=0
        pid=$(get_pid)
        while [[ -n "${pid}" && ${s} -le 60 ]]; do
            let s+=1
            sleep 1
            echo "INFO: Waiting for ${s} s..."
            pid=$(get_pid)
        done
    fi

    if [[ -n "${pid}" ]]; then
        echo "INFO: ${APP_NAME} stop failed! pid is ${pid}"
        exit 1
    fi

    echo "INFO: ${APP_NAME} stop successfully"
}

status() {
    local pid=$(get_pid)
    if [[ -n "${pid}" ]]; then
        echo "running"
    else
        echo "shutdown"
    fi
}

main() {
    case "${ACTION}" in
        start)
            start
        ;;
        stop)
            stop
        ;;
        restart)
            stop
            start
        ;;
        status)
            status
        ;;
        *)
            usage
        ;;
    esac
}

main
