/**
 *  XIP Virtual Camera
 *
 *  Copyright 2019 Indu Prakash
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 metadata {
	definition (name: "XIP Virtual Camera", namespace: "induprakash", author: "Indu Prakash") {
		capability "Image Capture"
		command "takePicture"
 	}

 	simulator { }
}

def installed() {
	log.trace "installed()"
	markDeviceOnline()
	initialize()
}
def updated() {
	log.trace "updated()"
	initialize()
}
def parse(String description) {}

def take(){
  log.trace "take()"
  sendEvent(name: "image", value: new Date(now()).format("ss"))
}

def takePicture(msg){
  log.trace "takePicture() $msg"
  sendEvent(name: "image", value: msg)
}

def markDeviceOnline() {
	setDeviceHealth("online")
}

private initialize() {
	log.trace "Executing 'initialize'"
	sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
}
