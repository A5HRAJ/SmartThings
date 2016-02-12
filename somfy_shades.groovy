/**
 * 
 * https://community.smartthings.com/t/my-somfy-smartthings-integration/13492
 * Modified ERS 2/12/2016
 * Changes:
 *        Shows if shades are in a my/stop state
 *        Multi-tile control
 *        Disable dual setLevel operations
 */
  metadata {
    definition (name: "Somfy Z-Wave Shades Multi tile", namespace: "ash", author: "Ash Raj") {
        capability "Switch Level"
        capability "Switch"
        capability "Window Shade"
        //capability "Polling"
        capability "Refresh"
        capability "Actuator"

        command "levelOpenClose"

        fingerprint deviceId: "0x1105", inClusters: "0x2C, 0x72, 0x26, 0x20, 0x25, 0x2B, 0x86"
    }

    simulator {
        status "on":  "command: 2003, payload: FF"
        status "off": "command: 2003, payload: 00"
        status "09%": "command: 2003, payload: 09"
        status "10%": "command: 2003, payload: 0A"
        status "33%": "command: 2003, payload: 21"
        status "66%": "command: 2003, payload: 42"
        status "99%": "command: 2003, payload: 63"
        
        // reply messages
        reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
        reply "200100,delay 5000,2602": "command: 2603, payload: 00"
        reply "200119,delay 5000,2602": "command: 2603, payload: 19"
        reply "200132,delay 5000,2602": "command: 2603, payload: 32"
        reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
        reply "200163,delay 5000,2602": "command: 2603, payload: 63"
    }


    tiles(scale: 2) {
        multiAttributeTile(name:"shade", type: "lighting", width: 6, height: 4) {
            tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
                attributeState("unknown", label:'${name}', action:"refresh.refresh", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e")
                attributeState("closed", label:'${name}', action:"open", icon:"st.doors.garage.garage-closed", backgroundColor:"#bbbbdd", nextState: "opening")
                attributeState("open", label:'${name}', action:"close", icon:"st.doors.garage.garage-open", backgroundColor:"#ffcc33", nextState: "closing")
                attributeState("partially open", label:'stop/my', action:"presetPosition", icon:"st.doors.garage.garage-open", backgroundColor:"#ffcc33")
                attributeState("closing", label:'${name}', action:"presetPosition", icon:"st.doors.garage.garage-closing", backgroundColor:"#bbbbdd")
                attributeState("opening", label:'${name}', action:"presetPosition", icon:"st.doors.garage.garage-opening", backgroundColor:"#ffcc33")
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState("level", action:"switch level.setLevel")
            }
            tileAttribute ("device.speedLevel", key: "VALUE_CONTROL") {
                attributeState("level", action: "levelOpenClose")
            }
        }

        standardTile("switchmain", "device.switch", width: 2, height: 2) {
            state "on", label:'open', action:"switch.off", icon:"st.doors.garage.garage-open", backgroundColor:"#ffcc33"
            state "off", label:'closed', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#bbbbdd"
            state "default", label:'stop/my', action:"presetPosition", icon:"st.doors.garage.garage-open", backgroundColor:"#ffcc33"
        }

        standardTile("on", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("on", label:'open', action:"switch.on", icon:"st.doors.garage.garage-opening")
        }
        standardTile("off", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("off", label:'close', action:"switch.off", icon:"st.doors.garage.garage-closing")
        }
        standardTile("stop", "device.level", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("default", label:'stop/my', action:"switch level.setLevel", icon:"st.Transportation.transportation13")
        }
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
            state("level", action:"switch level.setLevel")
        }

        standardTile("refresh", "command.refresh", width:2, height:2, inactiveLabel: false, decoration: "flat") {
                state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

//  Poll provides data, but the ZRTSII does not provide accurate status
//
//      standardTile("poll", "command.poll", width:2, height:2, inactiveLabel: false, decoration: "flat") {
//              state "default", label:'poll', action:"poll", icon:"st.secondary.poll"
//      }

        main(["switchmain"])
        details(["shade", "on", "off", "stop"])
    }
}

def parse(String description) {
    description
    def result = null
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
    log.debug "Parsed ${description} to ${cmd}"
    if (cmd) {
        result = zwaveEvent(cmd)
        log.debug "zwaveEvent( ${cmd} ) returned ${result.inspect()}"
    } else {
        log.debug "Non-parsed event: ${description}"
    }
    return result
}

def levelOpenClose(value) {
    log.trace "levelOpenClose called with value $value"
    if (value) {
        on()
    } else {
        off()
    }
}

