/**
 *  LeakSmart Water Valve v 1.0.3
 *  
 *  Capabilities:
 *      Configuration, Refresh, Switch, Valve, Polling
 *
 *  Author: 
 *     Kevin LaFramboise (krlaframboise)
 *
 *  Url to Documentation:
 *      
 *
 *  Changelog:
 *
 *    1.0.3 (05/22/2016)
 *      - Added last poll event creation.
 *
 *    1.0.2 (05/22/2016)
 *      - Fixed Contact event creation
 *
 *    1.0.1 (05/21/2016)
 *      - Initial Release
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "LeakSmart Water Valve", namespace: "krlaframboise", author: "Kevin LaFramboise") {
		capability "Actuator"
		//capability "Battery"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Valve"
		capability "Polling"

		attribute "lastPoll", "number"	
		
		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0006,0020,0B02", outClusters: "0019"
	}
	
	preferences {
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: false, 
			required: false
	}
	
	tiles(scale: 2) {
		standardTile("contact", "device.contact", width: 4, height: 4, canChangeIcon: true) {
			state "closed", 
				label:'Closed', 
				action: "valve.open", 
				nextState: "opening", 
				icon:"st.valves.water.closed", 
				backgroundColor:"#e86d13"
			state "opening", 
				label:'Opening', 
				action: "valve.close", 
				icon:"st.valves.water.open", 
				backgroundColor:"#ffe71e"
			state "open", 
				label:'Open', 
				action: "valve.close", 
				nextState: "closing",
				icon:"st.valves.water.open", 
				backgroundColor:"#53a7c0"
			state "closing", 
				label:'Closing', 
				action: "valve.open", 
				icon:"st.valves.water.open", 
				backgroundColor:"#ffe71e"
		}
		standardTile("refresh", "device.refresh", width: 2, height: 2, canChangeIcon: true) {
			state "default", 
				label: 'Refresh', 
				action: "refresh.refresh", 
				icon: ""			
		}		
		// valueTile("battery", "device.battery", width: 2, height: 2, canChangeIcon: true) {
			// state "battery", 
				// label: 'Battery ${currentValue}%',
				// unit: "%"
				// icon: ""			
		// }
		main "contact"
		details(["contact", "refresh", "battery"])
	}
}

def updated() {
	if (!state.configured) {		
		return response(configure())
	}
}

def parse(String description) {
	def result = []
	def evt = zigbee.getEvent(description)
	if (evt) {
		if (evt.name == "switch") {
			def val = (evt.value == "on") ? "open" : "closed"
			logDebug "Valve is $val"
			result << createEvent(name: "contact", value: val)
			result << createEvent(name: "lastPoll",value: new Date().time, isStateChange: true, displayed: false)
		}
		else {
			logDebug "Ignored Event: $evt"
		}
		result << createEvent(evt)
	}
	else {
		def map = zigbee.parseDescriptionAsMap(description)
		if (map) {			
			logDebug "Ignored Map: $map"
		}
		else { 
			logDebug "Ignored Description: $description"
		}
	}	
	return result
}

def on() {
	open()
}

def off() {
	close()
}

def open() {
	logDebug "Opening"
	zigbee.on()
}

def close() {
	logDebug "Closing"
	zigbee.off()
}

def poll() {
	def minimumPollMinutes = 180 // 3 Hours
	def lastPoll = device.currentValue("lastPoll")
	if ((new Date().time - lastPoll) > (minimumPollMinutes * 60 * 1000)) {
		logDebug "Poll: Refreshing because lastPoll was more than ${minimumPollMinutes} minutes ago."
		return refresh()
	}
	else {
		logDebug "Poll: Skipped because lastPoll was within ${minimumPollMinutes} minutes"
	}
}

def refresh() {
	logDebug "Refreshing"	
	return zigbee.onOffRefresh() + 
		zigbee.onOffConfig() + 
		configureBatteryReporting()		
}

def configure() {
	logDebug "Configuring Reporting and Bindings."
	state.configured = true
	return zigbee.onOffConfig() + 
		configureBatteryReporting() +
		zigbee.onOffRefresh() 
}

private configureBatteryReporting() {
	def interval = 14400 // 4 Hours	
	zigbee.configureReporting(0x0001, 0x0020, 0x20, 30, interval, 0x01)
}

private logDebug(msg) {
	if (settings.debugOutput != false) {
		log.debug "$msg"
	}
}