definition(
	name: "XIP Virtual Garage Door",
	namespace: "induprakash",
	author: "Indu Prakash",
	description: "Syncs XIP Virtual Garage Door device with a contact sensor.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/doorbot@2x.png")

preferences {
	section("Which sensor can tell if the door is closed?") {
		input "sensor", "capability.contactSensor",	title: "Garage Door Close Sensor", required: true
	}
	section("Which virtual garage door to use?") {
		input "virtualDoor", "capability.doorControl", title: "Virtual Garage Door", required: true
	}
	section("Check if door opened/closed correctly?") {
		input "checkAfter", "number", title: "Operation Check Delay?", required: true, defaultValue: 25
	}
	section("Notifications") {
		input("recipients", "contact", title: "Send notifications to") {
			input "sendMsg", "enum", title: "Send notification?", options: ["Yes", "No"], required: false
		}
	}
}

def installed() {
	log.debug "installed()"
	initialize()
}

def updated() {
	log.debug "updated()"
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	log.debug "initialize()"
	subscribe(virtualDoor, "door", doorHandler)
	subscribe(sensor, "contact", contactHandler)
	syncVirtual()
    //use hasCommand/getTypeName to check if garage door is compatible
}

def syncVirtual() {
	def sensorCurrentValue = sensor.currentValue("contact")
	if (sensorCurrentValue != virtualDoor.currentValue("contact")) {
		if (sensorCurrentValue == "closed") {
			log.debug "syncVirtual() closing virtual door"
			virtualDoor.finishClosing()
		} else if (sensorCurrentValue == "open") {
			log.debug "syncVirtual() opening virtual door"
			virtualDoor.finishOpening()
		}
	}
}

def contactHandler(evt) {
	syncVirtual()
}

def doorHandler(evt) {
	state.door = evt.value
	log.debug "doorHandler() operation=$state.door"
	if (evt.value == "opening") {
		state.doorAction = evt.value
		state.doorActionAt = evt.date
		if (checkAfter) {
			runIn(checkAfter, checkStatus)
		}
	} else if (evt.value == "closing") {
		state.doorAction = evt.value
		state.doorActionAt = evt.date
		if (checkAfter) {
			runIn(checkAfter, checkStatus)
		}
	}
}

def checkStatus() {
	def sensorCurrentValue = sensor.currentValue("contact")
	log.debug "checkStatus() $state.doorAction sensorCurrentValue=$sensorCurrentValue"
	if (state.doorAction == "opening") {
		if (sensorCurrentValue != "open") {
			trySendNotification("Door failed to open, door opened at ${state.doorActionAt}.")
		} else {}
	}
	
	if (state.doorAction == "closing") {
		if (sensorCurrentValue != "closed") {         	
			trySendNotification("Door failed to close, door closed at ${state.doorActionAt}.")
		} else {}
	}
}

private trySendNotification(String msg) {
	if (sendMsg != "No") {
		sendPush(msg)
	}
}