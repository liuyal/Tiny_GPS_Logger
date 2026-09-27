#include <EEPROM.h>
#include <NimBLEDevice.h>
#include <SPI.h>
#include "SD.h"
#include "FS.h"
#include "TinyGPS++.h"

/**********************************************************************
   Defines
 **********************************************************************/

// See the following for generating UUIDs: https://www.uuidgenerator.net/
#define SERVICE_UUID "000ffdf4-68d9-4e48-a89a-219e581f0d64"
#define CHARACTERISTIC_UUID "44a80b83-c605-4406-8e50-fc42f03b6d38"
#define GNSS_BUF_SIZE 128
#define LOG_BUFFER_SIZE 512
#define LOG_FLUSH_THRESHOLD (LOG_BUFFER_SIZE - GNSS_BUF_SIZE)
#define LED_PIN 22
#define RXD2 16
#define TXD2 17
#define DEBUG true

#define DEBUG_PRINT(...)         \
  do                             \
  {                              \
    if (DEBUG)                   \
      Serial.print(__VA_ARGS__); \
  } while (0)

#define DEBUG_PRINT_LN(...)        \
  do                               \
  {                                \
    if (DEBUG)                     \
      Serial.println(__VA_ARGS__); \
  } while (0)

/**********************************************************************
   Global Variables
 **********************************************************************/

// BLE server, service, and characteristic objects
NimBLEServer *pServer = NULL;
NimBLEService *pService = NULL;
NimBLECharacteristic *pCharacteristic = NULL;

// GPS object and SD card chip select pin
TinyGPSPlus gps;
const int CS = 5;

// System status flags
const int NUMBER_OF_FLAGS = 4;
bool statusFlags[NUMBER_OF_FLAGS];

// statusFlags indices
const int BLE_CONNECTED = 0;
const int GPS_ENABLED = 1;
const int GPS_HAS_FIX = 2;
const int GPS_LOGGING_ENABLED = 3;

// fixed-size status flag payload shared by the BLE and serial "status" replies
typedef byte StatusBuf[NUMBER_OF_FLAGS];

// GNSS log directory and buffer
char gnss_dir[] = "GNSS_LOGS";
char log_buffer[LOG_BUFFER_SIZE] = "";
int nfiles = 0;

/**********************************************************************
   SD Card Functions
 **********************************************************************/

int listDir(fs::FS &fs, const char *dirname, uint8_t levels)
{
  int count = 0;
  char info_buf[160];
  File root = fs.open(dirname);

  DEBUG_PRINT("Listing directory: ");
  DEBUG_PRINT_LN(dirname);

  if (!root || !root.isDirectory())
  {
    DEBUG_PRINT_LN("Failed to open directory");
    return -1;
  }

  File file = root.openNextFile();
  while (file)
  {
    if (file.isDirectory())
    {
      snprintf(info_buf, sizeof(info_buf), "  DIR : %s", file.name());
      DEBUG_PRINT_LN(info_buf);
      if (levels)
        listDir(fs, file.name(), levels - 1);
    }
    else
    {
      count += 1;
      snprintf(info_buf, sizeof(info_buf), "  FILE: %s  SIZE: %u", file.name(), (unsigned)file.size());
      DEBUG_PRINT_LN(info_buf);
    }
    file = root.openNextFile();
  }

  return count;
}

void createDir(fs::FS &fs, const char *path)
{
  char info_buf[160];
  if (SD.exists(path))
  {
    snprintf(info_buf, sizeof(info_buf), "Dir %s Exists...", path);
    DEBUG_PRINT_LN(info_buf);
    return;
  }
  snprintf(info_buf, sizeof(info_buf), "Creating Dir: %s", path);
  DEBUG_PRINT_LN(info_buf);
  if (fs.mkdir(path))
    DEBUG_PRINT_LN("Dir created");
  else
    DEBUG_PRINT_LN("mkdir failed");
}

