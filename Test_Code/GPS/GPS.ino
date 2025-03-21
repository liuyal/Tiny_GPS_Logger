#include <EEPROM.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"
#include "TinyGPS++.h"

#define DEBUG true

#if DEBUG
#define DEBUG_PRINT(x) Serial.print(x);
#else
#define DEBUG_PRINT(x) \
  do { \
  } while (0);
#endif

#if DEBUG
#define DEBUG_PRINT_LN(x) Serial.println(x);
#else
#define DEBUG_PRINT_LN(x) \
  do { \
  } while (0);
#endif

#define LED_PIN 22
#define RXD2 16
#define TXD2 17

TinyGPSPlus gps;

const int CS_PIN = 5;
int nFiles = 0;

/**********************************************************************
   SDCard Functions
 **********************************************************************/

void SD_INIT() {

  DEBUG_PRINT_LN("Initializing SD Card...");

  if (!SD.begin(CS_PIN)) {
    DEBUG_PRINT_LN("SD Card Initialization Failed!");
    return;
  }

  DEBUG_PRINT_LN("-------SD Card Info-------");

  uint8_t cardType = SD.cardType();
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();

  if (cardType == CARD_MMC) {
    DEBUG_PRINT_LN("SD Card Type:\tMMC");
  } else if (cardType == CARD_SD) {
    DEBUG_PRINT_LN("SD Card Type:\tSDSC");
  } else if (cardType == CARD_SDHC) {
    DEBUG_PRINT_LN("SD Card Type:\tSDHC");
  } else if (cardType == CARD_NONE) {
    DEBUG_PRINT_LN("SD Card Type:\tNo SD Card Attached");
  } else {
    DEBUG_PRINT_LN("UNKNOWN");
  }

  DEBUG_PRINT("Used(MB):\t");
  DEBUG_PRINT_LN((float)used_bytes / (1000 * 1000));
  DEBUG_PRINT("Used(GB):\t");
  DEBUG_PRINT_LN((float)used_bytes / (1000 * 1000 * 1000));
  DEBUG_PRINT("Volume(MB):\t");
  DEBUG_PRINT_LN((float)bytes / (1000 * 1000));
  DEBUG_PRINT("Volume(GB):\t");
  DEBUG_PRINT_LN((float)bytes / (1000 * 1000 * 1000));

  nFiles = countFiles(SD, "/", 0);

  DEBUG_PRINT("Files Count:\t");
  DEBUG_PRINT_LN(nFiles);
  DEBUG_PRINT_LN("--------------------------");
}

int countFiles(fs::FS &fs, String dirname, uint8_t levels) {

  int count = 0;
  File root = fs.open(dirname);
  if (!root || !root.isDirectory()) {
    return -1;
  }
  File file = root.openNextFile();

  while (file) {
    if (file.isDirectory()) {
      if (levels) countFiles(fs, file.name(), levels - 1);
    } else count += 1;

    file = root.openNextFile();
  }

  return count;
}


void appendFile(fs::FS &fs, String path, String message) {

  File file = fs.open(path, FILE_APPEND);
  if (!file) return;
  file.print(message);
  file.close();
}


/**********************************************************************
  Main
 **********************************************************************/

void setup() {

  // LED
  pinMode(LED_PIN, OUTPUT);

  // Initialize Serial
  Serial.begin(115200);

  // Initialize HW Serial to NEO
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);

  // Initialize SD Card
  SD_INIT();

  DEBUG_PRINT_LN("Initialization Complete!");
}

void loop() {

  String gnss_data = "";

  // Read from NEO serial
  while (Serial2.available()) {
    int raw_data = Serial2.read();
    gps.encode(raw_data);
    gnss_data += (char)raw_data;
  }

  // // Check fix and log to file
  // if (gps.location.isValid()) {
  //   digitalWrite(LED_PIN, HIGH);
  //   appendFile(SD, "/" + String(nFiles) + ".log", gnss_data);

  // } else {
  //   digitalWrite(LED_PIN, LOW);
  //   DEBUG_PRINT_LN("No GNSS FIX...");
  // }
  
  DEBUG_PRINT(gnss_data)
  delay(1000);
  appendFile(SD, "/" + String(nFiles) + ".log", gnss_data);
}
