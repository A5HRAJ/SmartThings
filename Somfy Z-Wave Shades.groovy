metadata {
	definition (name: "Somfy Z-Wave Shades", namespace: "NOTashwin", author: "Ashwin Rajani") {
		capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
        
        fingerprint deviceId: "0x1105", inClusters: "0x2C, 0x72, 0x26, 0x20, 0x25, 0x2B, 0x86"             
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
        	state "on", label:'open', action:"switch.off", icon:"st.doors.garage.garage-open", backgroundColor:"#ffdf3f"
        	state "off", label:'closed', action:"switch.on", icon:"st.doors.garage.garage-closed", backgroundColor:"#194775"
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
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch"])
		details(["switch", "on", "off", "stop", "refresh"])
	}
}

def on() {
	zwave.basicV1.basicSet(value: 0xFF).format()
}

def off() {
	zwave.basicV1.basicSet(value: 0x00).format()
}

def setLevel() {
	zwave.switchMultilevelV1.switchMultilevelStopLevelChange().format()
}

def refresh() {
	// zwave.switchBinaryV1.switchBinaryGet().format()
}

def poll() {
	// zwave.switchBinaryV1.switchBinaryGet().format()
}