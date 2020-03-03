#include <EEPROM.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"
#include "TinyGPS++.h"

#define RXD2 16
#define TXD2 17

#define SERVICE_UUID        "000ffdf4-68d9-4e48-a89a-219e581f0d64"
#define CHARACTERISTIC_UUID "44a80b83-c605-4406-8e50-fc42f03b6d38"

BLEServer* pServer = NULL;
BLEService* pService = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;

const int CS = 5;
bool gps_on = false;
bool gps_print = false;
bool logging_on = false;
int gps_flag_addr = 0;
int log_flag_addr = 1;
String gnss_dir = "GNSS_LOGS";
String gnss_data;
int nfiles = 0;

TinyGPSPlus gps;

void setup() {
  Serial.begin(115200);
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);
  EEPROM.begin(64);
  Serial_Print("\n*****ESP32_ON*****\n");
  BLE_INIT();
  SD_INIT();
  listDir(SD, "/", 0);
  createDir(SD, "/" + gnss_dir);
  nfiles = listDir(SD, "/" + gnss_dir, 0);
  if (EEPROM.read(gps_flag_addr) == 0x01) gps_on = true;
  if (EEPROM.read(log_flag_addr) == 0x01) logging_on = true;
  else logging_on = false;
  Serial_Print("\n");
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
        for (int i = 0; i < value.length(); i++) {
          Serial.print(value[i], HEX);
        }
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
    return;
  }
  Serial_Print("Initialization Success!\n");
  cd_info();
}

void cd_info() {
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
  Serial_Print("Read from file: \n");
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

void gps_valid() {
  Serial_Print(String(gps.location.isValid()) + "\n");
  Serial_Print(String(gps.location.isUpdated()) + "\n");
  Serial_Print(String(gps.location.age()) + "\n");
}

// TODO: Convert into BLE Callbacks
void CMD_EVENT() {

  String receivedChars;
  while (Serial.available() > 0 ) {
    receivedChars = Serial.readString();
    // Serial.println(receivedChars);
  }

  if (receivedChars.indexOf("gps_on") >= 0) {
    Serial_Print("\n[start_gps]\n");
    gps_on = true;
    EEPROM.write(gps_flag_addr, 0x01);
    EEPROM.commit();
  }
  if (receivedChars.indexOf("gps_off") >= 0) {
    Serial_Print("\n[stop_gps]\n");
    gps_on = false;
    EEPROM.write(gps_flag_addr, 0x00);
    EEPROM.commit();
  }
  else if (receivedChars.indexOf("log_on") >= 0) {
    Serial_Print("\n[start_logging]\n");
    logging_on = true;
    EEPROM.write(log_flag_addr, 0x01);
    EEPROM.commit();
  }
  else if (receivedChars.indexOf("log_off") >= 0) {
    Serial_Print("\n[end_logging]\n");
    logging_on = false;
    EEPROM.write(log_flag_addr, 0x00);
    EEPROM.commit();
  }
  else if (receivedChars.indexOf("list") >= 0) {
    int files = listDir(SD, "/" + gnss_dir, 0);
    Serial_Print("Number of Files: " + String(files) + "\n");
  }
  else if (receivedChars.indexOf("read|") >= 0) {
    String index = receivedChars.substring(receivedChars.indexOf("|") + 1, receivedChars.indexOf("]"));
    readFile(SD, "/" + gnss_dir + "/GPS_" + String(index.toInt()) + ".log");
  }
  else if (receivedChars.indexOf("reboot") >= 0) {
    Serial_Print("\n[rebooting]\n");
    delay(2000);
    ESP.restart();
  }
  else if (receivedChars.indexOf("reset") >= 0) {
    Serial_Print("\n[system_reset]\n");
    removeDir(SD, "/" + gnss_dir);
    createDir(SD, "/" + gnss_dir);
    nfiles = listDir(SD, "/" + gnss_dir, 0);
    logging_on = false;
    EEPROM.write(log_flag_addr, 0x00);
    EEPROM.commit();
  }
  else if (receivedChars.indexOf("gps_print") >= 0) {
    gps_print = !gps_print;
  }
  else if (receivedChars.indexOf("get_status") >= 0) {
     Serial_Print(gps_on ? "1" : "0");
     Serial_Print(gps_print ? "1" : "0");
     Serial_Print(logging_on ? "1" : "0");
  }
  else if (receivedChars.indexOf("get_data") >= 0) {
    gps_valid();
    String gps_latitude = String(gps.location.lat(), 7);
    String gps_longitude = String(gps.location.lng(), 7);
    String gps_date = String(gps.date.value());
    String gps_hours = String(gps.time.hour());
    String gps_minutes = String(gps.time.minute());
    String gps_seconds = String(gps.time.second());
    String gps_speed = String(gps.speed.kmph());
    String gps_course =  String(gps.course.deg());
    String gps_alt = String(gps.altitude.meters());
    String gps_nsat = String(gps.satellites.value());
    String gps_hdop = String(gps.hdop.value());
    if (gps.time.hour() < 10) gps_hours = "0" + gps_hours;
    if (gps.time.minute() < 10) gps_minutes = "0" + gps_minutes;
    if (gps.time.second() < 10) gps_seconds = "0" + gps_seconds;
    Serial_Print(gps_latitude + "\n");
    Serial_Print(gps_longitude + "\n");
    Serial_Print(gps_date + "\n");
    Serial_Print(gps_hours + ":" + gps_minutes + ":" + gps_seconds + "\n");
    Serial_Print(gps_speed + "\n");
    Serial_Print(gps_course + "\n");
    Serial_Print(gps_alt + "\n");
    Serial_Print(gps_nsat + "\n");
    Serial_Print(gps_hdop + "\n");
  }
  else if (receivedChars.indexOf("get_pos") >= 0) {
    gps_valid();
    String gps_latitude = String(gps.location.lat(), 7);
    String gps_longitude = String(gps.location.lng(), 7);
    Serial_Print(gps_latitude + "\n");
    Serial_Print(gps_longitude + "\n");
  }
  else if (receivedChars.indexOf("get_datetime") >= 0) {
    gps_valid();
    String gps_date = String(gps.date.value());
    String gps_hours = String(gps.time.hour());
    String gps_minutes = String(gps.time.minute());
    String gps_seconds = String(gps.time.second());
    if (gps.time.hour() < 10) gps_hours = "0" + gps_hours;
    if (gps.time.minute() < 10) gps_minutes = "0" + gps_minutes;
    if (gps.time.second() < 10) gps_seconds = "0" + gps_seconds;
    Serial_Print(gps_date + "\n");
    Serial_Print(gps_hours + ":" + gps_minutes + ":" + gps_seconds + "\n");
  }
  else if (receivedChars.indexOf("test") >= 0) {
    uint32_t value = 556;
    pCharacteristic->setValue((uint8_t*)&value, 4);
    pCharacteristic->notify();
  }
}

void loop() {

  gnss_data = "";

  if (gps_on) {
    while (Serial2.available()) {
      int raw_data = Serial2.read();
      gps.encode(raw_data);
      gnss_data = String(gnss_data + String((char)raw_data));
    }
    if (gps_print) {
      Serial_Print(gnss_data);
    }
  }

  if (gps_on && logging_on) {
    appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", gnss_data);
  }

  CMD_EVENT();
}
