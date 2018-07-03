# Virtual Garage Door
There are many good virtual garage door controllers available but they lacked technical details or were just too complicated for me. I could not get them working with the standard push openers.

One additional caveat was that my physical opener device was interfaced through [SmartThings MQTT Bridge](https://github.com/stjohnjohnson/smartthings-mqtt-bridge).

So I wrote my own set of device handler and app. This also gave me a chance to learn more about SmartThings structure.

---
## XIP Virtual Garage Door (Device Handler)
This represents a virtual garage door controller. 

It supports the following capabilities:
* Door Control
* Garage Door Control (this has been deprecated in favor of Door Control)
* Refresh
* Switch
* Temperature

And custom commands to interact with the SmartApp:
* finishOpening() - sets door state=open
* finishClosing() - sets door state=closed
* actuate() - sends event(switch=on) and then event(switch=off) after 1 second.

### Door Operation
The standard push opener either opens/closes the door and the door goes through the states:
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


## XIP Virtual Garage Door (Smart App)
The app is the brains linking the physical sensor and the virtual device.

It requires a
* Contact sensor device - this would be the physical sensor.
* Virtual door device - this would be the XIP Virtual Garage Door device.

And subscribes to contact and door messages on the above devices.


Messages:
* When physical contact opens/closes
    * Door has been opened by using physical switch or remote control or by the SmartApp, regardless sync the virtual door to be open or closed.
* If the virtual door is opening or closing
    * After a pre-defined interval, check if the door successfully closed or opened. Send a push notification in case of failure.

---
## Setup
Log into [SmartThings Groovy IDE](https://graph.api.smartthings.com/).
1. Go to My Device Handlers, click Create New Device Handler, use From Code and paste the [raw device handler code](https://raw.githubusercontent.com/InduPrakash/SmartThings/master/DeviceHandlers/VirtualGarageDoor.groovy). Save and Publish For Me.
2. Go to My Devices, create a new device. Give it a Name, Device Network id, Type "XIP Virtual Garage Door", Version "Published" and Hub.
3. Go to My SmartApps, click new SmartApp, use From Code and paste the [raw app code](https://raw.githubusercontent.com/InduPrakash/SmartThings/master/SmartApps/VirtualGarageDoorApp.groovy). Save and Publish For Me.
4. Launch the Classic SmartThings mobile app, go to SmartApps, add a SmartApp. Scroll down to My Apps and select XIP Virtual Garage Door, pick a contact sensor and virtual garage door device and you are set.
