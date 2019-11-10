/**
 *  Copyright 2019 G.Brown (MorkOz)
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
 *  Adapted and modified from code written by at9, motley74, sticks18 and AnotherUser 
 *  Original sources: 
 *  https://github.com/at-9/hubitat/blob/master/Drivers/at-9-Zemismart-3-Gang-Sticker-Switch
 *  https://github.com/motley74/SmartThingsPublic/blob/master/devicetypes/motley74/osram-lightify-dimming-switch.src/osram-lightify-dimming-switch.groovy
 *  https://github.com/AnotherUser17/SmartThingsPublic-1/blob/master/devicetypes/AnotherUser/osram-lightify-4x-switch.src/osram-lightify-4x-switch.groovy
 *
 *  Version Author              Note
 *  0.1     G.Brown (MorkOz)    Initial release
 *
 */
import hubitat.zigbee.zcl.DataType

metadata {
    definition (name: "Zemismart Zigbee Button Remote", namespace: "gbrown", author: "G.Brown") {
    capability "Actuator"
    capability "PushableButton"
    capability "HoldableButton"
    capability "DoubleTapableButton"
    capability "Configuration"
    capability "Refresh"
    capability "Sensor"

        fingerprint inClusters: "0000,0001,0006", outClusters: "0019", manufacturer: "_TYZB02_key8kk7r", model: "TS0041"
        fingerprint inClusters: "0000,0001,0006", outClusters: "0019", manufacturer: "_TYZB02_key8kk7r", model: "TS0042"
        fingerprint inClusters: "0000,0001,0006", outClusters: "0019", manufacturer: "_TYZB02_key8kk7r", model: "TS0043"		
    }

	preferences {
        input name: "debugEnable", type: "bool", title: "Enable debug logging", defaultValue: false
	}

}

private sendButtonNumber() {
    def remoteModel = device.getDataValue("model")
    switch(remoteModel){
        case "TS0041":
            sendEvent(name: "numberOfButtons", value: 1, isStateChange: true)
            break
        case "TS0042":
            sendEvent(name: "numberOfButtons", value: 2, isStateChange: true)
            break
        case "TS0043":
            sendEvent(name: "numberOfButtons", value: 3, isStateChange: true)
            break
    }
}

def installed() {
    sendButtonNumber
    state.start = now()
}

def updated() {
    sendButtonNumber
}

def refresh() {
   // read battery level attributes
      return zigbee.readAttribute(0x0001, 0x0020) + zigbee.configureReporting(0x0001, 0x0020, 0x20, 3600, 21600, 0x01)
  // this device doesn't support 0021
      zigbee.readAttribute(0x0001, 0x0021)
}

def configure() {
    sendButtonNumber
   
    def configCmds = []
  for (int endpoint=1; endpoint<=3; endpoint++) {
    def list = ["0006", "0001", "0000"]
    // the config to the switch
    for (cluster in list) {
      configCmds.add("zdo bind 0x${device.deviceNetworkId} 0x0${endpoint} 0x01 0x${cluster} {${device.zigbeeId}} {}")
    }
  }
  return configCmds
}

def parse(String description) {
    if ((description?.startsWith("catchall:")) || (description?.startsWith("read attr -"))) {
        def parsedMap = zigbee.parseDescriptionAsMap(description)
        if (debugEnable){
            log.debug("Message Map: '$parsedMap'")
        }
        switch(parsedMap.sourceEndpoint) {
            case "03": 
                button = "1"
                break
            case "02": 
                button = "2"
                break
            case "01": 
                button = "3"
                break
        }
        switch(parsedMap.data) {
            case "[00]": 
                name = "pushed" 
                break
            case "[01]": 
                name = "doubleTapped" 
                break
            case "[02]": 
                name = "held" 
                break
    }
        sendEvent(name: name, value: button, descriptionText: "Button $button was $name",isStateChange:true)
    }
    return
}