void removeDir(fs::FS &fs, const char *path)
{
  char info_buf[160];
  snprintf(info_buf, sizeof(info_buf), "Removing Dir: %s", path);
  DEBUG_PRINT_LN(info_buf);
  if (fs.rmdir(path))
    DEBUG_PRINT_LN("Dir removed");
  else
    DEBUG_PRINT_LN("rmdir failed");
}

void readFile(fs::FS &fs, const char *path)
{
  char info_buf[160];
  snprintf(info_buf, sizeof(info_buf), "Reading file: %s", path);
  DEBUG_PRINT_LN(info_buf);
  File file = fs.open(path);
  if (!file)
  {
    DEBUG_PRINT_LN("Failed to open file for reading");
    return;
  }
  DEBUG_PRINT_LN("Read from file: ");
  while (file.available())
    Serial.write(file.read());
  file.close();
}

void writeFile(fs::FS &fs, const char *path, const char *message)
{
  char info_buf[160];
  snprintf(info_buf, sizeof(info_buf), "Writing file: %s", path);
  DEBUG_PRINT_LN(info_buf);
  File file = fs.open(path, FILE_WRITE);
  if (!file)
  {
    DEBUG_PRINT_LN("Failed to open file for writing");
    return;
  }
  if (file.print(message))
    DEBUG_PRINT_LN("File written");
  else
    DEBUG_PRINT_LN("Write failed");
  file.close();
}

void appendFile(fs::FS &fs, const char *path, const char *message)
{
  // char info_buf[160];
  // snprintf(info_buf, sizeof(info_buf), "Append to file: %s", path);
  // DEBUG_PRINT_LN(info_buf);
  File file = fs.open(path, FILE_APPEND);
  if (!file)
  {
    DEBUG_PRINT_LN("Failed to open file");
    return;
  }
  if (file.print(message))
  {
    // DEBUG_PRINT_LN("Data appended");
  }
  else
  {
    DEBUG_PRINT_LN("Append failed");
  }
  file.close();
}

void renameFile(fs::FS &fs, const char *path1, const char *path2)
{
  char info_buf[160];
  snprintf(info_buf, sizeof(info_buf), "Renaming file %s to %s", path1, path2);
  DEBUG_PRINT_LN(info_buf);
  if (fs.rename(path1, path2))
    DEBUG_PRINT_LN("File renamed");
  else
    DEBUG_PRINT_LN("Rename failed");
}

void deleteFile(fs::FS &fs, const char *path)
{
  char info_buf[160];
  snprintf(info_buf, sizeof(info_buf), "Deleting file: %s", path);
  DEBUG_PRINT_LN(info_buf);
  if (fs.remove(path))
    DEBUG_PRINT_LN("File deleted");
  else
    DEBUG_PRINT_LN("Delete failed");
}

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

  char info_buf[48];
  snprintf(info_buf, sizeof(info_buf), "Volume(KB):\t%.2f", (float)bytes / 1000);
  DEBUG_PRINT_LN(info_buf);
  snprintf(info_buf, sizeof(info_buf), "Volume(MB):\t%.2f", (float)bytes / (1000 * 1000));
  DEBUG_PRINT_LN(info_buf);
  snprintf(info_buf, sizeof(info_buf), "Volume(GB):\t%.2f", (float)bytes / (1000 * 1000 * 1000));
  DEBUG_PRINT_LN(info_buf);
  snprintf(info_buf, sizeof(info_buf), "Used(KB):\t%.2f", (float)used_bytes / 1000);
  DEBUG_PRINT_LN(info_buf);
  snprintf(info_buf, sizeof(info_buf), "Used(MB):\t%.2f", (float)used_bytes / (1000 * 1000));
  DEBUG_PRINT_LN(info_buf);
  snprintf(info_buf, sizeof(info_buf), "Used(GB):\t%.2f", (float)used_bytes / (1000 * 1000 * 1000));
  DEBUG_PRINT_LN(info_buf);

  DEBUG_PRINT_LN("--------------------------\n");
}

