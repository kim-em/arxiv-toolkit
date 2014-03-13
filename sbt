#!/bin/sh
if test -f ~/.sbtconfig; then
  . ~/.sbtconfig
fi
java -Xms512M -Xmx3072M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:+UseCodeCacheFlushing -XX:MaxPermSize=384M -jar `dirname $0`/jars/sbt-launch-0.12.2.jar "$@"
