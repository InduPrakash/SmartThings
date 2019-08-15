/**
 *  Garage Door Device Handler
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
	definition(name: "Garage Door Device Handler", namespace: "induprakash", author: "Indu Prakash") {
		//http://docs.smartthings.com/en/latest/capabilities-reference.html
		capability "Door Control" //attribute=door(closed,closing,open,opening,unknown),commands=open,close
		capability "Refresh" //command=refresh
		capability "Sensor"
		capability "Health Check"
		capability "Temperature Measurement" //attribute=temperature
		capability "Relative Humidity Measurement" //attribute=humidity
		//Not adding "switch", "Contact Sensor" capabilities so that it doesn't appear as a choice in Bridge

		attribute "switch", "string"
		attribute "opensensor", "string"
		attribute "closedsensor", "string"
        attribute "message", "string"
		command "setDoorStatus"
		command "setStatus"
		command "open"
		command "close"
	}

	simulator {}

	// 6 x Unlimited grid
	tiles(scale: 2) {
    	multiAttributeTile(name:"status", type:"generic", width:6, height: 4, canChangeIcon: true) {
            tileAttribute("device.door", key: "PRIMARY_CONTROL") {
                attributeState "unknown", label: '', action: "refresh", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e"
                attributeState "closed", label: 'Closed', action: "open", icon: "st.doors.garage.closed", backgroundColor: "#79b821", nextState: "opening"
                attributeState "open", label: 'Open', action: "close", icon: "st.doors.garage.open", backgroundColor: "#ffa81e", nextState: "closing"
                attributeState "opening", label: 'Opening', icon: "st.doors.garage.garage-opening", backgroundColor: "#ffa81e"
                attributeState "closing", label: 'Closing', icon: "st.doors.garage.garage-closing", backgroundColor: "#79b821"
            }            
            
            tileAttribute("device.temperature", key: "SECONDARY_CONTROL") {
            	attributeState "temperature", label:'${currentValue}°', icon: "st.Weather.weather2"
        	}            
		}
        
		standardTile("open", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: 'Open', action: "open", icon: "st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: 'Close', action: "close", icon: "st.doors.garage.garage-closing"
		}
        
       valueTile("humidity", "device.humidity", width: 2, height: 2) {
            state "humidity", label:'${currentValue}%', unit: "", icon: "st.Weather.weather12"
        }
		standardTile("refresh", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: '', action: "refresh", icon:"st.secondary.refresh-icon"
		}
       
        standardTile("opensensor", "device.opensensor", width: 2, height: 2) {
			state("closed", label: 'SO', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC")
			state("open", label: 'SO', icon: "st.contact.contact.open", backgroundColor: "#e86d13")
		}
        standardTile("closedsensor", "device.closedsensor", width: 2, height: 2) {
			state("closed", label: 'SC', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC")
			state("open", label: 'SC', icon: "st.contact.contact.open", backgroundColor: "#e86d13")
		}
        
		input "debugLogging", "boolean", title: "Enable debug logging?", defaultValue: false, displayDuringSetup: true
		main(["status"])
		details(["status", "open", "close", "refresh", "humidity", "opensensor", "closedsensor"])
	}
}

// parse events into attributes
def parse(String description) {
	logDebug "parse($description)"
	if (description == "finishOpening") {
		finishOpening()
	}
	else if (description == "finishClosing") {
		finishClosing()
	}
	else if (description == "actuate") {
		actuate()
	}
	return getStatus()
}

//Commands
def setStatus(type, value) {
	logDebug "setStatus($type,$value)"
    updateStateAndSendEvent(type, value)
}
def setDoorStatus(state) {
	logDebug "setDoorStatus($state)"
	if (state) {
		if (state == "closed" || state == "open") {
			updateStateAndSendEvent("switch", "off")
		}
		updateStateAndSendEvent("door", state)
	}
}
def open() {
	def doorState = state.door

	if (doorState == "open") {
		sendNotification("Door already open.")
	}
	else if (doorState == "opening") {
		sendNotification("Door already opening.")
	}
	else if ((doorState == "closed") || (doorState == "unknown")) {
		logDebug "open() opening"
		actuate()
		updateStateAndSendEvent("door", "opening")
	}
	else if (doorState == "closing") {
		logDebug "open() door closing, stopping and then opening"
		actuate() //cancel closing
		state.door = "unknown"
		runIn(1, open)
	}
}
def close() {
	def doorState = state.door

	if (doorState == "closed") {
		sendNotification("Door already closed.")
	}
	else if (doorState == "closing") {
		sendNotification("Door already closing.")
	}
	else if ((doorState == "open") || (doorState == "unknown")) {
		logDebug "close() closing"
		actuate()
		updateStateAndSendEvent("door", "closing")
	}
	else if (doorState == "opening") {
		logDebug "close() door opening, stopping and then closing"
		actuate() //cancel opening
		state.door = "unknown"
		runIn(1, close)
	}
}

//Private stuff
def finishOpening() {
	setDoorStatus("open")
}
def finishClosing() {
	setDoorStatus("closed")
}
def actuate() { // Momentarily press the opener.
	def switchState = state.switch
	if (switchState != "on") {
		delayBetween([
			updateStateAndSendEvent("switch", "on"), updateStateAndSendEvent("switch", "off")
		], 500)
	}
}
def updateStateAndSendEvent(String name, String value) {
	state[name] = value
	sendEvent(name: name, value: value)
}
def logDebug(String msg) {
	if (debugLogging) {
		log.debug(msg)
	}
}
def sendNotification(msg) {
	updateStateAndSendEvent("notify", msg)
}
def List getStatus() {
	def results = []
	results << [name: "door", value: state.door]
	results << [name: "switch", value: state.switch]
    results << [name: "opensensor", value: state.opensensor]
    results << [name: "closedsensor", value: state.closedsensor]
	return results
}
def sendStatus() {
	sendEvent(name: "door", value: state.door)
	sendEvent(name: "switch", value: state.switch)
    sendEvent(name: "opensensor", value: state.opensensor)
    sendEvent(name: "closedsensor", value: state.closedsensor)
}


//Overrides
def initialize() {
	//state is a map preserved between device executions
	state.door = "closed"
	state.switch = "off"
    state.opensensor = "open"
    state.closedsensor = "closed"
	sendStatus()

	sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
	sendEvent(name: "healthStatus", value: "online")
	sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme: "untracked"].encodeAsJson(), displayed: false)
    sendEvent(name: "temperature", value: 72, unit: "F")
    sendEvent(name: "humidity", value: 40)
}
def installed() {
	logDebug "installed()"
	initialize()
}
def updated() {
	logDebug "updated()"
	initialize()
}
def ping() {
	logDebug "ping()" //ping is used by Device-Watch in attempt to reach the Device
	sendStatus()
}
def refresh() {
	logDebug "refresh()"
	sendStatus()
}