void FS_INIT(bool reset)
{
  char gnss_path[sizeof(gnss_dir) + 1];
  snprintf(gnss_path, sizeof(gnss_path), "/%s", gnss_dir);

  if (reset)
    removeDir(SD, gnss_path);

  listDir(SD, "/", 0);
  createDir(SD, gnss_path);

  int count = listDir(SD, gnss_path, 0);
  nfiles = count;
}

/**********************************************************************
   Shared Command Functions
   (used by both the BLE opcode handler and the serial command handler)
 **********************************************************************/

void build_status_buf(byte *buf)
{
  buf[BLE_CONNECTED] = statusFlags[BLE_CONNECTED] ? 0x01 : 0x00;
  buf[GPS_ENABLED] = statusFlags[GPS_ENABLED] ? 0x01 : 0x00;
  buf[GPS_HAS_FIX] = statusFlags[GPS_HAS_FIX] ? 0x01 : 0x00;
  buf[GPS_LOGGING_ENABLED] = statusFlags[GPS_LOGGING_ENABLED] ? 0x01 : 0x00;
}

void gps_start()
{
  statusFlags[GPS_ENABLED] = true;
  EEPROM.write(GPS_ENABLED, 0x01);
  EEPROM.commit();
}

void gps_stop()
{
  statusFlags[GPS_ENABLED] = false;
  EEPROM.write(GPS_ENABLED, 0x00);
  EEPROM.commit();
}

void logging_start()
{
  statusFlags[GPS_LOGGING_ENABLED] = true;
  EEPROM.write(GPS_LOGGING_ENABLED, 0x01);
  EEPROM.commit();
}

void logging_stop()
{
  statusFlags[GPS_LOGGING_ENABLED] = false;
  EEPROM.write(GPS_LOGGING_ENABLED, 0x00);
  EEPROM.commit();
}

void build_gps_packet(char *buf, size_t size)
{
  snprintf(buf, size,
           "[%d,%d,%lu,%.7f,%.7f,%u,%u,%u,%u,%u,%u,%lu,%.2f,%.2f,%.2f,%lu]",
           gps.location.isValid(),
           gps.location.isUpdated(),
           (unsigned long)gps.location.age(),
           gps.location.lat(),
           gps.location.lng(),
           gps.date.year(),
           gps.date.month(),
           gps.date.day(),
           gps.time.hour(),
           gps.time.minute(),
           gps.time.second(),
           (unsigned long)gps.satellites.value(),
           gps.speed.kmph(),
           gps.course.deg(),
           gps.altitude.meters(),
           (unsigned long)gps.hdop.value());
}

void build_sdcard_info(char *buf, size_t size)
{
  uint64_t bytes = SD.totalBytes();
  uint64_t used_bytes = SD.usedBytes();
  uint32_t bytes_low = (uint32_t)bytes;
  uint32_t bytes_high = (uint32_t)(bytes >> 32);
  uint32_t used_bytes_low = (uint32_t)used_bytes;
  uint32_t used_bytes_high = (uint32_t)(used_bytes >> 32);
  snprintf(buf, size, "[%lu,%lu,%lu,%lu]",
           (unsigned long)bytes_high,
           (unsigned long)bytes_low,
           (unsigned long)used_bytes_high,
           (unsigned long)used_bytes_low);
}

void system_reboot(uint32_t delay_ms)
{
  delay(delay_ms);
  ESP.restart();
}

void system_reset()
{
  statusFlags[GPS_ENABLED] = false;
  statusFlags[GPS_LOGGING_ENABLED] = false;
  EEPROM.write(GPS_ENABLED, 0x00);
  EEPROM.write(GPS_LOGGING_ENABLED, 0x00);
  EEPROM.commit();
  delay(1000);
  FS_INIT(true);
}

/**********************************************************************
   BLE Functions
 **********************************************************************/

