#include <EEPROM.h>
#include <SPI.h>
#include "TinyGPS++.h"

#define GNSS_BUF_SIZE 128
#define LED_PIN 22
#define RXD2 16
#define TXD2 17
#define DEBUG true

#define DEBUG_PRINT(x) \
  do { \
    if (DEBUG) \
      Serial.print(x); \
  } while (0)

#define DEBUG_PRINT_LN(x) \
  do { \
    if (DEBUG) \
      Serial.println(x); \
  } while (0)

TinyGPSPlus gps;

void setup() {
  // LED
  pinMode(LED_PIN, OUTPUT);
  // Initialize Serial
  Serial.begin(115200);
  // Initialize HW Serial to NEO
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2);

  DEBUG_PRINT_LN("Initialization Complete!");
}

void loop() {
  static char gnss_data[GNSS_BUF_SIZE];
  static size_t gnss_len = 0;

  // Read from NEO serial
  while (Serial2.available()) {
    int raw_data = Serial2.read();
    gps.encode(raw_data);
    // leave room for the null terminator
    if (gnss_len < GNSS_BUF_SIZE - 1) {
      gnss_data[gnss_len++] = (char)raw_data;
    }
  }

  gnss_data[gnss_len] = '\0';

  DEBUG_PRINT(gnss_data);
  gnss_len = 0;

  // Check fix and set LED status
  if (gps.location.isValid()) {
    digitalWrite(LED_PIN, HIGH);
  } else {
    digitalWrite(LED_PIN, LOW);
  }

  delay(1000);
}
