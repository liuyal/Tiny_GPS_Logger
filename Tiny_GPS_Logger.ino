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
bool logging_on = false;

int gps_flag_addr = 0;
int log_flag_addr = 1;
int print_flag_addr = 2;

const int CS = 5;
int nfiles = 0;
String gnss_dir = "GNSS_LOGS";
String gnss_data;
int log_counter = 0;
String log_buffer = "";

TinyGPSPlus gps;

void Serial_Print(String msg) {
  Serial.print(msg);
}

void setup() {
  Serial.begin(115200);
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);
  EEPROM.begin(64);
  Serial_Print("\n*****ESP32_ON*****\n");
  BLE_INIT();
  SD_INIT();
  listDir(SD, "/", 0);
  createDir(SD, "/" + gnss_dir);
  listDir(SD, "/" + gnss_dir, 0);
  if (EEPROM.read(gps_flag_addr) == 0x01) gps_on = true;
  if (EEPROM.read(log_flag_addr) == 0x01) logging_on = true;
  if (EEPROM.read(print_flag_addr) == 0x01) gps_print = true;
  Serial_Print("\n");
}

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      device_connected = true;
      Serial_Print("BLE Device Connected!\n");
    };
    void onDisconnect(BLEServer* pServer) {
      device_connected = false;
      Serial_Print("BLE Device Disconnect!\n");
    }
};

class BLE_Callbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      std::string value = pCharacteristic->getValue();
      if (value.length() > 0) {
        Serial_Print("BLE Code: ");
        for (int i = 0; i < value.length(); i++) Serial.print(value[i], HEX);
        Serial_Print("\n");

        if (value[0] == 0x01) {
          Serial_Print("Status: " + String(device_connected ? 0x01 : 0x00) + String(has_fix ? 0x01 : 0x00) + String(gps_on ? 0x01 : 0x00) + String(gps_print ? 0x01 : 0x00) + String(logging_on ? 0x01 : 0x00) + "\n");
          byte buf[5];
          buf[0] = device_connected ? 0x01 : 0x00;
          buf[1] = has_fix ? 0x01 : 0x00;
          buf[2] = gps_on ? 0x01 : 0x00;
          buf[3] = gps_print ? 0x01 : 0x00;
          buf[4] = logging_on ? 0x01 : 0x00;
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
        }
        else if (value[0] == 0x02) {
          Serial_Print("[start_gps]\n");
          gps_on = true;
          EEPROM.write(gps_flag_addr, 0x01);
          EEPROM.commit();
        }
        else if (value[0] == 0x03) {
          Serial_Print("[end_gps]\n");
          gps_on = false;
          EEPROM.write(gps_flag_addr, 0x00);
          EEPROM.commit();
        }
        else if (value[0] == 0x04) {
          Serial_Print("[start_logging]\n");
          logging_on = true;
          EEPROM.write(log_flag_addr, 0x01);
          EEPROM.commit();
        }
        else if (value[0] == 0x05) {
          Serial_Print("[end_logging]\n");
          logging_on = false;
          EEPROM.write(log_flag_addr, 0x00);
          EEPROM.commit();
        }
        else if (value[0] == 0x06) {
          String gps_valid = String(gps.location.isValid()) + "," + String(gps.location.isUpdated()) + "," + String(gps.location.age());
          String gps_data_a = String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7) + "," + String(gps.date.value());
          String gps_data_b = String(gps.time.hour()) + "," + String(gps.time.minute()) + "," + String(gps.time.second());
          String gps_data_c = String(gps.satellites.value()) + "," + String(gps.speed.kmph()) + "," + String(gps.course.deg()) + "," + String(gps.altitude.meters()) + "," +  String(gps.hdop.value());
          String packet = gps_valid + "," + gps_data_a  + "," + gps_data_b + "," + gps_data_c;
          byte buf[packet.length() + 1];
          packet.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(packet + "\n");
        }
        else if (value[0] == 0x07) {
          String listing = listDir(SD, "/" + gnss_dir, 0);
          byte buf[listing.length() + 1];
          listing.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(listing);
        }
        else if (value[0] == 0x08) {
          String text = readFile(SD, "/" + gnss_dir + "/GPS_" + int(value[1]) + ".log");
          // Not Possible so far
          // byte buf[text.length() + 1];
          // text.getBytes(buf, sizeof(buf));
          // pCharacteristic->setValue(buf, 20);
          // pCharacteristic->indicate();
          // Serial_Print(text);
        }
        else if (value[0] == 0x09) {
          Serial_Print("[rebooting]\n");
          delay(2000);
          ESP.restart();
        }
        else if (value[0] == 0x0a) {
          Serial_Print("[system_reset]\n");
          removeDir(SD, "/" + gnss_dir);
          createDir(SD, "/" + gnss_dir);
          listDir(SD, "/" + gnss_dir, 0);
          gps_on = false;
          logging_on = false;
          gps_print = false;
          EEPROM.write(gps_flag_addr, 0x00);
          EEPROM.write(log_flag_addr, 0x00);
          EEPROM.write(print_flag_addr, 0x00);
          EEPROM.commit();
          Serial_Print("[reset_complete]\n");
        }
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

