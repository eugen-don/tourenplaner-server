#!/bin/sh
#Copyright 2012 ToureNPlaner
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.


if [ $# -ne 3 ] 
then
   echo "Usage: $0 <file.json> <URL> <HTTP/HTTPS>"
   echo "Example: #$0 simpleSP.json komani.ath.cx:8081 HTTPS"
   exit 1
fi

./curl.sh "$2" "$1" algbdsp root@tourenplaner.de toureNPlaner "$3"
