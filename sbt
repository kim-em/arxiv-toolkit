#!/bin/sh
if test -f ~/.sbtconfig; then
  . ~/.sbtconfig
fi
XMX=3072M
java -Xms512M -Xmx$XMX -Xss1M -XX:+CMSClassUnloadingEnabled -XX:+UseCodeCacheFlushing -jar `dirname $0`/jars/sbt-launch-0.13.1.jar "$@"
