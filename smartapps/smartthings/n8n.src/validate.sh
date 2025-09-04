#!/bin/bash
# SmartThings n8n Integration Validation Script

echo "=== SmartThings n8n Integration Validation ==="
echo ""

# Check if the SmartApp file exists and has required components
SMARTAPP_FILE="smartapps/smartthings/n8n.src/n8n.groovy"

if [ ! -f "$SMARTAPP_FILE" ]; then
    echo "❌ SmartApp file not found: $SMARTAPP_FILE"
    exit 1
fi

echo "✅ SmartApp file exists: $SMARTAPP_FILE"

# Check for required SmartApp components
echo ""
echo "Checking required SmartApp components..."

# Check definition block
if grep -q "^definition(" "$SMARTAPP_FILE"; then
    echo "✅ definition() block found"
else
    echo "❌ definition() block missing"
fi

# Check preferences block
if grep -q "^preferences {" "$SMARTAPP_FILE"; then
    echo "✅ preferences block found"
else
    echo "❌ preferences block missing"
fi

# Check mappings block
if grep -q "^mappings {" "$SMARTAPP_FILE"; then
    echo "✅ mappings block found"
else
    echo "❌ mappings block missing"
fi

# Check OAuth configuration
if grep -q "oauth:" "$SMARTAPP_FILE"; then
    echo "✅ OAuth configuration found"
else
    echo "❌ OAuth configuration missing"
fi

# Check for required API endpoints
echo ""
echo "Checking API endpoints..."

endpoints=(
    '/:deviceType'
    '/:deviceType/states'
    '/:deviceType/subscription'
    '/:deviceType/:id'
    '/subscriptions'
    '/devices'
    '/webhook/:deviceType/:id'
)

for endpoint in "${endpoints[@]}"; do
    if grep -q "path(\"$endpoint\")" "$SMARTAPP_FILE"; then
        echo "✅ Endpoint found: $endpoint"
    else
        echo "❌ Endpoint missing: $endpoint"
    fi
done

# Check for required functions
echo ""
echo "Checking required functions..."

functions=(
    "def list()"
    "def listStates()"
    "def listAllDevices()"
    "def update()"
    "def show()"
    "def addSubscription()"
    "def removeSubscription()"
    "def deviceHandler(evt)"
    "def validateCommand"
)

for func in "${functions[@]}"; do
    if grep -q "$func" "$SMARTAPP_FILE"; then
        echo "✅ Function found: $func"
    else
        echo "❌ Function missing: $func"
    fi
done

# Check supported device types
echo ""
echo "Checking supported device types..."

device_types=(
    "switches"
    "motionSensors"
    "contactSensors"
    "presenceSensors"
    "temperatureSensors"
    "lightSensors"
    "humiditySensors"
    "waterSensors"
    "locks"
    "alarms"
)

for device_type in "${device_types[@]}"; do
    if grep -q "input \"$device_type\"" "$SMARTAPP_FILE"; then
        echo "✅ Device type supported: $device_type"
    else
        echo "❌ Device type missing: $device_type"
    fi
done

# Check webhook functionality
echo ""
echo "Checking webhook functionality..."

if grep -q "httpPostJson" "$SMARTAPP_FILE"; then
    echo "✅ Webhook HTTP POST functionality found"
else
    echo "❌ Webhook HTTP POST functionality missing"
fi

if grep -q "callbackUrl" "$SMARTAPP_FILE"; then
    echo "✅ Callback URL handling found"
else
    echo "❌ Callback URL handling missing"
fi

# File size check (should be substantial)
file_size=$(wc -l < "$SMARTAPP_FILE")
if [ "$file_size" -gt 300 ]; then
    echo "✅ SmartApp file size adequate: $file_size lines"
else
    echo "❌ SmartApp file size too small: $file_size lines"
fi

echo ""
echo "=== Validation Complete ==="

# Check if documentation exists
if [ -f "smartapps/smartthings/n8n.src/README.md" ]; then
    echo "✅ Documentation found: README.md"
else
    echo "❌ Documentation missing: README.md"
fi

# Check if examples exist
if [ -f "smartapps/smartthings/n8n.src/n8n-examples.js" ]; then
    echo "✅ Examples found: n8n-examples.js"
else
    echo "❌ Examples missing: n8n-examples.js"
fi

echo ""
echo "🎉 SmartThings n8n Integration validation complete!"
echo ""
echo "Next steps:"
echo "1. Install the SmartApp in your SmartThings account"
echo "2. Configure OAuth credentials in n8n"
echo "3. Select devices to integrate"
echo "4. Create n8n workflows using the provided examples"