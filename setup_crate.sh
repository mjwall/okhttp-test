#!/bin/sh

error() {
  echo $1
  exit 1
}

DOCKER=$(which docker) || error "Do you have docker installed?"
CURL=$(which curl) || error "Do you have curl installed?"

CONTAINER="library/crate:2.3.3"
CONTAINER_NAME="okhttp-bug-1"
CRATE_URL="127.0.0.1:4200"

get_crate_and_start() {
  $DOCKER pull $CONTAINER || error "See docker message above"
  $DOCKER run --rm -d -p 4200:4200 --name ${CONTAINER_NAME} $CONTAINER
  printf "Starting crate"
  while true; do
    $CURL -sqL $CRATE_URL >/dev/null && break
    printf '.' && sleep 1
  done
  echo
  echo Crate started to see http://${CRATE_URL}.  To stop the docker container run
  echo "\t$DOCKER stop $CONTAINER_NAME"
}

create_blobs() {
  $CURL -sSPOST "$CRATE_URL/_sql?pretty" -d '{"stmt":"create blob table myblob"}' > /dev/null
  $CURL -sSX PUT "$CRATE_URL/_blobs/myblob/6dcd4ce23d88e2ee9568ba546c007c63d9131c1b" -d "A"
  $CURL -sSX PUT "$CRATE_URL/_blobs/myblob/32096c2e0eff33d844ee6d675407ace18289357d" -d "C"
  $CURL -sSX PUT "$CRATE_URL/_blobs/myblob/50c9e8d5fc98727b4bbc93cf5d64a68db647f04f" -d "D"
}

get_crate_and_start && create_blobs