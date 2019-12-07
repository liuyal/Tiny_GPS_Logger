#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

#define SERVICE_UUID        "50246d76-cbaa-4551-9c7a-9915e2096e08"
#define CHARACTERISTIC_UUID "35766c2d-a829-4549-b6f1-be3b8586a4d9"


void setup() {
  Serial.begin(115200);
  Serial.println("Starting BLE Server!");

  BLEDevice::init("BLE_TEST");
  BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);

  BLECharacteristic *pCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
  pCharacteristic->setValue("TTTTT");

  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pService->start();

    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);

  BLEDevice::startAdvertising();
  Serial.println("Characteristic Defined!");
}

void loop() {
  delay(2000);
}
