definition(
	name: "Garage Door Checker",
	namespace: "induprakash",
	author: "Indu Prakash",
	description: "Sends notification if the garage door is open at the scheduled time.",
	category: "Convenience",
	iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn.png",
	iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation14-icn@2x.png") 

preferences {
    input "door", "capability.garageDoorControl", title: "Which garage door to check?", required: true
    input "executeTime", "time", title: "When to check?", required: true
}

def installed() {
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
	if (executeTime && door) {
    	schedule(executeTime, handler)	//run daily
	}    
}

def handler() {
    log.debug "handler executed at ${new Date()}"
    if (door.currentValue("door") == "open"){
    	sendNotification("The ${door} is open.")
    }
}
