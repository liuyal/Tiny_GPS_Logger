# Tiny GPS Logger

A portable GPS logging device built with ESP32, NEO-6M, and SDCard Module. 

## Hardware Schematic

## Hardware Pin Connection

## Android App User Guide

## BLE Code

| Hex Code | Function | Description |
| :---: | --- | --- |
| 0x00 | None             		|  |
| 0x01 | Get device status 		|  |
| 0x02 | Toggle GPS on 			|  |
| 0x03 | Toggle GPS off 		|  |
| 0x04 | Toggle logging on 		|  |
| 0x05 | Toggle logging off 	|  |
| 0x06 | Toggle BLE print on 	|  |
| 0x07 | Toggle BLE print off 	|  |
| 0x08 | Get GPS data 			|  |
| 0x09 | List Files 			|  |
| 0x0a | Read File 				|  |
| 0x0b | Get SDCard Status 		|  |
| 0x0c | Reboot 				|  |
| 0x0d | Reset 					|  |
	
## GPS Status Flags

### 6 bit status flag system

| Bit Number | Function | Description |
| :---: | --- | --- |
| 1 | Device Connection 		|  |
| 2 | GPS Has Fix 				|  |
| 3 | GPS On/Off Status			|  |
| 4 | GPS Serial Print Status  	|  |
| 5 | GPS BLE Print Status  	|  |
| 6 | Logging Status 			|  |

## Resource Links

GPS
- [NMEA Messages Info](https://www.gpsinformation.org/dale/nmea.htm)
- [Tiny GPS++ Library](http://arduiniana.org/libraries/tinygpsplus/)
- [NEO-6M Guide A](https://randomnerdtutorials.com/guide-to-neo-6m-gps-module-with-arduino/)
- [NEO-6M Guide B](https://lastminuteengineers.com/neo6m-gps-arduino-tutorial/)
- [NEO-6 DataSheet](https://www.u-blox.com/sites/default/files/products/documents/NEO-6_DataSheet_%28GPS.G6-HW-09005%29.pdf)
- [NEO-6 Product Summary](https://www.u-blox.com/sites/default/files/products/documents/NEO-6_ProductSummary_%28GPS.G6-HW-09003%29.pdf)
- [NEOGPS](https://github.com/SlashDevin/NeoGPS/tree/master/examples)

SDCARD
- [ESP32 SDCard Example](https://randomnerdtutorials.com/esp32-data-logging-temperature-to-microsd-card/)
- [SDCard Example](https://lastminuteengineers.com/arduino-micro-sd-card-module-tutorial/)
- [SD Library](https://www.arduino.cc/en/reference/SD)

BLE
- [UUID Generator](https://www.uuidgenerator.net/)
- [ESP32 BLE Guide](https://randomnerdtutorials.com/esp32-bluetooth-low-energy-ble-arduino-ide/)
- [Change Characteristic](https://github.com/espressif/arduino-esp32/issues/1038)
- Indication & Notification [Link1](https://community.nxp.com/docs/DOC-328525) [Link2](https://www.onethesis.com/2015/11/21/ble-introduction-notify-or-indicate/)

Andriod App
- [Bluetooth Device](https://developer.android.com/reference/kotlin/android/bluetooth/package-summary)
- [Bluetooth GATT](https://developer.android.com/reference/android/bluetooth/BluetoothGatt)
- [BLE Basics](https://developer.android.com/guide/topics/connectivity/bluetooth-le)
- [GATT (Services and Characteristics)](https://www.oreilly.com/library/view/getting-started-with/9781491900550/ch04.html)
- [Remote adb](https://stackoverflow.com/questions/4893953/run-install-debug-android-applications-over-wi-fi)


## Remote adb debug


1. Connect the device via USB and make sure debugging is working;
2. `adb tcpip 5555` This makes the device to start listening for connections on port 5555;
3. Look up the device IP address with `adb shell netcfg` or `adb shell ifconfig` with 6.0 and higher;
4. Disconnect the USB now;
5. `adb connect <DEVICE_IP_ADDRESS>:5555`. This connects to the server we set up on the device on step 2;
6. Now you have a device over the network with which you can debug as usual.


- To switch the server back to the USB mode, run `adb usb`, which will put the server on your phone back to the USB mode. 
- If you have more than one device, you can specify the device with the -s option: `adb -s <DEVICE_IP_ADDRESS>:5555 usb`
- To find the IP address of the device: run `adb shell` and then `netcfg`. 
- To find the IP address while using OSX run the command adb shell ip route.