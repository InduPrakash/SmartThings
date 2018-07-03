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
	definition (name: "XIP Virtual Garage Door", namespace: "induprakash", author: "Indu Prakash") {
		//http://docs.smartthings.com/en/latest/capabilities-reference.html		
		capability "Door Control"		//attributes=door(closed,closing,open,opening,unknown),commands=open,close
        capability "Garage Door Control"
		//capability "Health Check"
		capability "Refresh"	//commands=refresh
		capability "Switch"		//attributes=switch,commands=on,off		
		capability "Sensor"

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

		main "toggle"
		details(["toggle", "open", "close"])
	}

	//preferences {		
	//	input "finishDelay", "number", title: "Seconds", description: "Seconds after which opening/closing is considered finished", range: "*1..*", displayDuringSetup: false, defaultValue: 0
	//}
}

// parse events into attributes
def parse(String description) {
	log.debug "parse($description)"
	return getStatus()
}

// handle commands
def open() {
	def doorState = state.door
	log.debug "open() door=$doorState"

    if ((doorState == "open") || (doorState == "opening")) {
    	log.debug "open() already open/opening"
    }
    else if ((doorState == "closed") || (doorState == "unknown")){
    	log.debug "open() opening"
    	actuate()        
    	updateStateAndSendEvent("door", "opening")
        
    	//if (finishDelay) {
        //	runIn(finishDelay, finishOpening)
        //}
    }
    else if (doorState == "closing") {
    	log.debug "open() door closing, stopping and then opening"
    	actuate()	//cancel closing
        state.door = "unknown"
    	sendEvent(name: "door", value: "opening")
        runIn(1, open)
    }
}

def close() {
	def doorState = state.door
	log.debug "close() door=$doorState"

    if ((doorState == "closed") || (doorState == "closing")) {
    	log.debug "close() already closed/closing"
    }
    else if ((doorState == "open") || (doorState == "unknown")){
    	log.debug "close() closing"
    	actuate()
		updateStateAndSendEvent("door", "closing")        	

    	//if (finishDelay) {
        //	runIn(finishDelay, finishClosing)
        //}
    }
    else if (doorState == "opening") {
    	log.debug "close() door opening, stopping and then closing"
    	actuate()	//cancel opening
        state.door = "unknown"
    	sendEvent(name: "door", value: "closing")
        runIn(1, close)
    }
}

def on() {
	log.debug "on() Nothing done"
}
def off() {
	log.debug "off() Nothing done"
}

def finishOpening() {
	log.debug "finishOpening() switch=${device.currentValue("switch")}, door=${device.currentValue("door")}"
    finishActuate()
    updateStateAndSendEvent("door", "open")
}
def finishClosing() {
    log.debug "finishClosing() switch=${device.currentValue("switch")}, door=${device.currentValue("door")}"
    finishActuate()
    updateStateAndSendEvent("door", "closed")
}

def actuate() {
	log.debug "actuate()"
	// Momentarily press the opener. Turns it on and then off after 1 second.
	def switchState = state.switch
    if (switchState != "on"){
    	updateStateAndSendEvent("switch", "on")
    	runIn(1, finishActuate)
    }
}


def installed() {
	log.debug "installed()"
	initialize()
}
def updated() {
	log.debug "updated()"
  	initialize()
}
def ping() {
	log.debug "ping()"		//ping is used by Device-Watch in attempt to reach the Device
    initialize()
}
def refresh() {
	log.debug "refresh()"
    initialize()
}

def finishActuate() {
	def switchState = state.switch
    if (switchState == "on"){
    	updateStateAndSendEvent("switch", "off")
    }
}

private updateStateAndSendEvent(String name, String value) {
	log.debug "updateStateAndSendEvent() $name=$value"
	state[name] = value
	sendEvent(name: name, value: value)
}
private initialize() {
	log.debug "initialize()"	
   
    state.door = "closed"
	state.switch = "off"  
   
    sendEvent(name: "door", value: state.door)
	sendEvent(name: "status", value: state.door)
    sendEvent(name: "switch", value: state.switch)    
    //sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
	//sendEvent(name: "healthStatus", value: "online")
	//sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
}
private List getStatus() {
	def results = []
	results << [name: "door", value: state.door]
	results << [name: "status", value: state.door]
	results << [name: "switch", value: state.switch]	
	return results
}