# n8n SmartThings Integration

This SmartApp provides a comprehensive integration between SmartThings and n8n workflow automation platform.

## Features

- **OAuth Authentication**: Secure connection between SmartThings and n8n
- **Device Control**: Control SmartThings devices from n8n workflows
- **Real-time Notifications**: Receive device state changes via webhooks
- **Multiple Device Types**: Support for switches, sensors, locks, alarms, and more
- **RESTful API**: Clean REST endpoints for easy integration

## Supported Device Types

| Device Type | Capabilities | Commands | Attributes |
|-------------|-------------|----------|------------|
| Switches | Switch | on, off | on, off |
| Motion Sensors | Motion Sensor | - | active, inactive |
| Contact Sensors | Contact Sensor | - | open, closed |
| Presence Sensors | Presence Sensor | - | present, not present |
| Temperature Sensors | Temperature Measurement | - | temperature value |
| Light Sensors | Illuminance Measurement | - | lux value |
| Humidity Sensors | Humidity Measurement | - | humidity percentage |
| Water Sensors | Water Sensor | - | wet, dry |
| Locks | Lock | lock, unlock | locked, unlocked |
| Alarms | Alarm | strobe, siren, both, off | alarm state |

## API Endpoints

### Device Management
- `GET /:deviceType` - List devices of a specific type
- `GET /:deviceType/states` - Get current states of all devices of a type
- `GET /:deviceType/:id` - Get specific device details and state
- `PUT /:deviceType/:id` - Send command to specific device
- `GET /devices` - List all devices with their current states

### Webhook Subscriptions
- `POST /:deviceType/subscription` - Subscribe to device state changes
- `DELETE /:deviceType/subscriptions/:id` - Remove device subscription
- `GET /subscriptions` - List all active subscriptions

### n8n Specific
- `POST /webhook/:deviceType/:id` - Direct webhook endpoint for device interaction

## Setup Instructions

1. Install the n8n Integration SmartApp in your SmartThings account
2. Configure OAuth credentials in n8n
3. Select the devices you want to integrate
4. Use the provided API endpoints in your n8n workflows

## Example Usage in n8n

### Control a Switch
```
PUT /switches/{device_id}
{
  "command": "on"
}
```

### Subscribe to Motion Sensor
```
POST /motionSensors/subscription
{
  "deviceId": "{device_id}",
  "callbackUrl": "https://your-n8n-instance.com/webhook/motion"
}
```

## Webhook Payload Format

When device states change, n8n will receive webhooks with this structure:

```json
{
  "deviceId": "device-uuid",
  "deviceName": "Living Room Light",
  "deviceType": "switches",
  "attribute": "switch",
  "value": "on",
  "unit": null,
  "timestamp": 1634567890123,
  "source": "smartthings",
  "hubId": "hub-uuid"
}
```

## Security

- OAuth 2.0 authentication required
- All API calls must include valid access token
- Webhook URLs are validated and stored securely
- Device access is limited to user-selected devices only