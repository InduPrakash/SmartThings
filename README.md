# Virtual Garage Door
There are many good virtual garage door controller available but they lacked technical details or were just too complicated and I couldn't get them working with the standard push openers.

One additional caveat was that my physical opener device was interfaced through [SmartThings MQTT Bridge](https://github.com/stjohnjohnson/smartthings-mqtt-bridge).

So I wrote my own set of device handler and app.

---
## Device Handler
This represents a virtual garage door controller. 

It supports the following capabilities:
* Door Control
* Garage Door Control (this has been deprecated in favor of Door Control)
* Refresh
* Switch

And custom commands to interact with the SmartApp:
* finishOpening
* finishClosing
* actuate

### Door operation
The standard push opener either opens/closes the door; the door goes through the states:
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
* finishOpening() - sets door state=open
* finishClosing() - sets door state=closed
* actuate() - send event(switch=on) and then event(switch=off) after 1 second.


## Smart App


## Opener