class ServerCallbacks : public NimBLEServerCallbacks
{
  void onConnect(NimBLEServer *pServer, NimBLEConnInfo &connInfo)
  {
    DEBUG_PRINT_LN("BLE Device Connected");
  };
  void onDisconnect(NimBLEServer *pServer, NimBLEConnInfo &connInfo, int reason)
  {
    DEBUG_PRINT_LN("BLE Device Disconnect");
  }
};

// sends the 2-byte ack payload used by simple fire-and-forget BLE commands
void send_ack()
{
  byte buf[2] = {0x01, 0x01};
  pCharacteristic->setValue(buf, sizeof(buf));
  pCharacteristic->indicate();
}

// sends a null-terminated string over BLE and mirrors it to the debug log
void ble_indicate_and_log(const char *packet)
{
  pCharacteristic->setValue((const uint8_t *)packet, strlen(packet));
  pCharacteristic->indicate();
  DEBUG_PRINT_LN(packet);
}

class BLE_Callbacks : public NimBLECharacteristicCallbacks
{
  void onWrite(NimBLECharacteristic *pCharacteristic, NimBLEConnInfo &connInfo)
  {
    char value[64];
    size_t len = pCharacteristic->getValue().length();
    len = len < sizeof(value) - 1 ? len : sizeof(value) - 1;
    memcpy(value, pCharacteristic->getValue().data(), len);
    value[len] = '\0';

    if (len > 0)
    {
      DEBUG_PRINT("Value: ");
      for (size_t i = 0; i < len; i++)
        DEBUG_PRINT(value[i], HEX);
      DEBUG_PRINT_LN("");

      if (value[0] == 0x00)
      {
        ble_indicate_and_log("[BLE] ESP32_GPS");
      }
      else if (value[0] == 0x01)
      {
        StatusBuf buf;
        char status_buf[32];
        build_status_buf(buf);
        pCharacteristic->setValue(buf, sizeof(buf));
        pCharacteristic->indicate();
        snprintf(status_buf, sizeof(status_buf), "[BLE] Status: %d%d%d%d", buf[0], buf[1], buf[2], buf[3]);
        DEBUG_PRINT_LN(status_buf);
      }
      else if (value[0] == 0x02)
      {
        DEBUG_PRINT_LN("[BLE] Start GPS");
        gps_start();
        send_ack();
      }
      else if (value[0] == 0x03)
      {
        DEBUG_PRINT_LN("[BLE] Stop GPS");
        gps_stop();
        send_ack();
      }
      else if (value[0] == 0x04)
      {
        DEBUG_PRINT_LN("[BLE] Start logging");
        logging_start();
        send_ack();
      }
      else if (value[0] == 0x05)
      {
        DEBUG_PRINT_LN("[BLE] Stop logging");
        logging_stop();
        send_ack();
      }
      else if (value[0] == 0x06)
      {
        char packet[200];
        build_gps_packet(packet, sizeof(packet));
        ble_indicate_and_log(packet);
      }
      /*
      else if (value[0] == 0x07)
      {
        // char gnss_path[32];
        // snprintf(gnss_path, sizeof(gnss_path), "/%s", gnss_dir);
        // int count = listDir(SD, gnss_path, 0);
        // char listing[16];
        // snprintf(listing, sizeof(listing), "[%d]", count);
        // pCharacteristic->setValue((const uint8_t *)listing, strlen(listing));
        // pCharacteristic->indicate();
        // DEBUG_PRINT_LN(listing);
        // TODO: list file contents over BLE
      }
      else if (value[0] == 0x08)
      {
        // char log_path[64];
        // snprintf(log_path, sizeof(log_path), "/%s/GPS_%d.log", gnss_dir, (int)value[1]);
        // readFile(SD, log_path);
        // TODO: send file contents over BLE
      }
      */
      else if (value[0] == 0x09)
      {
        char sdcard_buf[64];
        build_sdcard_info(sdcard_buf, sizeof(sdcard_buf));
        ble_indicate_and_log(sdcard_buf);
      }
      else if (value[0] == 0x0a)
      {
        DEBUG_PRINT_LN("[BLE] Rebooting");
        send_ack();
        system_reboot(2000);
      }
      else if (value[0] == 0x0b)
      {
        DEBUG_PRINT_LN("[BLE] System reset");
        send_ack();
        system_reset();
        DEBUG_PRINT_LN("[BLE] System reset complete");
      }
    }
  }
};

