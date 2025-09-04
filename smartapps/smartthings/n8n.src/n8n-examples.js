/**
 * n8n Integration Test and Example Usage
 * 
 * This file demonstrates how to use the n8n SmartThings integration
 * and serves as a reference for n8n workflow developers.
 */

// Example n8n HTTP Request Node configurations

// 1. List all switches
const listSwitches = {
    method: 'GET',
    url: 'https://graph.api.smartthings.com/api/smartapps/installations/{installation_id}/switches',
    headers: {
        'Authorization': 'Bearer {{$node["OAuth2 SmartThings"].json["access_token"]}}',
        'Content-Type': 'application/json'
    }
};

// 2. Get all devices with states
const getAllDevices = {
    method: 'GET',
    url: 'https://graph.api.smartthings.com/api/smartapps/installations/{installation_id}/devices',
    headers: {
        'Authorization': 'Bearer {{$node["OAuth2 SmartThings"].json["access_token"]}}',
        'Content-Type': 'application/json'
    }
};

// 3. Control a switch
const controlSwitch = {
    method: 'PUT',
    url: 'https://graph.api.smartthings.com/api/smartapps/installations/{installation_id}/switches/{{$node["Get Device ID"].json["id"]}}',
    headers: {
        'Authorization': 'Bearer {{$node["OAuth2 SmartThings"].json["access_token"]}}',
        'Content-Type': 'application/json'
    },
    body: {
        command: 'on'  // or 'off'
    }
};

// 4. Subscribe to motion sensor events
const subscribeToMotion = {
    method: 'POST',
    url: 'https://graph.api.smartthings.com/api/smartapps/installations/{installation_id}/motionSensors/subscription',
    headers: {
        'Authorization': 'Bearer {{$node["OAuth2 SmartThings"].json["access_token"]}}',
        'Content-Type': 'application/json'
    },
    body: {
        deviceId: '{{$node["Motion Sensor ID"].json["id"]}}',
        callbackUrl: 'https://your-n8n-instance.com/webhook/motion-detected'
    }
};

// 5. Example webhook payload you'll receive from SmartThings
const exampleWebhookPayload = {
    deviceId: "12345678-1234-1234-1234-123456789012",
    deviceName: "Living Room Motion Sensor",
    deviceType: "motionSensors",
    attribute: "motion",
    value: "active",
    unit: null,
    timestamp: 1634567890123,
    source: "smartthings",
    hubId: "87654321-4321-4321-4321-210987654321"
};

// 6. n8n Workflow Example: Turn on lights when motion detected
const n8nWorkflowExample = {
    "nodes": [
        {
            "parameters": {
                "httpMethod": "POST",
                "path": "motion-detected",
                "responseMode": "onReceived"
            },
            "name": "Motion Webhook",
            "type": "n8n-nodes-base.webhook"
        },
        {
            "parameters": {
                "conditions": {
                    "string": [
                        {
                            "value1": "={{$json[\"value\"]}}",
                            "operation": "equal",
                            "value2": "active"
                        }
                    ]
                }
            },
            "name": "Motion Active?",
            "type": "n8n-nodes-base.if"
        },
        {
            "parameters": {
                "url": "https://graph.api.smartthings.com/api/smartapps/installations/{installation_id}/switches/{{$node[\"Get Light ID\"].json[\"id\"]}}",
                "authentication": "predefinedCredentialType",
                "nodeCredentialType": "smartthingsOAuth2Api",
                "requestMethod": "PUT",
                "jsonParameters": true,
                "bodyParametersJson": "={\"command\": \"on\"}"
            },
            "name": "Turn On Light",
            "type": "n8n-nodes-base.httpRequest"
        }
    ],
    "connections": {
        "Motion Webhook": {
            "main": [
                [
                    {
                        "node": "Motion Active?",
                        "type": "main",
                        "index": 0
                    }
                ]
            ]
        },
        "Motion Active?": {
            "main": [
                [
                    {
                        "node": "Turn On Light",
                        "type": "main",
                        "index": 0
                    }
                ]
            ]
        }
    }
};

module.exports = {
    listSwitches,
    getAllDevices,
    controlSwitch,
    subscribeToMotion,
    exampleWebhookPayload,
    n8nWorkflowExample
};