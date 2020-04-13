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

bool device_connected = false;
bool has_fix = false;
bool gps_on = false;
bool gps_print = false;
bool ble_print = false;
bool logging_on = false;

int gps_flag_addr = 0;
int log_flag_addr = 1;
int print_flag_addr = 2;
int ble_print_flag_addr = 3;

const int CS = 5;
int nfiles = 0;
String gnss_dir = "GNSS_LOGS";
String log_buffer = "";
int log_counter = 0;

TinyGPSPlus gps;

void Serial_Print(String msg) {
  Serial.print(msg);
}

void setup() {
  Serial.begin(115200);
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);
  EEPROM.begin(64);
  Serial_Print("\n\r*****ESP32_ON*****\n\r");
  BLE_INIT();
  SD_INIT();
  listDir(SD, "/", 0);
  createDir(SD, "/" + gnss_dir);
  listDir(SD, "/" + gnss_dir, 0);
  if (EEPROM.read(gps_flag_addr) == 0x01) gps_on = true;
  if (EEPROM.read(log_flag_addr) == 0x01) logging_on = true;
  if (EEPROM.read(print_flag_addr) == 0x01) gps_print = true;
  if (EEPROM.read(ble_print_flag_addr) == 0x01) ble_print = true;
  Serial_Print("\n\r");
}

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      device_connected = true;
      Serial_Print("BLE Device Connected!\n\r");
    };
    void onDisconnect(BLEServer* pServer) {
      device_connected = false;
      Serial_Print("BLE Device Disconnect!\n\r");
    }
};

class BLE_Callbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) {
        Serial_Print("BLE Code: ");
        for (int i = 0; i < value.length(); i++) Serial.print(value[i], HEX);
        Serial_Print("\n\r");

        if (value[0] == 0x01) {
          byte buf[6];
          buf[0] = device_connected ? 0x01 : 0x00;
          buf[1] = has_fix ? 0x01 : 0x00;
          buf[2] = gps_on ? 0x01 : 0x00;
          buf[3] = gps_print ? 0x01 : 0x00;
          buf[4] = ble_print ? 0x01 : 0x00;
          buf[5] = logging_on ? 0x01 : 0x00;
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print("Status: " + String(buf[0]) + String(buf[1]) + String(buf[2]) + String(buf[3]) + String(buf[4]) + String(buf[5]) + "\n\r");
        }
        else if (value[0] == 0x02) {
          Serial_Print("[start_gps]\n\r"); gps_on = true;
          EEPROM.write(gps_flag_addr, 0x01); EEPROM.commit();
        }
        else if (value[0] == 0x03) {
          Serial_Print("[end_gps]\n\r"); gps_on = false;
          EEPROM.write(gps_flag_addr, 0x00); EEPROM.commit();
        }
        else if (value[0] == 0x04) {
          Serial_Print("[start_logging]\n\r"); logging_on = true;
          EEPROM.write(log_flag_addr, 0x01); EEPROM.commit();
        }
        else if (value[0] == 0x05) {
          Serial_Print("[end_logging]\n\r"); logging_on = false;
          EEPROM.write(log_flag_addr, 0x00); EEPROM.commit();
        }
        else if (value[0] == 0x06) {
          Serial_Print("[start_ble_printing]\n\r"); ble_print = true;
          EEPROM.write(ble_print_flag_addr, 0x01); EEPROM.commit();
        }
        else if (value[0] == 0x07) {
          Serial_Print("[end_ble_printing]\n\r"); ble_print = false;
          EEPROM.write(ble_print_flag_addr, 0x00); EEPROM.commit();
        }
        else if (value[0] == 0x08) {
          String gps_valid = String(gps.location.isValid()) + "," + String(gps.location.isUpdated()) + "," + String(gps.location.age());
          String gps_data_a = String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7) + "," + String(gps.date.value());
          String gps_data_b = String(gps.time.hour()) + "," + String(gps.time.minute()) + "," + String(gps.time.second());
          String gps_data_c = String(gps.satellites.value()) + "," + String(gps.speed.kmph()) + "," + String(gps.course.deg()) + "," + String(gps.altitude.meters()) + "," +  String(gps.hdop.value());
          String packet = gps_valid + "," + gps_data_a  + "," + gps_data_b + "," + gps_data_c;
          byte buf[packet.length() + 1];
          packet.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(packet + "\n\r");
        }
        else if (value[0] == 0x09) {
          String listing = listDir(SD, "/" + gnss_dir, 0);
          byte buf[listing.length() + 1];
          listing.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(listing);
        }
        else if (value[0] == 0x0a) {
          String text = readFile(SD, "/" + gnss_dir + "/GPS_" + int(value[1]) + ".log");
          // Not Possible so far
          // byte buf[text.length() + 1];
          // text.getBytes(buf, sizeof(buf));
          // pCharacteristic->setValue(buf, 20);
          // pCharacteristic->indicate();
          // Serial_Print(text);
        }
        else if (value[0] == 0x0b) {
          uint64_t bytes = SD.totalBytes();
          uint64_t used_bytes = SD.usedBytes();
          uint32_t bytes_low = bytes % 0xFFFFFFFF;
          uint32_t bytes_high = (bytes >> 32) % 0xFFFFFFFF;
          uint32_t used_bytes_low = used_bytes % 0xFFFFFFFF;
          uint32_t used_bytes_high = (used_bytes >> 32) % 0xFFFFFFFF;
          String sdcard = String(bytes_high) + String(bytes_low) + "," + String(used_bytes_high) + String(used_bytes_low);
          byte buf[sdcard.length() + 1];
          sdcard.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(sdcard + "\n\r");
        }
        else if (value[0] == 0x0c) {
          Serial_Print("[rebooting]\n\r");
          delay(2000);
          ESP.restart();
        }
        else if (value[0] == 0x0d) {
          Serial_Print("[system_reset]\n\r");
          removeDir(SD, "/" + gnss_dir);
          createDir(SD, "/" + gnss_dir);
          listDir(SD, "/" + gnss_dir, 0);
          gps_on = false;
          logging_on = false;
          gps_print = false;
          ble_print = false;
          EEPROM.write(gps_flag_addr, 0x00);
          EEPROM.write(log_flag_addr, 0x00);
          EEPROM.write(print_flag_addr, 0x00);
          EEPROM.write(ble_print_flag_addr, 0x00);
          EEPROM.commit();
          Serial_Print("[reset_complete]\n\r");
        }
      }
    }
};

