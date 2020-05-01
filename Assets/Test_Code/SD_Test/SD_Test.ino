
#include <SPI.h>
#include <SD.h>

Sd2Card card;
SdVolume volume;
SdFile root;

File myFile;
const int CS = 4;

void setup() {
  Serial.begin(9600);
  SD_INFO();

  if (!SD.begin(CS)) {
    Serial.println("\nSD Init Failed!");
    return;
  }
  Serial.println("\nSD Initialization Success!");
}

void SD_INFO() {

  Serial.println("Mounting SD Card...");
  if (!card.init(SPI_HALF_SPEED, CS)) {
    Serial.println("SD Mount Failed!");
    return;
  }
  Serial.print("SD Mount Success!\n\nCard type:\t\t");
  switch (card.type()) {
    case SD_CARD_TYPE_SD1:
      Serial.println("SD1"); break;
    case SD_CARD_TYPE_SD2:
      Serial.println("SD2");
      break;
    case SD_CARD_TYPE_SDHC:
      Serial.println("SDHC");
      break;
    default:
      Serial.println("Unknown");
  }
  if (!volume.init(card)) {
    Serial.println("Could not find FAT16/FAT32 partition.");
    while (1);
  }
  Serial.print("Clusters:\t\t");
  Serial.println(volume.clusterCount());
  Serial.print("Blocks x Cluster:\t");
  Serial.println(volume.blocksPerCluster());
  Serial.print("Total Blocks:\t\t");
  Serial.println(volume.blocksPerCluster() * volume.clusterCount());
  uint32_t volumesize;
  Serial.print("\nVolume type is:\t\tFAT");
  Serial.println(volume.fatType(), DEC);
  volumesize = volume.blocksPerCluster();
  volumesize *= volume.clusterCount();
  volumesize /= 2;
  Serial.println("Volume size (Kb):\t" + String(volumesize));
  Serial.println("Volume size (Mb):\t" + String((float)volumesize / 1024.0));
  Serial.println("Volume size (Gb):\t" + String((float)volumesize / (1024.0 * 1024.0)));
  Serial.println("\nList Files (name, date, and size in bytes): ");
  root.openRoot(volume);
  root.ls(LS_R | LS_DATE | LS_SIZE);
}




void loop() {


}
