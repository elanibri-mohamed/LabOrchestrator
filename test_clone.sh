#!/bin/bash
curl -s -c /tmp/cookie -b /tmp/cookie -X POST -d '{"username":"admin","password":"eve","html5":1}' http://10.10.30.10/api/auth/login > /dev/null

test_payload() {
  echo "Testing payload: $1"
  curl -s -c /tmp/cookie -b /tmp/cookie -X POST -d "$1" -H 'Content-type: application/json' http://10.10.30.10/api/labs/lab-1.unl/clone
  echo -e "\n"
}

test_payload '{"name":"Clone1"}'
test_payload '{"name":"Clone1","path":"/"}'
test_payload '{"name":"Clone1.unl"}'
test_payload '{"name":"Clone1","folder":"/"}'
