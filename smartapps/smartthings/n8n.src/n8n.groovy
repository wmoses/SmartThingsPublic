/**
 *  Copyright 2023 SmartThings
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
 *  n8n Integration Application
 *
 *  Author: SmartThings
 *
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  Device Type          | Attribute Name | Commands                 | Attribute Values
 *  ---------------------+----------------+--------------------------+------------------------------------
 *  switches             | switch         | on, off                  | on, off
 *  motionSensors        | motion         |                          | active, inactive
 *  contactSensors       | contact        |                          | open, closed
 *  presenceSensors      | presence       |                          | present, 'not present'
 *  temperatureSensors   | temperature    |                          | <numeric, F or C according to unit>
 *  accelerationSensors  | acceleration   |                          | active, inactive
 *  waterSensors         | water          |                          | wet, dry
 *  lightSensors         | illuminance    |                          | <numeric, lux>
 *  humiditySensors      | humidity       |                          | <numeric, percent>
 *  alarms               | alarm          | strobe, siren, both, off | strobe, siren, both, off
 *  locks                | lock           | lock, unlock             | locked, unlocked
 *  ---------------------+----------------+--------------------------+------------------------------------
 */

definition(
    name: "n8n Integration",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Connect your SmartThings devices to n8n workflows for advanced automation.",
    category: "SmartThings Internal",
    iconUrl: "https://raw.githubusercontent.com/n8n-io/n8n/master/assets/n8n-logo.png",
    iconX2Url: "https://raw.githubusercontent.com/n8n-io/n8n/master/assets/n8n-logo.png",
    oauth: [displayName: "n8n Integration", displayLink: "https://n8n.io"]
)

preferences {
	section("Allow n8n to control and monitor these devices...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
		input "motionSensors", "capability.motionSensor", title: "Which Motion Sensors?", multiple: true, required: false
		input "contactSensors", "capability.contactSensor", title: "Which Contact Sensors?", multiple: true, required: false
		input "presenceSensors", "capability.presenceSensor", title: "Which Presence Sensors?", multiple: true, required: false
		input "temperatureSensors", "capability.temperatureMeasurement", title: "Which Temperature Sensors?", multiple: true, required: false
		input "accelerationSensors", "capability.accelerationSensor", title: "Which Vibration Sensors?", multiple: true, required: false
		input "waterSensors", "capability.waterSensor", title: "Which Water Sensors?", multiple: true, required: false
		input "lightSensors", "capability.illuminanceMeasurement", title: "Which Light Sensors?", multiple: true, required: false
		input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Which Relative Humidity Sensors?", multiple: true, required: false
		input "alarms", "capability.alarm", title: "Which Sirens?", multiple: true, required: false
		input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
	}
}

mappings {
	// Device listings
	path("/:deviceType") {
		action: [
			GET: "list"
		]
	}
	
	// Device states
	path("/:deviceType/states") {
		action: [
			GET: "listStates"
		]
	}
	
	// Webhook subscriptions
	path("/:deviceType/subscription") {
		action: [
			POST: "addSubscription"
		]
	}
	
	path("/:deviceType/subscriptions/:id") {
		action: [
			DELETE: "removeSubscription"
		]
	}
	
	// Individual device control and status
	path("/:deviceType/:id") {
		action: [
			GET: "show",
			PUT: "update"
		]
	}
	
	// Subscription management
	path("/subscriptions") {
		action: [
			GET: "listSubscriptions"
		]
	}
	
	// n8n specific endpoints
	path("/webhook/:deviceType/:id") {
		action: [
			POST: "deviceWebhook"
		]
	}
	
	path("/devices") {
		action: [
			GET: "listAllDevices"
		]
	}
}

def installed() {
	log.debug "n8n Integration installed with settings: ${settings}"
}

def updated() {
	def currentDeviceIds = settings.collect { k, devices -> devices }.flatten().collect { it.id }.unique()
	def subscriptionDevicesToRemove = app.subscriptions*.device.findAll { device ->
		!currentDeviceIds.contains(device.id)
	}
	subscriptionDevicesToRemove.each { device ->
		log.debug "Removing $device.displayName subscription"
		state.remove(device.id)
		unsubscribe(device)
	}
	log.debug "n8n Integration updated with settings: ${settings}"
}

def list() {
	log.debug "[n8n] list, params: ${params}"
	def type = params.deviceType
	settings[type]?.collect{deviceItem(it)} ?: []
}

def listStates() {
	log.debug "[n8n] states, params: ${params}"
	def type = params.deviceType
	def attributeName = attributeFor(type)
	settings[type]?.collect{deviceState(it, it.currentState(attributeName))} ?: []
}

def listAllDevices() {
	log.debug "[n8n] listAllDevices"
	def allDevices = []
	settings.each { deviceType, devices ->
		if (devices) {
			devices.each { device ->
				def attributeName = attributeFor(deviceType)
				def currentState = device.currentState(attributeName)
				allDevices.add([
					deviceType: deviceType,
					device: deviceItem(device),
					state: currentState ? [name: currentState.name, value: currentState.value, timestamp: currentState.date.time] : null
				])
			}
		}
	}
	return allDevices
}

def listSubscriptions() {
	state
}

def update() {
	def type = params.deviceType
	def data = request.JSON
	def devices = settings[type]
	def device = settings[type]?.find { it.id == params.id }
	def command = data.command

	log.debug "[n8n] update, params: ${params}, request: ${data}, devices: ${devices*.id}"
	
	if (!device) {
		httpError(404, "Device not found")
	} 
	
	if (validateCommand(device, type, command)) {
		device."$command"()
		
		// Return updated device state
		def attributeName = attributeFor(type)
		def currentState = device.currentState(attributeName)
		return [
			success: true,
			device: deviceItem(device),
			state: currentState ? [name: currentState.name, value: currentState.value, timestamp: currentState.date.time] : null
		]
	} else {
		httpError(403, "Access denied. This command is not supported by current capability.")
	}
}

