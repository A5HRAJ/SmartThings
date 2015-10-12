metadata {
    definition (name: "Somfy Z-Wave Shades", namespace: "ash", author: "Ash Raj") {
        capability "Switch Level"
        capability "Switch"

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

    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state("off", label:'closed', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"opening")
            state("on", label:'open', action:"switch.off", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e", nextState:"closing")
            state("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffe71e")
            state("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffe71e")
}
        standardTile("on", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "on", label:'up', action:"switch.on", icon:"st.doors.garage.garage-opening"
        }
        standardTile("off", "device.switch", inactiveLabel: false, decoration: "flat") {
            state "off", label:'down', action:"switch.off", icon:"st.doors.garage.garage-closing"
        }
        standardTile("stop", "device.level", inactiveLabel: false, decoration: "flat") {
            state "default", label:'stop', action:"switch level.setLevel", icon:"st.Transportation.transportation13"
        }
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
            state "level", action:"switch level.setLevel"
        }

        main(["switch"])
        details(["switch", "on", "off", "stop"])
    }
}

def parse(String description) {
    def item1 = [
        canBeCurrentState: false,
        linkText: getLinkText(device),
        isStateChange: false,
        displayed: false,
        descriptionText: description,
        value:  description
    ]
    def result
    def cmd = zwave.parse(description, [0x20: 1, 0x26: 1, 0x70: 1])
    if (cmd) {
        result = createEvent(cmd, item1)
    }
    else {
        item1.displayed = displayed(description, item1.isStateChange)
        result = [item1]
    }
    log.debug "Parse returned ${result?.descriptionText}"
    result
}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, Map item1) {
    def result = doCreateEvent(cmd, item1)
    for (int i = 0; i < result.size(); i++) {
        result[i].type = "physical"
    }
    result
}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, Map item1) {
    def result = doCreateEvent(cmd, item1)
    for (int i = 0; i < result.size(); i++) {
        result[i].type = "physical"
    }
    result
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd, Map item1) {
    []
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd, Map item1) {
    [response(zwave.basicV1.basicGet())]
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd, Map item1) {
    def result = doCreateEvent(cmd, item1)
    for (int i = 0; i < result.size(); i++) {
        result[i].type = "physical"
    }
    result
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, Map item1) {
    def result = doCreateEvent(cmd, item1)
    result[0].descriptionText = "${item1.linkText} is ${item1.value}"
    result[0].handlerName = cmd.value ? "statusOn" : "statusOff"
    for (int i = 0; i < result.size(); i++) {
        result[i].type = "digital"
    }
    result
}

def doCreateEvent(physicalgraph.zwave.Command cmd, Map item1) {
    def result = [item1]

    item1.name = "switch"
    item1.value = cmd.value ? "on" : "off"
    item1.handlerName = item1.value
    item1.descriptionText = "${item1.linkText} was turned ${item1.value}"
    item1.canBeCurrentState = true
    item1.isStateChange = isStateChange(device, item1.name, item1.value)
    item1.displayed = item1.isStateChange

    if (cmd.value >= 5) {
        def item2 = new LinkedHashMap(item1)
        item2.name = "level"
        item2.value = cmd.value as String
        item2.unit = "%"
        item2.descriptionText = "${item1.linkText} dimmed ${item2.value} %"
        item2.canBeCurrentState = true
        item2.isStateChange = isStateChange(device, item2.name, item2.value)
        item2.displayed = false
        result << item2
    }
    result
}
def createEvent(physicalgraph.zwave.Command cmd,  Map map) {
    // Handles any Z-Wave commands we aren't interested in
    log.debug "UNHANDLED COMMAND $cmd"
}

def on() {
    level = 99
    delayBetween([
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format(),
        sendEvent(name: "switch", value: on)
    ], 5000)
}

def off() {
    delayBetween([
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format(),
        sendEvent(name: "switch", value: off)
    ], 5000)
}

def setLevel() {
    delayBetween([
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format(),
        sendEvent(name: "switch", value: on)
    ], 5000)
}

def setLevel(level) {
    if (level >= 75) {
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format()
    } else if (level <= 25) {
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format()
    } else {
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format()
    }
}

def setLevel(level, duration) {
    if (level >= 75) {
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0xFF).format()
    } else if (level <= 25) {
        zwave.switchMultilevelV1.switchMultilevelSet(value: 0x00).format()
    } else {
        zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format()
    }
}