void BLE_INIT() {
  Serial_Print("\n\rInitializing BLE...\n\r");
  Serial_Print("SERVICE UUID: " + String(SERVICE_UUID) + "\n\r");
  Serial_Print("CHARACTERISTIC UUID: " + String(CHARACTERISTIC_UUID) + "\n\r");
  Serial_Print("Starting BLE Server...\n\r");
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
  Serial_Print("Characteristic Defined!\n\r");
}

void SD_INIT() {
  Serial_Print("\n\rInitializing SD Card...\n\r");
  if (!SD.begin(CS)) {
    Serial_Print("Initialization Failed!\n\r");
    delay(1000);
    return;
  }
  Serial_Print("Initialization Success!\n\r");
  Serial_Print("-------SD Card Info-------\n\r");
  Serial_Print("SD Card Type:\t");
  uint8_t cardType = SD.cardType();
  if (cardType == CARD_MMC) Serial_Print("MMC\n\r");
  else if (cardType == CARD_SD) Serial_Print("SDSC\n\r");
  else if (cardType == CARD_SDHC) Serial_Print("SDHC\n\r");
  else if (cardType == CARD_NONE) Serial_Print("No SD Card Attached\n\r");
  else Serial_Print("UNKNOWN\n\r");
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();
  Serial_Print("Volume(MB):\t" + String((float)bytes / (1000 * 1000)) + "\n\r");
  Serial_Print("Volume(GB):\t" + String((float)bytes / (1000 * 1000 * 1000)) + "\n\r");
  Serial_Print("Used(MB):\t" + String((float)used_bytes / (1000 * 1000)) + "\n\r");
  Serial_Print("Used(GB):\t" + String((float)used_bytes / (1000 * 1000 * 1000)) + "\n\r");
  Serial_Print("--------------------------\n\r");
}

