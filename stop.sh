#!/usr/bin/env bash

set -u

OWN_DIR=$(dirname "$0")
BOT_PID=$(cat "$OWN_DIR/bot.pid")
rm "$OWN_DIR/bot.pid"
for _ in {1..15}; do
  if ! ps -p "$BOT_PID" >/dev/null; then
    exit 0
  fi
  sleep 1
done
