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


}
