#!/bin/sh
if [ -n "${JAVA_HOME:-}" ]; then
  task_java="$JAVA_HOME/bin/java"
else
  task_java=java
fi
exec "$task_java" -classpath "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
