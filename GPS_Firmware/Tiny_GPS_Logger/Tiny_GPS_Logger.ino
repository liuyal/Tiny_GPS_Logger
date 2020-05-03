#include <EEPROM.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"
#include "TinyGPS++.h"

#define DEBUG true

#define RXD2 16
#define TXD2 17

// TODO: Get proper UUIDs
#define SERVICE_UUID        "000ffdf4-68d9-4e48-a89a-219e581f0d64"
#define CHARACTERISTIC_UUID "44a80b83-c605-4406-8e50-fc42f03b6d38"

BLEServer* pServer = NULL;
BLEService* pService = NULL;
BLECharacteristic* pCharacteristic = NULL;

const int NUMBER_OF_FLAGS = 4;
const int GPS_CONNECTION_FLAG_INDEX = 0;
const int GPS_ON_FLAG_INDEX = 1;
const int GPS_FIX_FLAG_INDEX = 2;
const int GPS_LOGGING_FLAG_INDEX = 3;

bool statusFlags[NUMBER_OF_FLAGS];

bool SERIAL_PRINT_FLAG = false;

const int CS = 5;
int log_counter = 0;
int nfiles = 0;
String gnss_dir = "GNSS_LOGS";
String log_buffer = "";

#if DEBUG
#include <WiFi.h>
#include <WiFiUdp.h>
const char* WIFI_SSID = "";
const char* WIFI_PWD = "";
const int WIFI_TIMEOUT = 10 * 1000;
const int UDP_PORT = 9996;
IPAddress HOST_IP(192, 168, 1, 80);
WiFiUDP Udp;
#endif

TinyGPSPlus gps;

void Serial_Print(String msg) {
#if DEBUG
  Serial.print(msg);
  if (WiFi.status() == WL_CONNECTED) {
    Udp.beginPacket(HOST_IP, UDP_PORT);
    Udp.printf((msg).c_str());
    Udp.endPacket();
  }
#endif
}

void setup() {
#if DEBUG
  Serial.begin(115200);
#endif
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);
  EEPROM.begin(64);
  Serial_Print("\n\r*************** GPS_ON ***************\n\r");
  BLE_INIT();
  SD_INIT();
  listDir(SD, "/", 0);
  createDir(SD, "/" + gnss_dir);
  listDir(SD, "/" + gnss_dir, 0);
  for (int i = 0; i < NUMBER_OF_FLAGS; i++) statusFlags[i] = false;
  if (EEPROM.read(GPS_ON_FLAG_INDEX) == 0x01) statusFlags[GPS_ON_FLAG_INDEX] = true;
  if (EEPROM.read(GPS_LOGGING_FLAG_INDEX) == 0x01) statusFlags[GPS_LOGGING_FLAG_INDEX] = true;
#if DEBUG
  WIFI_INIT();
#endif
  Serial_Print("\n\r\n\r[SETUP COMPLETE]\n\r\n\r");
}

/**********************************************************************
   BLE Functions
 **********************************************************************/

class ServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      statusFlags[GPS_CONNECTION_FLAG_INDEX] = true;
      Serial_Print("BLE Device Connected!\n\r");
    };
    void onDisconnect(BLEServer* pServer) {
      statusFlags[GPS_CONNECTION_FLAG_INDEX] = false;
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

        if (value[0] == 0x00) {
          String packet = "[ESP32_GPS]";
          byte buf[packet.length() + 1];
          packet.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(packet + "\n\r");
        }
        else if (value[0] == 0x01) {
          byte buf[NUMBER_OF_FLAGS];
          buf[GPS_CONNECTION_FLAG_INDEX] = statusFlags[GPS_CONNECTION_FLAG_INDEX] ? 0x01 : 0x00;
          buf[GPS_FIX_FLAG_INDEX] = statusFlags[GPS_FIX_FLAG_INDEX] ? 0x01 : 0x00;
          buf[GPS_ON_FLAG_INDEX] = statusFlags[GPS_ON_FLAG_INDEX] ? 0x01 : 0x00;
          buf[GPS_LOGGING_FLAG_INDEX] = statusFlags[GPS_LOGGING_FLAG_INDEX] ? 0x01 : 0x00;
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print("Status: " + String(buf[0]) + String(buf[1]) + String(buf[2]) + String(buf[3]) + "\n\r");
        }
        else if (value[0] == 0x02) {
          Serial_Print("[start_gps]\n\r"); statusFlags[GPS_ON_FLAG_INDEX] = true;
          EEPROM.write(GPS_ON_FLAG_INDEX, 0x01); EEPROM.commit();
          byte buf[2] = {0x01, 0x01};
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
        }
        else if (value[0] == 0x03) {
          Serial_Print("[end_gps]\n\r"); statusFlags[GPS_ON_FLAG_INDEX] = false;
          EEPROM.write(GPS_ON_FLAG_INDEX, 0x00); EEPROM.commit();
          byte buf[2] = {0x01, 0x01};
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
        }
        else if (value[0] == 0x04) {
          Serial_Print("[start_logging]\n\r"); statusFlags[GPS_LOGGING_FLAG_INDEX] = true;
          EEPROM.write(GPS_LOGGING_FLAG_INDEX, 0x01); EEPROM.commit();
          byte buf[2] = {0x01, 0x01};
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
        }
        else if (value[0] == 0x05) {
          Serial_Print("[end_logging]\n\r"); statusFlags[GPS_LOGGING_FLAG_INDEX] = false;
          EEPROM.write(GPS_LOGGING_FLAG_INDEX, 0x00); EEPROM.commit();
          byte buf[2] = {0x01, 0x01};
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
        }
        else if (value[0] == 0x06) {
          String gps_valid = String(gps.location.isValid()) + "," + String(gps.location.isUpdated()) + "," + String(gps.location.age());
          String gps_data_a = String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7);
          String gps_data_b = String(gps.date.year()) + "," + String(gps.date.month()) + "," + String(gps.date.day());
          String gps_data_c = String(gps.time.hour()) + "," + String(gps.time.minute()) + "," + String(gps.time.second());
          String gps_data_d = String(gps.satellites.value()) + "," + String(gps.speed.kmph()) + "," + String(gps.course.deg());
          String gps_data_e = String(gps.altitude.meters()) + "," +  String(gps.hdop.value());
          String packet = "[" + gps_valid + "," + gps_data_a  + "," + gps_data_b + "," + gps_data_c + "," + gps_data_d + "," + gps_data_e + "]";
          byte buf[packet.length() + 1];
          packet.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(packet + "\n\r");
        }
        else if (value[0] == 0x07) {
          String listing = "[" + listDir(SD, "/" + gnss_dir, 0) + "]";
          byte buf[listing.length() + 1];
          listing.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(listing);
        }
        else if (value[0] == 0x08) {
          String text = readFile(SD, "/" + gnss_dir + "/GPS_" + int(value[1]) + ".log");
          // BLOCKED
        }
        else if (value[0] == 0x09) {
          uint64_t bytes = SD.totalBytes();
          uint64_t used_bytes = SD.usedBytes();
          uint32_t bytes_low = bytes % 0xFFFFFFFF;
          uint32_t bytes_high = (bytes >> 32) % 0xFFFFFFFF;
          uint32_t used_bytes_low = used_bytes % 0xFFFFFFFF;
          uint32_t used_bytes_high = (used_bytes >> 32) % 0xFFFFFFFF;
          String sdcard = "[" + String(bytes_high) + String(bytes_low) + "," + String(used_bytes_high) + String(used_bytes_low) + "]";
          byte buf[sdcard.length() + 1];
          sdcard.getBytes(buf, sizeof(buf));
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print(sdcard + "\n\r");
        }
        else if (value[0] == 0x0a) {
          Serial_Print("[rebooting]\n\r");
          byte buf[2] = {0x01, 0x01};
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          delay(2000);
          ESP.restart();
        }
        else if (value[0] == 0x0b) {
          Serial_Print("[system_reset]\n\r");
          removeDir(SD, "/" + gnss_dir);
          createDir(SD, "/" + gnss_dir);
          listDir(SD, "/" + gnss_dir, 0);
          statusFlags[GPS_ON_FLAG_INDEX] = false;
          statusFlags[GPS_LOGGING_FLAG_INDEX] = false;
          EEPROM.write(GPS_ON_FLAG_INDEX, 0x00);
          EEPROM.write(GPS_LOGGING_FLAG_INDEX, 0x00);
          EEPROM.commit();
          byte buf[2] = {0x01, 0x01};
          pCharacteristic->setValue(buf, sizeof(buf));
          pCharacteristic->indicate();
          Serial_Print("[reset_complete]\n\r");
        }
      }
    }
};