def show() {
	def type = params.deviceType
	def devices = settings[type]
	def device = devices.find { it.id == params.id }

	log.debug "[n8n] show, params: ${params}, devices: ${devices*.id}"
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = attributeFor(type)
		def s = device.currentState(attributeName)
		def deviceInfo = deviceState(device, s)
		
		// Add additional device information useful for n8n
		deviceInfo.capabilities = device.capabilities.collect { it.name }
		deviceInfo.commands = getDeviceCapabilityCommands(device.capabilities)
		
		return deviceInfo
	}
}

def addSubscription() {
	log.debug "[n8n] addSubscription"
	def type = params.deviceType
	def data = request.JSON
	def attribute = attributeFor(type)
	def devices = settings[type]
	def deviceId = data.deviceId
	def callbackUrl = data.callbackUrl
	def device = devices.find { it.id == deviceId }

	log.debug "[n8n] addSubscription, params: ${params}, request: ${data}, device: ${device}"
	if (device) {
		log.debug "Adding n8n subscription for ${device.displayName} to ${callbackUrl}"
		state[deviceId] = [callbackUrl: callbackUrl, deviceType: type]
		subscribe(device, attribute, deviceHandler)
		
		return [
			success: true,
			message: "Subscription added for ${device.displayName}",
			deviceId: deviceId,
			attribute: attribute
		]
	} else {
		httpError(404, "Device not found")
	}
}

def removeSubscription() {
	def type = params.deviceType
	def devices = settings[type]
	def deviceId = params.id
	def device = devices.find { it.id == deviceId }

	log.debug "[n8n] removeSubscription, params: ${params}, device: ${device}"
	if (device) {
		log.debug "Removing n8n subscription for $device.displayName"
		state.remove(device.id)
		unsubscribe(device)
		
		return [
			success: true,
			message: "Subscription removed for ${device.displayName}",
			deviceId: deviceId
		]
	} else {
		httpError(404, "Device not found")
	}
}

def deviceWebhook() {
	def type = params.deviceType
	def deviceId = params.id
	def data = request.JSON
	def devices = settings[type]
	def device = devices.find { it.id == deviceId }
	
	log.debug "[n8n] deviceWebhook, params: ${params}, data: ${data}, device: ${device}"
	
	if (!device) {
		httpError(404, "Device not found")
	}
	
	// Execute command if provided
	if (data.command && validateCommand(device, type, data.command)) {
		device."${data.command}"()
	}
	
	// Return current device state
	def attributeName = attributeFor(type)
	def currentState = device.currentState(attributeName)
	
	return [
		success: true,
		device: deviceItem(device),
		state: currentState ? [name: currentState.name, value: currentState.value, timestamp: currentState.date.time] : null,
		webhook: true
	]
}

def deviceHandler(evt) {
	def deviceInfo = state[evt.deviceId]
	if (deviceInfo) {
		def payload = [
			deviceId: evt.deviceId,
			deviceName: evt.displayName,
			deviceType: deviceInfo.deviceType,
			attribute: evt.name,
			value: evt.value,
			unit: evt.unit,
			timestamp: evt.date.time,
			source: "smartthings",
			hubId: evt.hubId
		]
		
		try {
			httpPostJson(uri: deviceInfo.callbackUrl, path: '', body: payload) {
				log.debug "[n8n] Event data successfully posted to n8n workflow"
			}
		} catch (groovyx.net.http.ResponseParseException e) {
			log.debug("Error parsing n8n response: ${e}")
		} catch (Exception e) {
			log.error("Error posting to n8n webhook: ${e}")
		}
	} else {
		log.debug "[n8n] No subscribed device found for ${evt.deviceId}"
	}
}

/**
 * Validating the command passed by the user based on capability.
 * @return boolean
 */
def validateCommand(device, deviceType, command) {
	def capabilityCommands = getDeviceCapabilityCommands(device.capabilities)
	def currentDeviceCapability = getCapabilityName(deviceType)
	if (capabilityCommands[currentDeviceCapability]) {
		return command in capabilityCommands[currentDeviceCapability] ? true : false
	} else {
		// Handling other device types here, which don't accept commands
		httpError(400, "Bad request - device type does not support commands.")
	}
}

/**
 * Need to get the attribute name to do the lookup. Only
 * doing it for the device types which accept commands
 * @return attribute name of the device type
 */
def getCapabilityName(type) {
    switch(type) {
		case "switches":
			return "Switch"
		case "alarms":
			return "Alarm"
		case "locks":
			return "Lock"
		default:
			return type
	}
}

/**
 * Constructing the map over here of
 * supported commands by device capability
 * @return a map of device capability -> supported commands
 */
def getDeviceCapabilityCommands(deviceCapabilities) {
	def map = [:]
	deviceCapabilities.collect {
		map[it.name] = it.commands.collect{ it.name.toString() }
	}
	return map
}

private deviceItem(it) {
	it ? [id: it.id, label: it.displayName, name: it.name] : null
}

private deviceState(device, s) {
	device && s ? [id: device.id, label: device.displayName, name: s.name, value: s.value, unit: s.unit, timestamp: s.date.time] : null
}

private attributeFor(type) {
	switch (type) {
		case "switches":
			return "switch"
		case "locks":
			return "lock"
		case "alarms":
			return "alarm"
		case "lightSensors":
			return "illuminance"
		default:
			return type - "Sensors"
	}
}