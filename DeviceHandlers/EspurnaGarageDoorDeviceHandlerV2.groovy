/**
 *  Espurna API Garage Door Device Handler
 *
 *  Copyright 2019 Indu Prakash
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

/*
https://docs.smartthings.com/en/latest/smartapp-developers-guide/state.html
https://www.javaworld.com/article/2074120/documenting-groovy-with-groovydoc.html
https://www.tutorialspoint.com/groovy/groovy_methods.htm
https://docs.smartthings.com/en/latest/tools-and-ide/logging.html
http://scripts.3dgo.net/smartthings/icons/
http://docs.smartthings.com/en/latest/capabilities-reference.html
*/

metadata {
	definition(
		name: "Espurna Garage Door Device Handler V2", namespace: "induprakash", author: "Indu Prakash") {
		capability "Door Control" //attribute=door,commands=open,close
		capability "Health Check"
		capability "Sensor"
		capability "Refresh"	//command=refresh
		capability "Relative Humidity Measurement" //attribute=humidity
		capability "Temperature Measurement" //attribute=temperature
		
		attribute "opensensor", "string"
		attribute "closedsensor", "string"
		attribute "notify", "string"
		
		command "setStatus"
		command "refresh"
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
			state "stopped", label: 'Stopped', icon: "st.contact.contact.open", backgroundColor: "#ffdd00"
		}
		
		standardTile("action", "device.door", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true, decoration: "flat") { 
			state "unknown", label: '', icon: "st.unknown.unknown.unknown", backgroundColor: "#afafaf", action: "refresh"
			state "closed", label: 'Closed', icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821", action: "open"
			state "open", label: 'Open', icon: "st.doors.garage.open", backgroundColor: "#ffa81e", action: "close"
			state "opening", label: 'Opening', icon: "st.doors.garage.garage-opening", backgroundColor: "#ffa81e"
			state "closing", label: 'Closing', icon: "st.doors.garage.garage-closing", backgroundColor: "#79b821"
			state "moving", label: 'Moving', icon: "st.motion.motion.active", backgroundColor: "#ffdd00", action: "refresh"
			state "stopped", label: 'Stopped', icon: "st.contact.contact.open", backgroundColor: "#ffdd00", action: "close"
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
		
		standardTile("refresh", "", width: 2, height: 2, decoration: "flat") {
			state "default", label: 'Refresh', action: "refresh.refresh", icon: "st.secondary.refresh"
		}
		 
		main(["status"])
		details(["action", "forceOpen", "forceClose", "temperature", "refresh" ])
	}
	
	preferences{
		//Setting a default value (defaultValue: "foobar") for an input may render that selection in the mobile app, but the user still needs to enter data in that field. It’s recommended to not use defaultValue to avoid confusion.
		input "ipAddress", "text", title: "Garage Controller's IP Address", description: "IP address of the Espurna device", displayDuringSetup: true
		input "apiKey", "text", title: "Garage Controller's API Key", description: "HTTP API key", displayDuringSetup: true
		input "debugLogging", "boolean", title: "Enable logging?", defaultValue: false, displayDuringSetup: false
	}
}

/**
 * Command to refresh door status.
 */
def refresh() {
	fetchStatus()
}
/**
 * Command to open the door.
 */
def open() {
	openClose(1)
}
/**
 * Command to close the door.
 */
def close() {
	openClose(2)
}

/**
 * Invoked by MQTT Bridge.
 */
def setStatus(type, value) {
	log.info "setStatus($type,$value)"
	
	if (type == "door") {
		sendEvent(name: type, value: value, displayed: true)
	}
	else if (type == "opensensor") {
		sendEvent(name: type, value: value, displayed: false)
	}
	else if (type == "closedsensor") {
		sendEvent(name: type, value: value, displayed: false)
	}
	else if (type == "temperature") {
		sendEvent(name: type, value: value, displayed: true)
	}
	else if (type == "humidity") {
		sendEvent(name: type, value: value, displayed: true)
	}
	else if (type == "notify") {
		sendEvent(name: type, value: value, displayed: false)
	}
}

def fetchCallback(physicalgraph.device.HubResponse hubResponse) {
	//Callback result contains state/opensensorPressed/closedsensorPressed
	def splitted = hubResponse.body?.split("/")
	if (splitted?.size() == 3) {
		//logDebug "${splitted}"
		def stateValue = "unknown"
		def stateOpenSensor = "open"
		def stateClosedSensor = "open"
		switch(splitted[0]) {
			case "1":
				stateValue = "open"
				break
			case "2":
				stateValue = "closed"
				break
			case "3":
				stateValue = "opening"
				break
			case "4":
				stateValue = "closing"
				break
			case "5":
				stateValue = "stopped"
				break
		}
		
		if (splitted[1] == "1") {
			stateOpenSensor = "closed"
		}
		if (splitted[2] == "1") {
			stateClosedSensor = "closed"
		}
		
		log.info "fetchCallback door=${stateValue} openSensor=${stateOpenSensor} closedSensor=${stateClosedSensor}"
		sendEvent(name: "door", value: stateValue)	
		sendEvent(name: "opensensor", value: stateOpenSensor, displayed: false)
		sendEvent(name: "closedsensor", value: stateClosedSensor, displayed: false)
	}
	
	sendEvent(name: "healthStatus", value: "online", displayed: false)
	sendEvent(name: "DeviceWatch-DeviceStatus", value: "online", displayed: false)
}

/**
 * Fetch status from the device. This gets calls from a timer as well.
 */
def fetchStatus() {
	if (apiKey && ipAddress) {
		def hubAction = new physicalgraph.device.HubAction(
			method: "GET",
			path:  "/api/door?apikey=${apiKey}",
			headers: [
				HOST: "${ipAddress}:80"
			],
			null,
			[callback: fetchCallback]
		)
		logDebug "fetchStatus"
		sendHubCommand(hubAction)
	}
	
	//Fetch status again every 60 seconds
	runIn(60, fetchStatus)
}

//Private stuff
private openClose(value){
	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path:  "/api/door?apikey=${apiKey}&value=${value}",
		headers: [
			HOST: "${ipAddress}:80"
		],
		null,
		[callback: fetchCallback]
	)
	
	def doorState = device.currentValue("door")
	log.info "openClose(${value}) current doorState=${doorState} value=${value}"
	sendHubCommand(hubAction)
}
private logDebug(String msg) {
	if (debugLogging) {
		log.debug(msg)
	}
}
private initialize() {
	//The "state" map preserved between device executions	
	
	sendEvent(name: "door", value: "unknown", displayed: false)
	sendEvent(name: "opensensor", value: "", displayed: false)
	sendEvent(name: "closedsensor", value: "", displayed: false)	
	sendEvent(name: "DeviceWatch-DeviceStatus", value: "offline", displayed: false)
	sendEvent(name: "healthStatus", value: "offline", displayed: false)
	sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme: "untracked"].encodeAsJson(), displayed: false)
	fetchStatus()
}

//Overrides
def parse(String description) {
	logDebug "Parse $description"	
}
def installed() {
	logDebug "Installed Garage Door"
	initialize()
}
def updated() {
	logDebug "Updating Garage Door"
	initialize()
}