void BLE_INIT()
{
  char uuid_buf[96];
  DEBUG_PRINT_LN("Initializing BLE...");
  snprintf(uuid_buf, sizeof(uuid_buf), "SERVICE UUID: %s", SERVICE_UUID);
  DEBUG_PRINT_LN(uuid_buf);
  snprintf(uuid_buf, sizeof(uuid_buf), "CHARACTERISTIC UUID: %s", CHARACTERISTIC_UUID);
  DEBUG_PRINT_LN(uuid_buf);
  DEBUG_PRINT_LN("Starting BLE Server...");

  NimBLEDevice::init("ESP32_GPS_LOGGER");
  DEBUG_PRINT("BLE MAC Address: ");
  DEBUG_PRINT_LN(NimBLEDevice::getAddress().toString().c_str());

  pServer = NimBLEDevice::createServer();
  pServer->setCallbacks(new ServerCallbacks());
  pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::NOTIFY | NIMBLE_PROPERTY::INDICATE);
  pCharacteristic->setCallbacks(new BLE_Callbacks());
  pCharacteristic->createDescriptor("2901", NIMBLE_PROPERTY::READ)->setValue("BLE Control Service");
  pService->start();

  NimBLEAdvertising *pAdvertising = NimBLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->enableScanResponse(true);
  pAdvertising->setPreferredParams(0x06, 0x12);
  pServer->getAdvertising()->start();

  DEBUG_PRINT_LN("GATT Service Defined!");
  DEBUG_PRINT_LN("GATT Characteristic Defined!");
}

/**********************************************************************
   Control Command Functions
 **********************************************************************/

// strip trailing CR/LF left over from a serial command argument
void trim_line_ending(char *str)
{
  size_t len = strlen(str);
  while (len > 0 && (str[len - 1] == '\r' || str[len - 1] == '\n'))
    str[--len] = '\0';
}

void control_cmd_event()
{
  static char cmd_buf[64];
  size_t cmd_len = 0;

  while (Serial.available() > 0 && cmd_len < sizeof(cmd_buf) - 1)
    cmd_buf[cmd_len++] = (char)Serial.read();
  cmd_buf[cmd_len] = '\0';

  if (cmd_len == 0)
    return;

  if (strstr(cmd_buf, "status"))
  {
    StatusBuf buf;
    char status_buf[32];
    build_status_buf(buf);
    snprintf(status_buf, sizeof(status_buf), "Status: %d%d%d%d", buf[0], buf[1], buf[2], buf[3]);
    DEBUG_PRINT_LN(status_buf);
  }
  else if (strstr(cmd_buf, "gps on") && !statusFlags[GPS_ENABLED])
  {
    DEBUG_PRINT_LN("Start GPS");
    gps_start();
  }
  else if (strstr(cmd_buf, "gps off") && statusFlags[GPS_ENABLED])
  {
    DEBUG_PRINT_LN("Stop GPS");
    gps_stop();
  }
  else if (strstr(cmd_buf, "log on") && !statusFlags[GPS_LOGGING_ENABLED])
  {
    DEBUG_PRINT_LN("Start logging");
    logging_start();
  }
  else if (strstr(cmd_buf, "log off") && statusFlags[GPS_LOGGING_ENABLED])
  {
    DEBUG_PRINT_LN("End logging");
    logging_stop();
  }
  else if (strstr(cmd_buf, "data"))
  {
    char packet[200];
    build_gps_packet(packet, sizeof(packet));
    DEBUG_PRINT_LN(packet);
  }
  else if (strstr(cmd_buf, "ls"))
  {
    char *arg = strstr(cmd_buf, "ls") + 2;
    while (*arg == ' ')
      arg++;
    trim_line_ending(arg);

    char list_path[64];
    if (arg[0] == '\0' || strcmp(arg, ".") == 0)
      strcpy(list_path, "/");
    else
      snprintf(list_path, sizeof(list_path), "/%s", arg);

    listDir(SD, list_path, 0);
  }
  else if (strstr(cmd_buf, "cat "))
  {
    char filename[52];
    strncpy(filename, strstr(cmd_buf, "cat ") + 4, sizeof(filename) - 1);
    filename[sizeof(filename) - 1] = '\0';
    trim_line_ending(filename);

    // bare filenames are assumed to live in the GNSS log directory;
    // paths containing '/' are treated as relative to the SD root
    char log_path[64];
    if (strchr(filename, '/'))
      snprintf(log_path, sizeof(log_path), "/%s", filename);
    else
      snprintf(log_path, sizeof(log_path), "/%s/%s", gnss_dir, filename);

    readFile(SD, log_path);
  }
  else if (strstr(cmd_buf, "sdcard"))
  {
    char sdcard_buf[64];
    build_sdcard_info(sdcard_buf, sizeof(sdcard_buf));
    DEBUG_PRINT_LN(sdcard_buf);
  }
  else if (strstr(cmd_buf, "reboot"))
  {
    DEBUG_PRINT_LN("Rebooting esp32");
    system_reboot(1000);
  }
  else if (strstr(cmd_buf, "reset"))
  {
    DEBUG_PRINT_LN("System reset");
    system_reset();
    DEBUG_PRINT_LN("Reset complete");
  }
}

