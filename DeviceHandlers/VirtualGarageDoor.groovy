/**
 *  My Virtual Garage Door
 *
 *  Copyright 2018 Indu Prakash
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
	definition (name: "XIP Virtual Garage Door v1", namespace: "induprakash", author: "Indu Prakash") {
		//http://docs.smartthings.com/en/latest/capabilities-reference.html		
		capability "Actuator"
        capability "Door Control"		//attributes=door(closed,closing,open,opening,unknown),commands=open,close
        capability "Garage Door Control"
        capability "Refresh"	//commands=refresh		
		capability "Sensor"	
        capability "Switch"		//attributes=switch,commands=on,off		
		
        command "finishOpening"		
        command "finishClosing"
		command "actuate"
	}

	simulator {	}

	// 6 x Unlimited grid
	tiles(scale: 2) {
		standardTile("toggle", "device.door", width: 2, height: 2, canChangeIcon: true) {			
			//Dynamic device state values like '${currentValue}' and '${name}' must be used inside single quotes. This is in contrast to
			//Groovy’s string interpolation that requires double quotes.
			//action can be "<capability>.<command>" or "on"
			state("unknown", label:'${name}', action:"refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e")
			state("closed", label:'${name}', action:"open", icon:"st.doors.garage.garage-closed", backgroundColor:"#00A0DC", nextState:"opening")
			state("open", label:'${name}', action:"close", icon:"st.doors.garage.garage-open", backgroundColor:"#e86d13", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-closed", backgroundColor:"#e86d13")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#00A0DC")
			
		}
		standardTile("open", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"open", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"close", icon:"st.doors.garage.garage-closing"
		}
       	standardTile("refresh", "device.door", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh", icon:"st.secondary.refresh"
		}

    	main "toggle"
		details(["toggle", "open", "close"])
	}
    
    preferences { 
		input "debugLogging", "boolean", 
		title: "Enable debug logging?",
		defaultValue: false,
		displayDuringSetup: true
	}
}

// parse events into attributes
def parse(String description) {
	logDebug "parse($description)"
	return getStatus()
}

// handle commands
def open() {
	def doorState = state.door
	logDebug "open() door=$doorState"

    if ((doorState == "open") || (doorState == "opening")) {
    	logDebug "open() already open/opening"
    }
    else if ((doorState == "closed") || (doorState == "unknown")){
    	logDebug "open() opening"
    	actuate()        
    	updateStateAndSendEvent("door", "opening")
    }
    else if (doorState == "closing") {
    	logDebug "open() door closing, stopping and then opening"
    	actuate()	//cancel closing
        state.door = "unknown"
    	sendEvent(name: "door", value: "opening")
        runIn(1, open)
    }
}

def close() {
	def doorState = state.door
	logDebug "close() door=$doorState"

    if ((doorState == "closed") || (doorState == "closing")) {
    	logDebug "close() already closed/closing"
    }
    else if ((doorState == "open") || (doorState == "unknown")){
    	logDebug "close() closing"
    	actuate()
		updateStateAndSendEvent("door", "closing")
    }
    else if (doorState == "opening") {
    	logDebug "close() door opening, stopping and then closing"
    	actuate()	//cancel opening
        state.door = "unknown"
    	sendEvent(name: "door", value: "closing")
        runIn(1, close)
    }
}

def on() {
	logDebug "on() Nothing done"
}
def off() {
	logDebug "off() Nothing done"
}

def finishOpening() {
	logDebug "finishOpening() switch=${device.currentValue("switch")}, door=${device.currentValue("door")}"
    finishActuate()
    updateStateAndSendEvent("door", "open")
}
def finishClosing() {
    logDebug "finishClosing() switch=${device.currentValue("switch")}, door=${device.currentValue("door")}"
    finishActuate()
    updateStateAndSendEvent("door", "closed")
}

def actuate() {
	logDebug "actuate()"
	// Momentarily press the opener.
	def switchState = state.switch
    if (switchState != "on"){
		delayBetween([
			updateStateAndSendEvent("switch", "on"),
			finishActuate()
		], 500)
    }
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
	logDebug "ping()"		//ping is used by Device-Watch in attempt to reach the Device
    initialize()
}
def refresh() {
	logDebug "refresh()"
    initialize()
}

def finishActuate() {
	def switchState = state.switch
    if (switchState == "on"){
    	updateStateAndSendEvent("switch", "off")
    }
}

private logDebug(String msg) {
	if (state.debuggingEnabled) {
    	log.debug (msg)
    }
}
private updateStateAndSendEvent(String name, String value) {
	logDebug "updateStateAndSendEvent() $name=$value"
	state[name] = value
	sendEvent(name: name, value: value)
}
private initialize() {
   	state.debuggingEnabled = (debugLogging == "true")    
    logDebug "initialize()"	
    
    state.door = "closed"
	state.switch = "off"  
   
    sendEvent(name: "door", value: state.door)
	sendEvent(name: "status", value: state.door)
    sendEvent(name: "switch", value: state.switch)
}
private List getStatus() {
	def results = []
	results << [name: "door", value: state.door]
	results << [name: "status", value: state.door]
	results << [name: "switch", value: state.switch]	
	return results
}