String listDir(fs::FS &fs, String dirname, uint8_t levels) {
  String listing = "";
  int count = 0;
  Serial_Print("Listing directory: " + dirname + "\n\r");
  File root = fs.open(dirname);
  if (!root || !root.isDirectory()) {
    Serial_Print("Failed to open directory\n\r");
    return "-1,";
  }
  File file = root.openNextFile();
  while (file) {
    if (file.isDirectory()) {
      Serial_Print("  DIR : " + String(file.name()) + "\n\r");
      if (levels) listDir(fs, file.name(), levels - 1);
    }
    else {
      count += 1;
      Serial_Print("  FILE: " + String(file.name()) + "  SIZE: " + String(file.size()) + "\n\r");
      listing = listing + "," +  String(file.name()) + "," + String(file.size());
    }
    file = root.openNextFile();
  }
  nfiles = count;
  return String(count) + listing;
}

void createDir(fs::FS &fs, String path) {
  if (SD.exists(path)) {
    Serial_Print("Dir " + path + " Exists...\n\r");
    return;
  }
  Serial_Print("Creating Dir: " + path + "\n\r");
  if (fs.mkdir(path)) Serial_Print("Dir created\n\r");
  else Serial_Print("mkdir failed\n\r");
}

void removeDir(fs::FS &fs, String path) {
  Serial_Print("Removing Dir: " + path + "\n\r");
  File root = fs.open(path);
  if (!root || !root.isDirectory()) {
    Serial_Print("Failed to open directory\n\r");
    return;
  }
  File file = root.openNextFile();
  while (file) {
    if (!file.isDirectory()) deleteFile(SD, file.name());
    file = root.openNextFile();
  }
  if (fs.rmdir(path))Serial_Print("Dir removed\n\r");
  else Serial_Print("rmdir failed\n\r");
}

void appendFile(fs::FS &fs, String path, String message) {
  File file = fs.open(path, FILE_APPEND);
  if (!file) return;
  file.print(message);
  file.close();
}

String readFile(fs::FS &fs, String path) {
  String text = "";
  Serial_Print("Reading file: " + path + "\n\r");
  File file = fs.open(path);
  if (!file) {
    return "Failed to open file for reading\n\r";
  }
  while (file.available()) {
    text = text + (char)file.read();
  }
  file.close();
  return text;
}

void deleteFile(fs::FS &fs, String path) {
  Serial_Print("Deleting file: " + path + "\n\r");
  if (fs.remove(path))Serial_Print("File deleted\n\r");
  else Serial_Print("Delete failed\n\r");
}