// Somfy ZRTSII does not report accurate status for the device.
// This device handler maintains an internal view of device status based on last command
// reissuing a command to the shade (up, down, my (when stopped)) does not move the shade if it is already in that position
// My/stop command does different actions depending if the shade is idle (go to MY position) or moving (stop)

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
    def result = []
    log.trace "Basic report cmd.value:  ${cmd.value}"
    if (cmd.value == 0) {
        //result << createEvent(name: "switch", value: "off")
        log.debug "Reported state is closed; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    } else if (cmd.value == 0xFF) {
        //result << createEvent(name: "switch", value: "on")
        log.debug "Reported state is open; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    } else {  // This has never happend
        //result << createEvent(name: "switch", value: "default")
        log.debug "Reported state is neither open or closed; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    def result = []
    log.debug "SwitchBinaryReport cmd.value:  ${cmd.value}"
    
    if (cmd.value == 0) {
        log.debug "Reported state is closed; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    } else if (cmd.value == 0xFF) {
        log.debug "Reported state is open; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    } else {  // this has never happened
        log.debug "Reported state is neither open or closed; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    }
    
    //result << createEvent(name:"switch", value: cmd.value ? "on" : "off")
    //result << createEvent(name: "level",value: cmd.value, unit:"%",
        //descriptionText:"${device.displayName} dimmed ${cmd.value==255 ? 100 : cmd.value}%")
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd)
{
    def result = []
    log.trace "SwitchMultilevelReport cmd.value:  ${cmd.value}"
    
    if (cmd.value == 0) {
        //result << createEvent(name: "switch", value: "off")
        log.debug "Reported state is closed; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    } else if (cmd.value == 0xFF) {
        //result << createEvent(name: "switch", value: "on")
        log.debug "Reported state is open; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    } else {
       //result << createEvent(name: "switch", value: "default")
       log.debug "Reported state is neither open or closed; device is ${device.latestValue('switch')}  ${device.latestValue('level')} "
    }
    //result << createEvent(name: "level",value: cmd.value, unit:"%",
      //descriptionText:"${device.displayName} dimmed ${cmd.value==255 ? 100 : cmd.value}%")
    return result
}

def on() {
    int level = 100
    log.trace "on() treated as open()"
    setLevel(level) 
}

def off() {
    int level = 0
    log.trace "off() treated as close()"
    setLevel(level) 
}

def setLevel() {
    log.trace "setLevel() treated as my/stop"
    setLevel(50) 
}

def open() {
    log.trace "open()"
    on()
}

def close() {
    log.trace "close()"
    off()
}

def presetPosition() {
    log.trace "presetPosition() treated as my/stop"
    setLevel()
}

def refresh() {
    log.trace "refresh()"
    delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(),
        zwave.switchMultilevelV1.switchMultilevelGet().format(),
        //zwave.meterV2.meterGet(scale: 0).format(),      // get kWh
        //zwave.meterV2.meterGet(scale: 2).format(),      // get Watts
        //zwave.sensorMultilevelV1.sensorMultilevelGet().format(),
        //zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType:1, scale:1).format(),  // get temp in Fahrenheit
        //zwave.batteryV1.batteryGet().format(),
        zwave.basicV1.basicGet().format(),
    ], 3000)
}

// If you add the Polling capability to your device type, this command
// will be called approximately every 5 minutes to check the device's state
// zrtsII does not provide accurate status of shade position
//def poll() {
//        log.trace "Poll"
//        zwave.basicV1.basicGet().format()
//}

def setLevel(level) {
    log.trace "setLevel(level)  {$level}"
    log.debug "level.inspect " + level.inspect()

    int newlevel = level

    if (level > null) {

        sendEvent(name: "level", value: level)

        if (level >= 75) {
            sendEvent(name: "windowShade", value: "opening")
            newlevel = 100
            delayBetween([
                zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format(),
                sendEvent(name: "switch", value: "on"),
                sendEvent(name: "windowShade", value: "open")
            ], 5000)
        } else if (level <= 25) {
            sendEvent(name: "windowShade", value: "closing")
            newlevel = 0
            delayBetween([
                zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format(),
                sendEvent(name: "switch", value: "off"),
                sendEvent(name: "windowShade", value: "closed")
            ], 5000)
        } else {
            delayBetween([
                zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),
                sendEvent(name: "switch", value: "default"),
                sendEvent(name: "windowShade", value: "partially open")
            ], 5000)
        }

        // this code below causes commands not be sent/received by the Somfy ZRTSII - assume delayBetween is asynchronous...

        //log.trace("finished level adjust")
        //if (newlevel != level) { 
            //log.trace("finished level adjust1")
            //delayBetween([
                //sendEvent(name: "level", value: newlevel)
            //], 1000)
        //}
    }
}

// this appears to never be called

//def setLevel(level, duration) {
//    log.trace "setLevel(level, duration)  {$level} ${duration}"
//    setLevel(level)
//    return
//}
