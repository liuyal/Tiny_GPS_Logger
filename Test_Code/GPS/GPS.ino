#include <EEPROM.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"
#include "TinyGPS++.h"

#define DEBUG true
#define LED_PIN 22
#define RXD2 16
#define TXD2 17

TinyGPSPlus gps;
File myFile;

const int CS_PIN = 5;

void logger(String msg, bool nl = true) {
#if DEBUG
  if (nl) { msg = msg + "\n"; }
  Serial.print(msg);
#endif
}


/**********************************************************************
   SDCard Functions
 **********************************************************************/

void SD_INIT() {
  logger("\n\rInitializing SD Card...");

  if (!SD.begin(CS_PIN)) {
    logger("SD Card Initialization Failed!");
    delay(1000);
    return;
  }

  logger("-------SD Card Info-------");
  uint8_t cardType = SD.cardType();
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();

  if (cardType == CARD_MMC) logger("SD Card Type:\tMMC");
  else if (cardType == CARD_SD) logger("SD Card Type:\tSDSC");
  else if (cardType == CARD_SDHC) logger("SD Card Type:\tSDHC");
  else if (cardType == CARD_NONE) logger("SD Card Type:\tNo SD Card Attached");
  else logger("UNKNOWN");

  logger("Used(MB):\t" + String((float)used_bytes / (1000 * 1000)));
  logger("Used(GB):\t" + String((float)used_bytes / (1000 * 1000 * 1000)));
  logger("Volume(MB):\t" + String((float)bytes / (1000 * 1000)));
  logger("Volume(GB):\t" + String((float)bytes / (1000 * 1000 * 1000)));
  logger("--------------------------");
}


// void appendFile(fs::FS &fs, String path, String message) {
//   File file = fs.open(path, FILE_APPEND);
//   if (!file) return;
//   file.print(message);
//   file.close();
// }

// String readFile(fs::FS &fs, String path) {
//   String text = "";
//   logger("Reading file: " + path + "\n\r");
//   File file = fs.open(path);
//   if (!file) {
//     return "Failed to open file for reading\n\r";
//   }
//   while (file.available()) {
//     text = text + (char)file.read();
//   }
//   file.close();
//   return text;
// }

// void deleteFile(fs::FS &fs, String path) {
//   logger("Deleting file: " + path + "\n\r");
//   if (fs.remove(path)) logger("File deleted\n\r");
//   else logger("Delete failed\n\r");
// }

/**********************************************************************
  Main
 **********************************************************************/

void setup() {

  // LED
  pinMode(LED_PIN, OUTPUT);

  // Initialize Serial
  Serial.begin(115200);

  // Initialize Serial to NEO
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);

  // Initialize SD Card
  SD_INIT();

  logger("Initialization Complete!");
}

void loop() {

  String gnss_data = "";

  // Read from NEO serial
  while (Serial2.available()) {
    int raw_data = Serial2.read();
    gps.encode(raw_data);
    gnss_data = String(gnss_data + String((char)raw_data));
  }

  if (gps.location.isValid()) {
    digitalWrite(LED_PIN, HIGH);
    // appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", log_buffer);

  } else {
    digitalWrite(LED_PIN, LOW);
    // logger("No GNSS FIX...");
    // delay(30000);
  }

  logger(gnss_data, false);
}