void CMD_EVENT() {
  String receivedChars;
  while (Serial.available() > 0 ) {
    receivedChars = Serial.readString();
    // Serial.println(receivedChars);
  }
  if (receivedChars.indexOf("status") >= 0) {
    byte buf[6];
    buf[0] = device_connected ? 0x01 : 0x00;
    buf[1] = has_fix ? 0x01 : 0x00;
    buf[2] = gps_on ? 0x01 : 0x00;
    buf[3] = gps_print ? 0x01 : 0x00;
    buf[4] = ble_print ? 0x01 : 0x00;
    buf[5] = logging_on ? 0x01 : 0x00;
    Serial_Print("Status: " + String(buf[0]) + String(buf[1]) + String(buf[2]) + String(buf[3]) + String(buf[4]) + String(buf[5]) + "\n\r");
  }
  else if (receivedChars.indexOf("gps") >= 0) {
    if (!gps_on) {
      Serial_Print("[start_gps]\n\r"); gps_on = true;
      EEPROM.write(gps_flag_addr, 0x01); EEPROM.commit();
    }
    else {
      Serial_Print("[end_gps]\n\r"); gps_on = false;
      EEPROM.write(gps_flag_addr, 0x00); EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("log") >= 0) {
    if (!logging_on) {
      Serial_Print("[start_logging]\n\r"); logging_on = true;
      EEPROM.write(log_flag_addr, 0x01); EEPROM.commit();
    }
    else {
      Serial_Print("[end_logging]\n\r"); logging_on = false;
      EEPROM.write(log_flag_addr, 0x00); EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("print") >= 0) {
    if (!gps_print) {
      Serial_Print("[start_printing]\n\r"); gps_print = true;
      EEPROM.write(print_flag_addr, 0x01); EEPROM.commit();
    }
    else {
      Serial_Print("[end_printing]\n\r"); gps_print = false;
      EEPROM.write(print_flag_addr, 0x00); EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("ble") >= 0) {
    if (!ble_print) {
      Serial_Print("[start_ble_printing]\n\r"); ble_print = true;
      EEPROM.write(ble_print_flag_addr, 0x01); EEPROM.commit();
    }
    else {
      Serial_Print("[end_ble_printing]\n\r"); ble_print = false;
      EEPROM.write(ble_print_flag_addr, 0x00); EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("data") >= 0) {
    String gps_valid = String(gps.location.isValid()) + "," + String(gps.location.isUpdated()) + "," + String(gps.location.age());
    String gps_data_a = String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7) + "," + String(gps.date.value());
    String gps_data_b = String(gps.time.hour()) + "," + String(gps.time.minute()) + "," + String(gps.time.second());
    String gps_data_c = String(gps.satellites.value()) + "," + String(gps.speed.kmph()) + "," + String(gps.course.deg()) + "," + String(gps.altitude.meters()) + "," +  String(gps.hdop.value());
    String packet = gps_valid + "," + gps_data_a  + "," + gps_data_b + "," + gps_data_c;
    Serial_Print(packet + "\n\r");
  }
  else if (receivedChars.indexOf("list") >= 0) {
    listDir(SD, "/" + gnss_dir, 0);
  }
  else if (receivedChars.indexOf("read|") >= 0) {
    String index = receivedChars.substring(receivedChars.indexOf("|") + 1, receivedChars.indexOf("]"));
    Serial_Print(readFile(SD, "/" + gnss_dir + "/GPS_" + String(index.toInt()) + ".log"));
  }
  else if (receivedChars.indexOf("sdcard") >= 0) {
    uint64_t bytes = SD.totalBytes();
    uint64_t used_bytes = SD.usedBytes();
    uint32_t bytes_low = bytes % 0xFFFFFFFF;
    uint32_t bytes_high = (bytes >> 32) % 0xFFFFFFFF;
    uint32_t used_bytes_low = used_bytes % 0xFFFFFFFF;
    uint32_t used_bytes_high = (used_bytes >> 32) % 0xFFFFFFFF;
    Serial_Print(String(bytes_high) + String(bytes_low) + "," + String(used_bytes_high) + String(used_bytes_low) + "\n\r");
  }
  else if (receivedChars.indexOf("reboot") >= 0) {
    Serial_Print("[rebooting]\n\r");
    delay(2000);
    ESP.restart();
  }
  else if (receivedChars.indexOf("reset") >= 0) {
    Serial_Print("[system_reset]\n\r");
    removeDir(SD, "/" + gnss_dir);
    createDir(SD, "/" + gnss_dir);
    listDir(SD, "/" + gnss_dir, 0);
    gps_on = false;
    logging_on = false;
    gps_print = false;
    ble_print = false;
    EEPROM.write(gps_flag_addr, 0x00);
    EEPROM.write(log_flag_addr, 0x00);
    EEPROM.write(print_flag_addr, 0x00);
    EEPROM.write(ble_print_flag_addr, 0x00);
    EEPROM.commit();
    Serial_Print("[reset_complete]\n\r");
  }
}

void loop() {

  String gnss_data = "";

  if (gps_on) {
    while (Serial2.available()) {
      int raw_data = Serial2.read();
      gps.encode(raw_data);
      gnss_data = String(gnss_data + String((char)raw_data));
    }

    if (gps.location.age() > 10000) has_fix = false;
    else has_fix = true;

    if (gps_print) Serial_Print(gnss_data);

    if (ble_print && gnss_data.length() > 20) {
      byte buf[gnss_data.length() + 1];
      gnss_data.getBytes(buf, sizeof(buf));
      pCharacteristic->setValue(buf, sizeof(buf));
      pCharacteristic->notify();
      delay(1000);
    }

    log_buffer = log_buffer + gnss_data;

    if (logging_on && log_counter % 1000 == 0 && log_counter != 0) {
      appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", log_buffer);
      log_buffer = "";
      log_counter = 0;
    }
  }
  else has_fix = false;

  log_counter++;
  CMD_EVENT();
}
