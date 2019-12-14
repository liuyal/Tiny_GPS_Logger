#include <EEPROM.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"

#define RXD2 16
#define TXD2 17

#define SERVICE_UUID        "000ffdf4-68d9-4e48-a89a-219e581f0d64"
#define CHARACTERISTIC_UUID "44a80b83-c605-4406-8e50-fc42f03b6d38"

BLEServer* pServer = NULL;
BLEService* pService = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;

const int CS = 5;
int log_flag_addr = 0;
bool logging_on = false;

String gnss_dir = "GNSS_LOGS";
int nfiles = 0;

const byte numChars = 50;
char receivedChars[numChars];
bool newData = false;

void setup() {
  Serial.begin(115200);
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);
  EEPROM.begin(64);
  Serial_Print("\nESP32_ON\n");
  BLE_INIT();
  SD_INIT();
  listDir(SD, "/", 0);
  createDir(SD, "/" + gnss_dir);
  nfiles = listDir(SD, "/" + gnss_dir, 0);
  if ( EEPROM.read(log_flag_addr) == 0x01) logging_on = true;
  else logging_on = false;
}

void Serial_Print(String msg) {
  Serial.print(msg);
}

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial_Print("BLE Device Connected!\n");
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial_Print("BLE Device Disconnect!\n");
    }
};

class BLE_Callbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) {
        Serial_Print("BLE Value: ");
        // DOTO: Trigger logging + ble send data
        for (int i = 0; i < value.length(); i++) Serial.print(value[i], HEX);
        Serial_Print("\n");
      }
    }
};

void BLE_INIT() {
  Serial_Print("\nInitializing BLE...\n");
  Serial_Print("SERVICE UUID: " + String(SERVICE_UUID) + "\n");
  Serial_Print("CHARACTERISTIC UUID: " + String(CHARACTERISTIC_UUID) + "\n");
  Serial_Print("Starting BLE Server...\n");
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
  pCharacteristic->setCallbacks(new BLE_Callbacks());
  pService->start();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  pServer->getAdvertising()->start();
  Serial_Print("Characteristic Defined!\n");
}

void SD_INIT() {
  Serial_Print("\nInitializing SD Card...\n");
  if (!SD.begin(CS)) {
    Serial_Print("Initialization Failed!\n");
    delay(1000);
    ESP.restart();
    return;
  }
  Serial_Print("Initialization Success!\n");
  Serial_Print("-------SD Card Info-------\n");
  Serial_Print("SD Card Type:\t");
  uint8_t cardType = SD.cardType();
  if (cardType == CARD_MMC) Serial_Print("MMC\n");
  else if (cardType == CARD_SD) Serial_Print("SDSC\n");
  else if (cardType == CARD_SDHC) Serial_Print("SDHC\n");
  else if (cardType == CARD_NONE) Serial_Print("No SD Card Attached\n");
  else Serial_Print("UNKNOWN\n");
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();
  Serial_Print("Volume(MB):\t" + String((float)bytes / (1000 * 1000)) + "\n");
  Serial_Print("Volume(GB):\t" + String((float)bytes / (1000 * 1000 * 1000)) + "\n");
  Serial_Print("Used(MB):\t" + String((float)used_bytes / (1000 * 1000)) + "\n");
  Serial_Print("Used(GB):\t" + String((float)used_bytes / (1000 * 1000 * 1000)) + "\n");
  Serial_Print("--------------------------\n");
}

int listDir(fs::FS &fs, String dirname, uint8_t levels) {
  int count = 0;
  Serial_Print("Listing directory: " + dirname + "\n");
  File root = fs.open(dirname);
  if (!root || !root.isDirectory()) {
    Serial_Print("Failed to open directory\n");
    return -1;
  }
  File file = root.openNextFile();
  while (file) {
    if (file.isDirectory()) {
      Serial_Print("  DIR : " + String(file.name()) + "\n");
      if (levels) listDir(fs, file.name(), levels - 1);
    }
    else {
      count += 1;
      Serial_Print("  FILE: " + String(file.name()) + "  SIZE: " + String(file.size()) + "\n");
    }
    file = root.openNextFile();
  }
  return count;
}

