/**
 *  Notifier
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
definition(
    name: "Notifier",
    namespace: "induprakash",
    author: "Indu Prakash",
    description: "Allows a device to send push message.",
    category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/window_contact@2x.png")

preferences {
	section("Device with 'notify' attribute:") {
		input "source", "capability.sensor", title: "Device?", required: true
	}
	section() {
		input "sendMsg", "boolean", title: "Send notification?", defaultValue: true, displayDuringSetup: true
        input "debugLogging", "boolean", title: "Enable logging?", defaultValue: false, displayDuringSetup: false
	}
}

//Public
def installed() {
    logDebug "Installed Notifier"
    initialize()
}
def updated() {
	logDebug "Updated Notifier"
    unsubscribe()
	initialize()
}
def initialize() {	
	if (source.hasAttribute("notify")) {
		subscribe(source, "notify", notificationHandler)
    }
	else {
		log.error("Unsupported device, it does not have 'notify' attribute.")
	}
}

//Event handler need to be public
def notificationHandler(evt) {
    logDebug "Received ${evt.name} ${evt.value}"    
    
    if (sendMsg) {
		sendPush("${evt.value}")
	}
}
def logDebug(String msg) {
    if (debugLogging) {
		log.debug(msg)
	}
}
