#include <NimBLEDevice.h>

// See the following for generating UUIDs: https://www.uuidgenerator.net/
#define SERVICE_UUID "000ffdf4-68d9-4e48-a89a-219e581f0d64"
#define CHARACTERISTIC_UUID "44a80b83-c605-4406-8e50-fc42f03b6d38"

NimBLEServer *pServer = NULL;
NimBLEService *pService = NULL;
NimBLECharacteristic *pCharacteristic = NULL;

bool deviceConnected = false;

class ServerCallbacks : public NimBLEServerCallbacks
{
    void onConnect(NimBLEServer *pServer, NimBLEConnInfo &connInfo)
    {
        deviceConnected = true;
        Serial.println("BLE Device Connected");
    };
    void onDisconnect(NimBLEServer *pServer, NimBLEConnInfo &connInfo, int reason)
    {
        deviceConnected = false;
        Serial.println("BLE Device Disconnect");
    }
};

class BLE_Callbacks : public NimBLECharacteristicCallbacks
{
    void onWrite(NimBLECharacteristic *pCharacteristic, NimBLEConnInfo &connInfo)
    {
        char value[64];
        size_t len = pCharacteristic->getValue().length();
        len = len < sizeof(value) - 1 ? len : sizeof(value) - 1;
        memcpy(value, pCharacteristic->getValue().data(), len);
        value[len] = '\0';
        if (len > 0)
        {
            Serial.print("Value: ");
            for (size_t i = 0; i < len; i++)
                Serial.print(value[i], HEX);
            Serial.println();
        }
    }
};

void setup()
{
    Serial.begin(115200);
    Serial.print("SERVICE UUID: ");
    Serial.println(SERVICE_UUID);
    Serial.print("CHARACTERISTIC UUID: ");
    Serial.println(CHARACTERISTIC_UUID);
    Serial.println("Starting BLE Server...");

    NimBLEDevice::init("ESP32_BLE");
    Serial.print("BLE MAC Address: ");
    Serial.println(NimBLEDevice::getAddress().toString().c_str());
    pServer = NimBLEDevice::createServer();
    pServer->setCallbacks(new ServerCallbacks());
    pService = pServer->createService(SERVICE_UUID);
    pCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::NOTIFY | NIMBLE_PROPERTY::INDICATE);
    pCharacteristic->setCallbacks(new BLE_Callbacks());
    pCharacteristic->createDescriptor("2901", NIMBLE_PROPERTY::READ)->setValue("BLE Control Service");
    pService->start();
    NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->enableScanResponse(true);
    pAdvertising->setPreferredParams(0x06, 0x12);
    pServer->getAdvertising()->start();
    Serial.println("Characteristic Defined!");
}

void loop()
{
    int txValue = random(1, 20);
    char txString[8];
    dtostrf(txValue, 1, 2, txString);
    pCharacteristic->setValue(txString);
    pCharacteristic->notify();
    delay(2000);
}
