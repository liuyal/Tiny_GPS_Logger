#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"

const int CS = 5;
String gnss_dir = "GNSS_LOGS";
int nfiles = 0;
bool logging_on = true;

void setup() {
  Serial.begin(115200);
  SD_INIT();
  listDir(SD, "/", 0);
  createDir(SD, "/" + gnss_dir);
  nfiles = listDir(SD, "/" + gnss_dir, 0);
}

void Serial_Print(String msg) {
  Serial.print(msg);
}

void BLE_INIT(){

}

void SD_INIT() {
  Serial_Print("\nInitializing SD Card...\n");
  if (!SD.begin(CS)) {
    Serial_Print("Initialization Failed!\n");
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
  Serial_Print("Volume(KB):\t" + String((float)bytes / (1000)) + "\n");
  Serial_Print("Volume(MB):\t" + String((float)bytes / (1000 * 1000)) + "\n");
  Serial_Print("Volume(GB):\t" + String((float)bytes / (1000 * 1000 * 1000)) + "\n");
  Serial_Print("Used(KB):\t" + String((float)used_bytes / (1000)) + "\n");
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
    return 0;
  }
  File file = root.openNextFile();
  while (file) {
    if (file.isDirectory()) {
      Serial_Print("  DIR : " + String(file.name()) + "\n");
      time_t t = file.getLastWrite();
      struct tm * tmstruct = localtime(&t);
      //      Serial.printf("  LAST WRITE: %d-%02d-%02d %02d:%02d:%02d\n", (tmstruct->tm_year) + 1900, ( tmstruct->tm_mon) + 1, tmstruct->tm_mday, tmstruct->tm_hour , tmstruct->tm_min, tmstruct->tm_sec);
      if (levels) listDir(fs, file.name(), levels - 1);
    }
    else {
      count += 1;
      Serial_Print("  FILE: " + String(file.name()) + "  SIZE: " + String(file.size()) + "\n");
      time_t t = file.getLastWrite();
      struct tm * tmstruct = localtime(&t);
      //      Serial.printf("  LAST WRITE: %d-%02d-%02d %02d:%02d:%02d\n", (tmstruct->tm_year) + 1900, ( tmstruct->tm_mon) + 1, tmstruct->tm_mday, tmstruct->tm_hour , tmstruct->tm_min, tmstruct->tm_sec);
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

void writeFile(fs::FS &fs, String path, String message) {
  Serial_Print("Writing file: " + path + "\n");
  File file = fs.open(path, FILE_WRITE);
  if (!file) {
    Serial_Print("Failed to open file for writing\n");
    return;
  }
  if (file.print(message)) Serial_Print("File written\n");
  else Serial_Print("Write failed\n");
  file.close();
}

void appendFile(fs::FS &fs, String path, String message) {
  Serial_Print("Append to file: " + path + " ");
  File file = fs.open(path, FILE_APPEND);
  if (!file) {
    Serial_Print("Failed to open file\n");
    return;
  }
  if (file.print(message)) Serial_Print("| Data appended\n");
  else Serial_Print("Append failed\n");
  file.close();
}

void renameFile(fs::FS &fs, String path1, String path2) {
  Serial_Print("Renaming file " + path1 + " to " + path2 + "\n");
  if (fs.rename(path1, path2)) Serial_Print("File renamed\n");
  else Serial_Print("Rename failed\n");
}

void deleteFile(fs::FS &fs, String path) {
  Serial_Print("Deleting file: " + path + "\n");
  if (fs.remove(path))Serial_Print("File deleted\n");
  else Serial_Print("Delete failed\n");
}

void loop() {
  delay(1000);
  // TODO: Add system on/off, Insert GNSS data 
  if (logging_on) {
    int data1 = millis();
    appendFile(SD, "/" + gnss_dir + "/GPS_" + String(nfiles) + ".log", String(data1) + "\n");
  }

}