void BLE_INIT() {
  Serial_Print("\n\rInitializing BLE...\n\r");
  Serial_Print("SERVICE UUID: \t\r" + String(SERVICE_UUID) + "\n\r");
  Serial_Print("CHARACTERISTIC UUID: \r" + String(CHARACTERISTIC_UUID) + "\n\r");
  Serial_Print("Starting BLE Server...\n\r");
  BLEDevice::init("ESP32_GPS_LOGGER");
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
  Serial_Print("GATT Service Defined!\n\r");
  Serial_Print("GATT Characteristic Defined!\n\r");
}


/**********************************************************************
   SDCard Functions
 **********************************************************************/

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
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();
  if (cardType == CARD_MMC) Serial_Print("MMC\n\r");
  else if (cardType == CARD_SD) Serial_Print("SDSC\n\r");
  else if (cardType == CARD_SDHC) Serial_Print("SDHC\n\r");
  else if (cardType == CARD_NONE) Serial_Print("No SD Card Attached\n\r");
  else Serial_Print("UNKNOWN\n\r");

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


/**********************************************************************
   WIFI Functions
 **********************************************************************/

#if DEBUG
void WIFI_INIT() {
  WiFi.begin(WIFI_SSID, WIFI_PWD);
  Serial_Print("\n\rConnecting to WiFi");
  unsigned long start_wait = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start_wait <= WIFI_TIMEOUT) {
    Serial_Print(".");
    delay(500);
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial_Print("\n\rConnected to: \t" + String(WIFI_SSID));
    Serial_Print("\n\rGateway IP: \t" + WiFi.gatewayIP().toString());
    Serial_Print("\n\rHost IP: \t" + HOST_IP.toString());
    Serial_Print("\n\rLocal IP: \t" + WiFi.localIP().toString());
    Serial_Print("\n\rUDP port: \t" + String(UDP_PORT));
    Udp.begin(UDP_PORT);
  } else Serial_Print("Unable to Connect to WiFi.");
}

String UDP_listen () {
  char incomingPacket[255];
  String packet;
  if (WiFi.status() == WL_CONNECTED) {
    int packetSize = Udp.parsePacket();
    if (packetSize) {
      int len = Udp.read(incomingPacket, 255);
      if (len > 0) incomingPacket[len] = 0;
      Serial_Print("Received from: " + String(Udp.remoteIP().toString()) + ":" + String(Udp.remotePort()) + "\n\r");
      Serial_Print("UDP Packet Contents: [" + String(packetSize) + " bytes] " + String(incomingPacket));
    } else return "";
  } else return "";
  return String(incomingPacket);
}
#endif


/**********************************************************************
   CMD Functions
 **********************************************************************/