void setup()
{
  // Initialize Serial
  Serial.begin(115200);
  DEBUG_PRINT_LN("Starting Initialization...");

  // Initialize EEPROM
  EEPROM.begin(64);

  // Initialize status flags from EEPROM
  for (int i = 0; i < NUMBER_OF_FLAGS; i++)
    statusFlags[i] = false;
  if (EEPROM.read(GPS_ENABLED) == 0x01)
    statusFlags[GPS_ENABLED] = true;
  if (EEPROM.read(GPS_LOGGING_ENABLED) == 0x01)
    statusFlags[GPS_LOGGING_ENABLED] = true;

  // Initialize HW Serial to NEO
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);

  // Initialize SD card
  SD_INIT();

  // Initialize FS
  FS_INIT(false);

  // Initialize BLE
  BLE_INIT();

  // Initialize GPS status LED
  pinMode(LED_PIN, OUTPUT);

  DEBUG_PRINT_LN("Initialization Complete!");
}

void loop()
{
  static char gnss_data[GNSS_BUF_SIZE];
  static size_t gnss_len = 0;

  if (statusFlags[GPS_ENABLED])
  {
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
      statusFlags[GPS_HAS_FIX] = true;
    }
    else
    {
      digitalWrite(LED_PIN, LOW);
      statusFlags[GPS_HAS_FIX] = false;
    }

    // bounded append to avoid overflowing the fixed-size log buffer
    strncat(log_buffer, gnss_data, sizeof(log_buffer) - strlen(log_buffer) - 1);

    // flush once the buffer is too full to safely hold another chunk
    if (statusFlags[GPS_LOGGING_ENABLED] && strlen(log_buffer) >= LOG_FLUSH_THRESHOLD)
    {
      char log_path[64];
      snprintf(log_path, sizeof(log_path), "/%s/GPS_%d.log", gnss_dir, nfiles);
      appendFile(SD, log_path, log_buffer);
      log_buffer[0] = '\0';
    }

    // Null-terminate the GNSS data buffer
    gnss_data[gnss_len] = '\0';
    // DEBUG_PRINT(gnss_data);
    gnss_len = 0;
  }
  else
  {
    statusFlags[GPS_HAS_FIX] = false;
  }

  delay(100);
  control_cmd_event();
}