void createDir(fs::FS &fs, String path) {
  if (SD.exists(path)) {
    Serial_Print("Dir " + path + " Exists...\n");
    return;
  }
  Serial_Print("Creating Dir: " + path + "\n");
  if (fs.mkdir(path)) Serial_Print("Dir created\n");
  else Serial_Print("mkdir failed\n");
}

void removeDir(fs::FS &fs, String path) {
  Serial_Print("Removing Dir: " + path + "\n");
  File root = fs.open(path);
  if (!root || !root.isDirectory()) {
    Serial_Print("Failed to open directory\n");
    return;
  }
  File file = root.openNextFile();
  while (file) {
    if (!file.isDirectory()) deleteFile(SD, file.name());
    file = root.openNextFile();
  }
  if (fs.rmdir(path))Serial_Print("Dir removed\n");
  else Serial_Print("rmdir failed\n");
}

void readFile(fs::FS &fs, String path) {
  Serial_Print("Reading file: " + path + "\n");
  File file = fs.open(path);
  if (!file) {
    Serial_Print("Failed to open file for reading\n");
    return;
  }
  Serial_Print("Read from file: ");
  while (file.available()) Serial.write(file.read());
  file.close();
}

void appendFile(fs::FS &fs, String path, String message) {
  File file = fs.open(path, FILE_APPEND);
  if (!file) return;
  file.print(message);
  file.close();
}

void deleteFile(fs::FS &fs, String path) {
  Serial_Print("Deleting file: " + path + "\n");
  if (fs.remove(path))Serial_Print("File deleted\n");
  else Serial_Print("Delete failed\n");
}

void recvWithStartEndMarkers() {
  static boolean recvInProgress = false;
  static byte ndx = 0;
  char startMarker = '<';
  char endMarker = '>';
  char rc;
  while (Serial.available() > 0 && newData == false) {
    rc = Serial.read();
    if (recvInProgress == true) {
      if (rc != endMarker) {
        receivedChars[ndx] = rc;
        ndx++;
        if (ndx >= numChars)  ndx = numChars - 1;
      }
      else {
        receivedChars[ndx] = '\0';
        recvInProgress = false;
        ndx = 0;
        newData = true;
      }
    }
    else if (rc == startMarker) recvInProgress = true;
  }
}

void CMD_EVENT() {
  recvWithStartEndMarkers();
  if (newData == true) {
    Serial_Print(String(receivedChars) + "\n");
    if (String(receivedChars).indexOf("log_on") >= 0) {
      Serial_Print("[start_logging]\n");
      logging_on = true;
      EEPROM.write(log_flag_addr, 0x01);
      EEPROM.commit();
    }
    else if (String(receivedChars).indexOf("log_off") >= 0) {
      Serial_Print("[end_logging]\n");
      logging_on = false;
      EEPROM.write(log_flag_addr, 0x00);
      EEPROM.commit();
    }
    else if (String(receivedChars).indexOf("list") >= 0) {
      int files = listDir(SD, "/" + gnss_dir, 0);
      Serial_Print("Number of Files: " + String(files) + "\n");
    }
    else if (String(receivedChars).indexOf("reboot") >= 0) {
      Serial_Print("[rebooting]\n");
      delay(3000);
      ESP.restart();
    }
    else if (String(receivedChars).indexOf("reset") >= 0) {
      Serial_Print("[system_reset]\n");
      removeDir(SD, "/" + gnss_dir);
      createDir(SD, "/" + gnss_dir);
      nfiles = listDir(SD, "/" + gnss_dir, 0);
      logging_on = false;
      EEPROM.write(log_flag_addr, 0x00);
      EEPROM.commit();
    }
    newData = false;
  }
}

void loop() {

  String gnss_data;

  if (logging_on) {
    while (Serial2.available()) gnss_data = String(gnss_data + String((char)Serial2.read()));
    Serial_Print(gnss_data);
    appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", gnss_data);
  }

  // DOTO: replace with GNSS DATA OR Callback (send for every request from phone)
  //  char txString[8];
  //  pCharacteristic->setValue(txString);
  //  pCharacteristic->notify();
  //  delay(1000);

  // DOTO: Replace with BLE callback
  CMD_EVENT();
}