void CMD_EVENT() {
  String receivedChars;
  while (Serial.available() > 0 ) receivedChars = Serial.readString();

#if DEBUG
  String input = UDP_listen();
  if (input != "") receivedChars = input;
#endif

  if (receivedChars.indexOf("status") >= 0) {
    byte buf[NUMBER_OF_FLAGS];
    buf[GPS_CONNECTION_FLAG_INDEX] = statusFlags[GPS_CONNECTION_FLAG_INDEX] ? 0x01 : 0x00;
    buf[GPS_FIX_FLAG_INDEX] = statusFlags[GPS_FIX_FLAG_INDEX] ? 0x01 : 0x00;
    buf[GPS_ON_FLAG_INDEX] = statusFlags[GPS_ON_FLAG_INDEX] ? 0x01 : 0x00;
    buf[GPS_LOGGING_FLAG_INDEX] = statusFlags[GPS_LOGGING_FLAG_INDEX] ? 0x01 : 0x00;
    Serial_Print("Status: " + String(buf[0]) + String(buf[1]) + String(buf[2]) + String(buf[3]) + "\n\r");
  }
  else if (receivedChars.indexOf("gps") >= 0) {
    if (!statusFlags[GPS_ON_FLAG_INDEX]) {
      Serial_Print("[start_gps]\n\r"); statusFlags[GPS_ON_FLAG_INDEX] = true;
      EEPROM.write(GPS_ON_FLAG_INDEX, 0x01); EEPROM.commit();
    } else {
      Serial_Print("[end_gps]\n\r"); statusFlags[GPS_ON_FLAG_INDEX] = false;
      EEPROM.write(GPS_ON_FLAG_INDEX, 0x00); EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("log") >= 0) {
    if (!statusFlags[GPS_LOGGING_FLAG_INDEX]) {
      Serial_Print("[start_logging]\n\r"); statusFlags[GPS_LOGGING_FLAG_INDEX] = true;
      EEPROM.write(GPS_LOGGING_FLAG_INDEX, 0x01); EEPROM.commit();
    }
    else {
      Serial_Print("[end_logging]\n\r"); statusFlags[GPS_LOGGING_FLAG_INDEX] = false;
      EEPROM.write(GPS_LOGGING_FLAG_INDEX, 0x00); EEPROM.commit();
    }
  }
  else if (receivedChars.indexOf("print") >= 0) {
    if (!SERIAL_PRINT_FLAG) {
      Serial_Print("[start_printing]\n\r"); 
      SERIAL_PRINT_FLAG = true;
    }
    else {
      Serial_Print("[end_printing]\n\r"); 
      SERIAL_PRINT_FLAG = false;
    }
  }
  else if (receivedChars.indexOf("data") >= 0) {
    String gps_valid = String(gps.location.isValid()) + "," + String(gps.location.isUpdated()) + "," + String(gps.location.age());
    String gps_data_a = String(gps.location.lat(), 7) + "," + String(gps.location.lng(), 7);
    String gps_data_b = String(gps.date.year()) + "," + String(gps.date.month()) + "," + String(gps.date.day());
    String gps_data_c = String(gps.time.hour()) + "," + String(gps.time.minute()) + "," + String(gps.time.second());
    String gps_data_d = String(gps.satellites.value()) + "," + String(gps.speed.kmph()) + "," + String(gps.course.deg());
    String gps_data_e = String(gps.altitude.meters()) + "," +  String(gps.hdop.value());
    String packet = "[" + gps_valid + "," + gps_data_a  + "," + gps_data_b + "," + gps_data_c + "," + gps_data_d + "," + gps_data_e + "]";
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
    String sdcard = "[" + String(bytes_high) + String(bytes_low) + "," + String(used_bytes_high) + String(used_bytes_low) + "]";
    Serial_Print(sdcard);
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
    statusFlags[GPS_ON_FLAG_INDEX] = false;
    statusFlags[GPS_LOGGING_FLAG_INDEX] = false;
    EEPROM.write(GPS_ON_FLAG_INDEX, 0x00);
    EEPROM.write(GPS_LOGGING_FLAG_INDEX, 0x00);
    EEPROM.commit();
    Serial_Print("[reset_complete]\n\r");
  }
}

void loop() {

  String gnss_data = "";

  if (statusFlags[GPS_ON_FLAG_INDEX]) {
    while (Serial2.available()) {
      int raw_data = Serial2.read();
      gps.encode(raw_data);
      gnss_data = String(gnss_data + String((char)raw_data));
    }

    if (gps.location.age() > 10000) statusFlags[GPS_FIX_FLAG_INDEX] = false;
    else statusFlags[GPS_FIX_FLAG_INDEX] = true;

    if (SERIAL_PRINT_FLAG && gnss_data != "") Serial_Print(gnss_data);


    log_buffer = log_buffer + gnss_data;
    if (statusFlags[GPS_LOGGING_FLAG_INDEX] && log_counter % 1000 == 0 && log_counter != 0) {
      appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", log_buffer);
      log_buffer = "";
      log_counter = 0;
    }
  }
  else statusFlags[GPS_FIX_FLAG_INDEX] = false;
  log_counter++;

  CMD_EVENT();
}
