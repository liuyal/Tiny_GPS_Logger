#include <SPI.h>
#include "SD.h"
#include "FS.h"

File myFile;
const int CS = 5;

void setup() {
  Serial.begin(115200);
  SD_INIT();
}

void SD_INIT() {
  Serial.println("\nInitializing SD Card...");
  if (!SD.begin(CS)) {
    Serial.println("Initialization Failed!");
    return;
  }
  Serial.println("Initialization Success!");
  Serial.println("-------SD Card Info-------");
  Serial.print("SD Card Type:\t");
  uint8_t cardType = SD.cardType();
  if (cardType == CARD_MMC) Serial.println("MMC");
  else if (cardType == CARD_SD) Serial.println("SDSC");
  else if (cardType == CARD_SDHC) Serial.println("SDHC");
  else if (cardType == CARD_NONE) Serial.println("No SD Card Attached");
  else Serial.println("UNKNOWN");
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();
  Serial.println("Volume(KB):\t" + String((float)bytes / (1000)));
  Serial.println("Volume(MB):\t" + String((float)bytes / (1000 * 1000)));
  Serial.println("Volume(GB):\t" + String((float)bytes / (1000 * 1000 * 1000)));
  Serial.println("Used(KB):\t" + String((float)used_bytes / (1000)));
  Serial.println("Used(MB):\t" + String((float)used_bytes / (1000 * 1000)));
  Serial.println("Used(GB):\t" + String((float)used_bytes / (1000 * 1000 * 1000)));
  Serial.println("--------------------------\n");
}

void loop() {


}
