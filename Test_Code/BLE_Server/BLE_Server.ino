#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/
#define SERVICE_UUID        "000ffdf4-68d9-4e48-a89a-219e581f0d64"
#define CHARACTERISTIC_UUID "44a80b83-c605-4406-8e50-fc42f03b6d38"

BLEServer* pServer = NULL;
BLEService* pService = NULL;
BLECharacteristic* pCharacteristic = NULL;

bool deviceConnected = false;

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("BLE Device Connected");
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("BLE Device Disconnect");
    }
};

class BLE_Callbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) {
        Serial.print("Value: ");
        for (int i = 0; i < value.length(); i++) Serial.print(value[i], HEX);
        Serial.println();
      }
    }
};

void setup() {
  Serial.begin(115200);
  Serial.println("SERVICE UUID: " + String(SERVICE_UUID));
  Serial.println("CHARACTERISTIC UUID: " + String(CHARACTERISTIC_UUID));
  Serial.println("Starting BLE Server...");
  BLEDevice::init("ESP32_BLE");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());
  pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );
  pCharacteristic->addDescriptor(new BLE2902());
  pCharacteristic->setCallbacks(new BLE_Callbacks());
  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  pServer->getAdvertising()->start();
  Serial.println("Characteristic Defined!");
}

void loop() {

  int txValue = random(1, 20);
  char txString[8];
  dtostrf(txValue, 1, 2, txString);
  pCharacteristic->setValue(txString);
  pCharacteristic->notify();

  delay(2000);
}
