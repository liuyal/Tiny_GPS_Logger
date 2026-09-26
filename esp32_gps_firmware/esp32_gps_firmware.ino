#include <EEPROM.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"
#include "TinyGPS++.h"

#define GNSS_BUF_SIZE 128
#define LED_PIN 22
#define RXD2 16
#define TXD2 17
#define DEBUG true

#define DEBUG_PRINT(x) \
  do                   \
  {                    \
    if (DEBUG)         \
      Serial.print(x); \
  } while (0)

#define DEBUG_PRINT_LN(x) \
  do                      \
  {                       \
    if (DEBUG)            \
      Serial.println(x);  \
  } while (0)

TinyGPSPlus gps;
const int CS = 5;

void SD_INIT()
{
  if (!SD.begin(CS))
  {
    DEBUG_PRINT_LN("SD Card Initialization Failed!");
    return;
  }
  DEBUG_PRINT_LN("Initialized SD Card!");
  DEBUG_PRINT_LN("------- SD Card Info -------");

  uint8_t cardType = SD.cardType();
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();

  DEBUG_PRINT("SD Card Type:\t");
  if (cardType == CARD_MMC)
    DEBUG_PRINT_LN("MMC");
  else if (cardType == CARD_SD)
    DEBUG_PRINT_LN("SDSC");
  else if (cardType == CARD_SDHC)
    DEBUG_PRINT_LN("SDHC");
  else if (cardType == CARD_NONE)
    DEBUG_PRINT_LN("No SD Card Attached");
  else
    DEBUG_PRINT_LN("UNKNOWN");

  DEBUG_PRINT_LN("Volume(KB):\t" + String((float)bytes / (1000)));
  DEBUG_PRINT_LN("Volume(MB):\t" + String((float)bytes / (1000 * 1000)));
  DEBUG_PRINT_LN("Volume(GB):\t" + String((float)bytes / (1000 * 1000 * 1000)));

  DEBUG_PRINT_LN("Used(KB):\t" + String((float)used_bytes / (1000)));
  DEBUG_PRINT_LN("Used(MB):\t" + String((float)used_bytes / (1000 * 1000)));
  DEBUG_PRINT_LN("Used(GB):\t" + String((float)used_bytes / (1000 * 1000 * 1000)));
  DEBUG_PRINT_LN("--------------------------\n");
}

int listDir(fs::FS &fs, String dirname, uint8_t levels)
{
  int count = 0;
  DEBUG_PRINT_LN("Listing directory: " + dirname + "\n");
  File root = fs.open(dirname);
  if (!root || !root.isDirectory())
  {
    DEBUG_PRINT_LN("Failed to open directory\n");
    return -1;
  }
  File file = root.openNextFile();
  while (file)
  {
    if (file.isDirectory())
    {
      DEBUG_PRINT_LN("  DIR : " + String(file.name()) + "\n");
      if (levels)
        listDir(fs, file.name(), levels - 1);
    }
    else
    {
      count += 1;
      DEBUG_PRINT_LN("  FILE: " + String(file.name()) + "  SIZE: " + String(file.size()) + "\n");
    }
    file = root.openNextFile();
  }
  return count;
}

void createDir(fs::FS &fs, String path)
{
  if (SD.exists(path))
  {
    DEBUG_PRINT_LN("Dir " + path + " Exists...\n");
    return;
  }
  DEBUG_PRINT_LN("Creating Dir: " + path + "\n");
  if (fs.mkdir(path))
    DEBUG_PRINT_LN("Dir created\n");
  else
    DEBUG_PRINT_LN("mkdir failed\n");
}

void removeDir(fs::FS &fs, String path)
{
  DEBUG_PRINT_LN("Removing Dir: " + path + "\n");
  if (fs.rmdir(path))
    DEBUG_PRINT_LN("Dir removed\n");
  else
    DEBUG_PRINT_LN("rmdir failed\n");
}

void readFile(fs::FS &fs, String path)
{
  DEBUG_PRINT_LN("Reading file: " + path + "\n");
  File file = fs.open(path);
  if (!file)
  {
    DEBUG_PRINT_LN("Failed to open file for reading\n");
    return;
  }
  DEBUG_PRINT_LN("Read from file: ");
  while (file.available())
    Serial.write(file.read());
  file.close();
}

void writeFile(fs::FS &fs, String path, String message)
{
  DEBUG_PRINT_LN("Writing file: " + path + "\n");
  File file = fs.open(path, FILE_WRITE);
  if (!file)
  {
    DEBUG_PRINT_LN("Failed to open file for writing\n");
    return;
  }
  if (file.print(message))
    DEBUG_PRINT_LN("File written\n");
  else
    DEBUG_PRINT_LN("Write failed\n");
  file.close();
}

void appendFile(fs::FS &fs, String path, String message)
{
  DEBUG_PRINT_LN("Append to file: " + path + " ");
  File file = fs.open(path, FILE_APPEND);
  if (!file)
  {
    DEBUG_PRINT_LN("Failed to open file\n");
    return;
  }
  if (file.print(message))
    DEBUG_PRINT_LN("| Data appended\n");
  else
    DEBUG_PRINT_LN("Append failed\n");
  file.close();
}

void renameFile(fs::FS &fs, String path1, String path2)
{
  DEBUG_PRINT_LN("Renaming file " + path1 + " to " + path2 + "\n");
  if (fs.rename(path1, path2))
    DEBUG_PRINT_LN("File renamed\n");
  else
    DEBUG_PRINT_LN("Rename failed\n");
}

void deleteFile(fs::FS &fs, String path)
{
  DEBUG_PRINT_LN("Deleting file: " + path + "\n");
  if (fs.remove(path))
    DEBUG_PRINT_LN("File deleted\n");
  else
    DEBUG_PRINT_LN("Delete failed\n");
}

void setup()
{
  // LED
  pinMode(LED_PIN, OUTPUT);
  // Initialize Serial
  Serial.begin(115200);
  // Initialize HW Serial to NEO
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);

  // Initialize SD card
  SD_INIT();

  DEBUG_PRINT_LN("Initialization Complete!");
}

void loop()
{
  static char gnss_data[GNSS_BUF_SIZE];
  static size_t gnss_len = 0;

  // Read from NEO serial
  while (Serial2.available())
  {
    int raw_data = Serial2.read();
    gps.encode(raw_data);
    // leave room for the null terminator
    if (gnss_len < GNSS_BUF_SIZE - 1)
    {
      gnss_data[gnss_len++] = (char)raw_data;
    }
  }

  // Check fix and set LED status
  if (gps.location.isValid())
  {
    digitalWrite(LED_PIN, HIGH);
  }
  else
  {
    digitalWrite(LED_PIN, LOW);
  }

  gnss_data[gnss_len] = '\0';
  DEBUG_PRINT(gnss_data);
  gnss_len = 0;

  delay(1000);
}
