# Espurna Garage Door Controller

This project implements a garage door opener controller based on a ESP8266 device interfaced through SmartThings.


# Features
* Open/close the door
* Verify opening/closing operation
* Notifications about door operations
* Temperature monitoring

# The ESP8266 Device

An ESP8266 device (ESP12 in my case) connects to the existing garage opener through a relay and receives input from 2 sensors (open sensor and closed sensor). 

The firmware is based on Espurna with many things turned off.


## SmartThings

### Device Handler
A device

### SmartApp

### Bridge



I found some virtual garage door controllers but just could not get them working with the standard push openers in my setup.

I had  already interfaced some ESP8266 devices and exposed SmartThings devices to [HomeAssistant](https://www.home-assistant.io/) using [SmartThings MQTT Bridge](https://github.com/InduPrakash/smartthings-mqtt-bridge).


So, I wrote my own set of device handler and app. This also gave me a chance to learn more about SmartThings structure. 


<hr/>

## SmartThings

### Device Handler ([XIP Virtual Garage Door](https://github.com/InduPrakash/SmartThings/blob/master/DeviceHandlers/VirtualGarageDoor.groovy))
This represents a virtual garage door device. The main capabilities which it implements are [Door Control](http://docs.smartthings.com/en/latest/capabilities-reference.html#door-control) and [Switch](http://docs.smartthings.com/en/latest/capabilities-reference.html#switch). It also exposes a custom command to allow interaction with the smart app.

#### Door Operation
The standard push opener either opens or closes the door:
* closed -> opening -> open
* open -> closing -> closed

If the door is already opening/closing then pressing the opener cancels the current operation and pressing again reverses the operation.


These are the mapped operations:
* open()
    * if state=closed then actuate()
    * if state=open/opening then no action needed
    * if state=closing then actuate(), open() in 1 second
* close()
    * if state=open then actuate()
    * if state=closed/closing then no action needed
    * if state=opening then actuate(), close() in 1 second


### Smart App ([Two Sensor Garage Door Controller](https://github.com/InduPrakash/SmartThings/blob/master/SmartApps/VirtualGarageDoorAppTwoSensors.groovy))
The app is the brains linking the physical sensors and the virtual device.

It requires:
* Two contact sensor devices, one indicating completely opened state and the other closed state.
* Virtual door device - this would be the XIP Virtual Garage Door device.

It syncs the device, if the door was opened through the physical switch. If the door is opened through the app, then a verification check is done after a configurable amount of time.


## Setup
Log into [SmartThings Groovy IDE](https://graph.api.smartthings.com/).
1. Go to My Device Handlers, click Create New Device Handler, use From Code and paste the [raw device handler code](https://raw.githubusercontent.com/InduPrakash/SmartThings/master/DeviceHandlers/VirtualGarageDoor.groovy). Save and Publish For Me.
2. Go to My Devices, create a new device. Give it a Name, Device Network id, Type "XIP Virtual Garage Door", Version "Published" and Hub.
3. Go to My SmartApps, click new SmartApp, use From Code and paste the [raw app code](https://raw.githubusercontent.com/InduPrakash/SmartThings/master/SmartApps/VirtualGarageDoorApp.groovy). Save and Publish For Me.
4. Launch the Classic SmartThings mobile app, go to SmartApps, add a SmartApp. Scroll down to My Apps and select XIP Virtual Garage Door, pick contact sensors and virtual garage door device and you are set.

There are 3 devices involves - the XIP Virtual Garage Door based device and 2 contact sensors. My open sensor was actually a virtual device based on this [device handler](https://github.com/InduPrakash/SmartThings/blob/master/DeviceHandlers/ContactSensorCapability.groovy). This is why I had to modify the [SmartThings MQTT Bridge](https://github.com/InduPrakash/smartthings-mqtt-bridge).




## ESP8266 (ESP-12E)
The controller in my case was a ESP-12E module of [ESP8266](https://en.wikipedia.org/wiki/ESP8266) microchip. It has plenty of GPIO pins and is quite cost effective. It controls the garage opener and also acts as the open sensor. It is flashed with a custom [Espurna based firmware](https://github.com/InduPrakash/espurna/tree/button).

* The controller subscribes to "smartthings/[SmartThings Device Name]/switch" topic to activate the relay connected to the garage switch.
* It also sends out a message to "smartthings/[Open Sensor Device Name]/contact" topic when the physical magnetic contact switch is activated.


<hr/>

## The Complete Picture
On the SmartThings end, there is a XIP Virtual Garage Door device and an open

* ST app -> turns the switch "on" and then "off"
    * MQTT Bridge converts these into MQTT messages
        * Espurna device activates the garage door
    
* Door completely open -> sensor triggered -> a MQTT message sent out
    * MQTT Bridge converts the message into an event on ST device
        * ST app receives the event and updates the virtual device.

