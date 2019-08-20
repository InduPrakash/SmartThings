/**
 *  Espurna Garage Door Device Handler
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
	definition(name: "Espurna Garage Door Device Handler", namespace: "induprakash", author: "Indu Prakash") {
		//http://scripts.3dgo.net/smartthings/icons/
        //http://docs.smartthings.com/en/latest/capabilities-reference.html
        capability "Door Control" //attribute=door(closed,closing,open,opening,unknown),commands=open,close
		capability "Sensor"
		capability "Health Check"
		capability "Temperature Measurement" //attribute=temperature
		capability "Relative Humidity Measurement" //attribute=humidity
		//Not adding "switch", "Contact Sensor" capabilities so that it doesn't appear as a choice in the Bridge app

		attribute "switch", "string"
		attribute "opensensor", "string"
		attribute "closedsensor", "string"
		attribute "notify", "string"
		attribute "uptime", "number" //heartbeat value
		
		command "setStatus"
		command "open"
		command "close"
	}

	simulator {}

	// 6 x Unlimited grid
	tiles(scale: 2) {
    	standardTile("status", "device.door", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true, decoration: "flat") { 
			state "unknown", label: 'Unknown', icon: "st.unknown.unknown.unknown", backgroundColor: "#afafaf"
            state "closed", label: 'Closed', icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821"
            state "open", label: 'Open', icon: "st.doors.garage.open", backgroundColor: "#ffa81e"
            state "opening", label: 'Opening', icon: "st.doors.garage.garage-opening", backgroundColor: "#ffa81e"
            state "closing", label: 'Closing', icon: "st.doors.garage.garage-closing", backgroundColor: "#79b821"
            state "moving", label: 'Moving', icon: "st.motion.motion.active", backgroundColor: "#ffdd00"
        }  
        
        standardTile("action", "device.door", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true, decoration: "flat") { 
        	state "unknown", label: '', icon: "st.unknown.unknown.unknown", backgroundColor: "#afafaf"
            state "closed", label: 'Closed', icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821", action: "open", nextState: "opening"
            state "open", label: 'Open', icon: "st.doors.garage.open", backgroundColor: "#ffa81e", action: "close", nextState: "closing"
            state "opening", label: 'Opening', icon: "st.doors.garage.garage-opening", backgroundColor: "#ffa81e"
            state "closing", label: 'Closing', icon: "st.doors.garage.garage-closing", backgroundColor: "#79b821"
            state "moving", label: 'Moving', icon: "st.motion.motion.active", backgroundColor: "#ffdd00"
        }   	
        
		standardTile("forceOpen", "", width: 2, height: 2, decoration: "flat") {
			state "default", label: 'Force Open', action: "open", icon: "st.doors.garage.garage-opening"
		}
		standardTile("forceClose", "", width: 2, height: 2, decoration: "flat") {
			state "default", label: 'Force Close', action: "close", icon: "st.doors.garage.garage-closing"
		}
        
       	valueTile("temperature", "device.temperature", width: 2, height: 2, canChangeIcon: true) {
            state "temperature", label:'${currentValue}°', icon: "st.Weather.weather2"            	
        }       
        valueTile("humidity", "device.humidity", width: 2, height: 2) {
			state "humidity", label: '${currentValue}%', unit: "", icon: "st.Weather.weather12"
		}
        standardTile("opensensor", "device.opensensor", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
			state("closed", label: 'Open Sensor', icon: "st.contact.contact.closed", backgroundColor: "#79b821")
			state("open", label: 'Open Sensor', icon: "st.contact.contact.open", backgroundColor: "#ffa81e")
		}
        standardTile("closedsensor", "device.closedsensor", width: 2, height: 2, canChangeIcon: true, decoration: "flat") {
			state("closed", label: 'Open Sensor', icon: "st.contact.contact.closed", backgroundColor: "#79b821")
			state("open", label: 'Open Sensor', icon: "st.contact.contact.open", backgroundColor: "#ffa81e")
		}        
		
		main(["status"])
		details(["action", "forceOpen", "forceClose", "temperature", "humidity" ])
	}
    
    preferences{
    	//Setting a default value (defaultValue: "foobar") for an input may render that selection in the mobile app, but the user still needs to enter data in that field. It’s recommended to not use defaultValue to avoid confusion.
        input "checkAfter", "number", title: "Seconds (5..60) after which to verify door operation?", range: "5..60", displayDuringSetup: false
        input "debugLogging", "boolean", title: "Enable logging?", defaultValue: false, displayDuringSetup: false
    }
}

//setStatus is invoked by the bridge
def setStatus(type, value) {
	logDebug "setStatus($type,$value)"
    def doorCurrentValue = state.door
    
    if (type == "opensensor") {
        sendEvent(name: type, value: value, isStateChange: true, displayed: false)
        
    	if (value == "closed"){	// && doorCurrentValue != "open") {
			setDoorStatus("open")
		}
		if (value == "open" && doorCurrentValue != "closing") {
			sendNotifyEvent("Garage door physically closed.")
			setDoorStatus("closing")
		}
    }
    else if (type == "closedsensor") {
        sendEvent(name: type, value: value, isStateChange: true, displayed: false)
        
        if (value == "open" && doorCurrentValue != "opening") { //Garage door opened through the physical button
            sendNotifyEvent("Garage door physically opened.")
            setDoorStatus("opening")
        }
        if (value == "closed") {	// && doorCurrentValue != "closed") {
            setDoorStatus("closed")
        }
    }
    else if (type == "temperature") {
    	sendEvent(name: type, value: value, isStateChange: true, displayed: true)
    }
    else if (type == "humidity") {
    	sendEvent(name: type, value: value, isStateChange: true, displayed: true)
    }
    else if (type == "notify") {
    	sendEvent(name: type, value: value, isStateChange: true, displayed: false)
    }
    else if (type == "uptime") {
    	//uptime is sent every 5 minutes
        state.heartbeat = new Date().getTime()
        sendEvent(name: "healthStatus", value: "online")
	}
}

def open() {
	def doorState = state.door

	if (doorState == "open") {
		sendNotifyEvent("Door already open.")
	}
	else if (doorState == "opening") {
		sendNotifyEvent("Door already opening.")
	}
	else if ((doorState == "closed") || (doorState == "unknown")) {
		logDebug "Opening the door"
		setDoorStatus("opening")
        actuate()		
	}
	else if (doorState == "closing") {
		logDebug "Door was closing, first stopping"
		actuate() //cancel closing
        
        //setting state without sending event. open() will send the correct event
		state.door = "unknown"
		runIn(2, open)
	}
}
def close() {
	def doorState = state.door

	if (doorState == "closed") {
		sendNotifyEvent("Door already closed.")
	}
	else if (doorState == "closing") {
		sendNotifyEvent("Door already closing.")
	}
	else if ((doorState == "open") || (doorState == "unknown")) {
		logDebug "Closing the door"
        
        //Set closing status before actuating - actuate triggers opensensor event
        //which can generate false notification of "Garage door manually closed"
		setDoorStatus("closing")
        actuate()
	}
	else if (doorState == "opening") {
		logDebug "Door was opening, first stopping"
		actuate() //cancel opening
		
        //setting state without sending event. close() will send the correct event
        state.door = "unknown"
		runIn(2, close)
	}
}

//Timer callbacks have to be public
def checkStatus(data) {
	def operation = data.operation
	def doorCurrentValue = state.door    
	logDebug "Checking door status, door=$doorCurrentValue, operation=$operation"

	if (operation == "opening") {
    	if (doorCurrentValue == "closed") {
        	sendNotifyEvent("Door failed to open and instead closed, opened at ${data.doorActionAt}.")            
        }
        else if (doorCurrentValue == "opening") {
        	sendNotifyEvent("Door failed to open, opened at ${data.doorActionAt}.")
        }
	}
    else if (operation == "closing") {
    	if (doorCurrentValue == "open") {
        	sendNotifyEvent("Door failed to close and instead opened, closed at ${data.doorActionAt}.")
        }
        else if (doorCurrentValue == "closing") {
        	sendNotifyEvent("Door failed to close, closed at ${data.doorActionAt}.")
        }
	}
}
//Private stuff
def setDoorStatus(state) {
	logDebug "setDoorStatus($state)"
	if (state) {
		if (state == "closed" || state == "open") {
            sendEvent(name: "switch", value: "off", isStateChange: true, displayed: false)
		}
        
        if (state == "opening" || state == "closing") {
            if (checkAfter) {
                runIn(checkAfter, checkStatus, [data: [operation: state, doorActionAt: getFormattedTime(new Date(now()))]]) //the default behavior is to overwrite the pending schedule
            }
        }
		
        sendEvent(name: "door", value: state, isStateChange: true)
	}
}
def switchOff() {
    sendEvent(name: "switch", value: "off", isStateChange: true, displayed: false)
}
def switchOn() {
    sendEvent(name: "switch", value: "on", isStateChange: true, displayed: false)
}
def performScheduledTasks() {
	logDebug "performScheduledTasks()"
    def currentTime = new Date().getTime()
    def duration = currentTime - state.heartbeat
    if (duration > (5 * 60 * 1000)) {	//5 minutes
    	sendEvent(name: "healthStatus", value: "offline")
    }
}
def actuate() {
    // Momentarily press the opener. delayBetween is not documented, using runIn
    switchOn();
    runIn(1, switchOff)
}
def logDebug(String msg) {
	if (debugLogging) {
		log.debug(msg)
	}
}
//Don't use sendNotification, it is a public method
def sendNotifyEvent(msg) {
	//state[name] = value
	sendEvent(name: "notify", value: msg, isStateChange: true, displayed: true)
}
def String getFormattedTime(Date dt) {
	if (!dt) {
		return ""
	}
	def tz = location.getTimeZone()
	if (!tz) {
		tz = TimeZone.getTimeZone("CST")
	}
	return dt.format('h:mm a', tz)
}


//Overrides
def initialize() {
    //The "state" map preserved between device executions
	state.door = "closed"
	state.switch = "off"
    state.opensensor = "open"
    state.closedsensor = "closed"
    state.temperature = ""
    state.humidity = ""
    state.notify = ""
	state.heartbeat = 0
    
    sendEvent(name: "door", value: state.door)
	sendEvent(name: "switch", value: state.switch, displayed: false)
    sendEvent(name: "opensensor", value: state.opensensor, displayed: false)
    sendEvent(name: "closedsensor", value: state.closedsensor, displayed: false)
    sendEvent(name: "temperature", value: state.temperature, displayed: false)
    sendEvent(name: "humidity", value: state.humidity, displayed: false)
    sendEvent(name: "notify", value: state.notify, displayed: false)
    
	sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false)
	sendEvent(name: "healthStatus", value: "online")
	sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme: "untracked"].encodeAsJson(), displayed: false)
    
    runEvery10Minutes(performScheduledTasks)
}
def installed() {
	logDebug "Installed Garage Door"
    initialize()
}
def updated() {
	logDebug "Updating Garage Door"
	initialize()
}
def ping() {
	logDebug "ping()" //ping is used by Device-Watch in attempt to reach the Device
	//Sending temperature value in ping
	sendEvent(name: "temperature", value: state.temperature)
}
def parse(String description) {
	logDebug "Parse description $description"
}
