/**
 * 
 * https://community.smartthings.com/t/my-somfy-smartthings-integration/13492
 * Modified ERS 1/28/2016
 * Changes:
 *	Shows if shades are in a my/stop state
 *	Multi-tile control
 *	Disable dual setLevel operations
 */
  metadata {
    definition (name: "Somfy Z-Wave Shades Multi tile", namespace: "ash", author: "Ash Raj") {
        capability "Switch Level"
        capability "Switch"

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
	multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4){
		tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
		    attributeState("on", label:'open', action:"switch.off", icon:"st.doors.garage.garage-open", backgroundColor:"#ffdf3f")
		    attributeState("off", label:'closed', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#194775")
		    attributeState("default", label:'stop/my', action:"switch level.setLevel", icon:"st.doors.garage.garage-open", backgroundColor:"#ffdf3f")
		}
		tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState("level", action:"switch level.setLevel")
		}
		tileAttribute ("device.speedLevel", key: "VALUE_CONTROL"){
                   attributeState("level", action: "levelOpenClose")
                 }
	}


        standardTile("switchmain", "device.switch", width: 2, height: 2) {
            state "on", label:'open', action:"switch.off", icon:"st.doors.garage.garage-open", backgroundColor:"#ffdf3f"
            state "off", label:'closed', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#194775"
            state "default", label:'stop/my', action:"switch level.setLevel", icon:"st.doors.garage.garage-open", backgroundColor:"#ffdf3f"
        }

        standardTile("on", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("on", label:'up', action:"switch.on", icon:"st.doors.garage.garage-opening")
        }
        standardTile("off", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("off", label:'down', action:"switch.off", icon:"st.doors.garage.garage-closing")
        }
        standardTile("stop", "device.level", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state("default", label:'stop/my', action:"switch level.setLevel", icon:"st.Transportation.transportation13")
        }
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
            state("level", action:"switch level.setLevel")
        }

        main(["switchmain"])
        details(["switch", "on", "off", "stop"])
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

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
    def result
    log.debug "Basic report cmd.value:  ${cmd.value}"
    if (cmd.value == 0) {
        //result = createEvent(name: "switch", value: "off")
    } else {
        //result = createEvent(name: "switch", value: "on")
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd)
{
    def result
    log.debug "Multilevel report cmd.value:  ${cmd.value}"
    if (cmd.value == 0) {
        result = createEvent(name: "switch", value: "off")
    } else if (cmd.value == 0xFF) {
        result = createEvent(name: "switch", value: "on")
    } else {
	result = createEvent(name: "switch", value: "default")
    }
    return result
}

def on() {
    int level = 100
    log.debug "on()"
    setLevel(level) 
}

def off() {
    int level = 0
    log.debug "off()"
    setLevel(level) 
}

def setLevel() {
    log.debug "setLevel()"
    setLevel(50) 
}

def setLevel(level) {
    log.debug "setLevel(level)  {$level}"
    int newlevel = level

    if (level > null) {

	sendEvent(name: "level", value: level)

	if (level >= 75) {

	    delayBetween([
		zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format(),
		sendEvent(name: "switch", value: "on")
	    ], 5000)
	    newlevel = 100

	} else if (level <= 25) {

	    delayBetween([
    		zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format(),
    		sendEvent(name: "switch", value: "off")
	    ], 5000)
	    newlevel = 0

	} else {

	    delayBetween([
		zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),
		sendEvent(name: "switch", value: "default")
	    ], 5000)

	}
	if (newlevel != level) sendEvent(name: "level", value: newlevel)
    }
}

def setLevel(level, duration) {
    log.debug "setLevel(level, duration)  {$level} ${duration}"
    setLevel(level)
    return

    if (level >= 75) {
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format()
    } else if (level <= 25) {
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format()
    } else {
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format()
    }
}