String listDir(fs::FS &fs, String dirname, uint8_t levels) {
  String listing = "";
  int count = 0;
  Serial_Print("Listing directory: " + dirname + "\n");
  File root = fs.open(dirname);
  if (!root || !root.isDirectory()) {
    Serial_Print("Failed to open directory\n");
    return "-1,";
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
      listing = listing + "," +  String(file.name()) + "," + String(file.size());
    }
    file = root.openNextFile();
  }
  nfiles = count;
  return String(count) + listing;
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

void appendFile(fs::FS &fs, String path, String message) {
  File file = fs.open(path, FILE_APPEND);
  if (!file) return;
  file.print(message);
  file.close();
}

String readFile(fs::FS &fs, String path) {
  String text = "";
  Serial_Print("Reading file: " + path + "\n");
  File file = fs.open(path);
  if (!file) {
    return "Failed to open file for reading\n";
  }
  while (file.available()) {
    text = text + (char)file.read();
  }
  file.close();
  return text;
}

void deleteFile(fs::FS &fs, String path) {
  Serial_Print("Deleting file: " + path + "\n");
  if (fs.remove(path))Serial_Print("File deleted\n");
  else Serial_Print("Delete failed\n");
}

void CMD_EVENT() {
  String receivedChars;
  while (Serial.available() > 0 ) {
    receivedChars = Serial.readString();
    // Serial.println(receivedChars);
  }
  if (receivedChars.indexOf("status") >= 0) {
    Serial_Print("Status: " + String(device_connected ? 0x01 : 0x00) + String(has_fix ? 0x01 : 0x00) + String(gps_on ? 0x01 : 0x00) + String(gps_print ? 0x01 : 0x00) + String(logging_on ? 0x01 : 0x00) + "\n");
  }
  else if (receivedChars.indexOf("gps") >= 0) {
    if (!gps_on) {
      Serial_Print("[start_gps]\n");
      gps_on = true;
      EEPROM.write(gps_flag_addr, 0x01);
      EEPROM.commit();
    }
    else {
      Serial_Print("[end_gps]\n");
      gps_on = false;
      EEPROM.write(gps_flag_addr, 0x00);
      EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("log") >= 0) {
    if (!logging_on) {
      Serial_Print("[start_logging]\n");
      logging_on = true;
      EEPROM.write(log_flag_addr, 0x01);
      EEPROM.commit();
    }
    else {
      Serial_Print("[end_logging]\n");
      logging_on = false;
      EEPROM.write(log_flag_addr, 0x00);
      EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("print") >= 0) {
    if (!gps_print) {
      Serial_Print("[start_printing]\n");
      gps_print = true;
      EEPROM.write(print_flag_addr, 0x01);
      EEPROM.commit();
    }
    else {
      Serial_Print("[end_printing]\n");
      gps_print = false;
      EEPROM.write(print_flag_addr, 0x00);
      EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("data") >= 0) {
    String gps_valid = String(gps.location.isValid()) + "," + String(gps.location.isUpdated()) + "," + String(gps.location.age());
    String gps_data_a = String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7) + "," + String(gps.date.value());
    String gps_data_b = String(gps.time.hour()) + "," + String(gps.time.minute()) + "," + String(gps.time.second());
    String gps_data_c = String(gps.satellites.value()) + "," + String(gps.speed.kmph()) + "," + String(gps.course.deg()) + "," + String(gps.altitude.meters()) + "," +  String(gps.hdop.value());
    String packet = gps_valid + "," + gps_data_a  + "," + gps_data_b + "," + gps_data_c;
    Serial_Print(packet + "\n");
  }
  else if (receivedChars.indexOf("list") >= 0) {
    listDir(SD, "/" + gnss_dir, 0);
  }
  else if (receivedChars.indexOf("read|") >= 0) {
    String index = receivedChars.substring(receivedChars.indexOf("|") + 1, receivedChars.indexOf("]"));
    Serial_Print(readFile(SD, "/" + gnss_dir + "/GPS_" + String(index.toInt()) + ".log"));
  }
  else if (receivedChars.indexOf("reboot") >= 0) {
    Serial_Print("[rebooting]\n");
    delay(2000);
    ESP.restart();
  }
  else if (receivedChars.indexOf("reset") >= 0) {
    Serial_Print("[system_reset]\n");
    removeDir(SD, "/" + gnss_dir);
    createDir(SD, "/" + gnss_dir);
    listDir(SD, "/" + gnss_dir, 0);
    gps_on = false;
    logging_on = false;
    gps_print = false;
    EEPROM.write(gps_flag_addr, 0x00);
    EEPROM.write(log_flag_addr, 0x00);
    EEPROM.write(print_flag_addr, 0x00);
    EEPROM.commit();
    Serial_Print("[reset_complete]\n");
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
    if (gps_print) Serial_Print(gnss_data);
    log_buffer = log_buffer + gnss_data;
    if (logging_on && log_counter % 1000 == 0 && log_counter != 0) {
      appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", log_buffer);
      log_buffer = "";
      log_counter = 0;
    }
  }

  log_counter++;
  CMD_EVENT();
}
