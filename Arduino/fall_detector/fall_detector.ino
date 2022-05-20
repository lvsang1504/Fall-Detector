#include <Wire.h>
#include <FirebaseArduino.h>
#include <ESP8266WiFi.h>
#include <NTPClient.h>
#include <WiFiUdp.h>

#include <ArduinoJson.h>
#include "ESP8266WiFi.h"
#include <ESP8266HTTPClient.h>

//for get current time
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

const int MPU_addr = 0x68; // 0110 1000 I2C address of the MPU-6050
int16_t AcX, AcY, AcZ, Tmp, GyX, GyY, GyZ;
float ax = 0, ay = 0, az = 0, gx = 0, gy = 0, gz = 0;
boolean fall = false; //stores if a fall has occurred
boolean trigger1 = false; //stores if first trigger (lower threshold) has occurred
boolean trigger2 = false; //stores if second trigger (upper threshold) has occurred
boolean trigger3 = false; //stores if third trigger (orientation change) has occurred
byte trigger1count = 0; //stores the counts past since trigger 1 was set true
byte trigger2count = 0; //stores the counts past since trigger 2 was set true
byte trigger3count = 0; //stores the counts past since trigger 3 was set true
int angleChange = 0;
// Set up firebase.
#define FIREBASE_HOST "fall-detector-55495-default-rtdb.firebaseio.com" 

#define FIREBASE_AUTH "2PLr7evoGRknFJe6yQbo651kMjpW84gX3WDpmxyo"

//Credentials for Google GeoLocation API...
const char* Host = "www.googleapis.com";

String thisPage = "/geolocation/v1/geolocate?key=";

String key = "AIzaSyBcDWqaGwpJyz57hwd_9fyRp9fDy2TxBVc";

int status = WL_IDLE_STATUS;

String jsonString = "{\n";

double latitude    = 0.0;

double longitude   = 0.0;

double accuracy    = 0.0;

int more_text = 1;    // set to 1 for more debug output

//Wifi
#define WIFI_SSID "there_is_no_one_at_all"
#define WIFI_PASSWORD "123456788"




void setup() {
  Serial.begin(115200);

  WiFi.mode(WIFI_STA);
  
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  timeClient.begin();
  timeClient.setTimeOffset(25200); //GMT -1 = -3600 // GMT 0 = 0

  Wire.begin();  // Khởi tạo thư viện i2c
  Wire.beginTransmission(MPU_addr); // Bắt đầu truyền dữ liệu về địa chỉ số MPU_addr
  Wire.write(0x6B);  // PWR_MGMT_1 register
  Wire.write(0);     // set to zero (wakes up the MPU-6050)
  Wire.endTransmission(true);// Kết thúc truyền dữ liệu
  Serial.println("Wrote to IMU");
  Serial.println("Connecting to ");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");              // print … till not connected
  }
  Serial.println("");
  Serial.println("WiFi connected" + WiFi.localIP());

}


void loop() {
  mpu_read();
  ax = (AcX - 2050) / 16384.00;
  ay = (AcY - 77) / 16384.00;
  az = (AcZ - 1947) / 16384.00;
  gx = (GyX + 270) / 131.07;
  gy = (GyY - 351) / 131.07;
  gz = (GyZ + 136) / 131.07;
  // calculating Amplitute vactor for 3 axis
  float Raw_Amp = pow(pow(ax, 2) + pow(ay, 2) + pow(az, 2), 0.5); //Tính vectơ biên độ của các giá trị gia tốc kế.
  int Amp = Raw_Amp * 10;  // Mulitiplied by 10 bcz values are between 0 to 1
  Serial.println(Amp);
  if (Amp <= 2 && trigger2 == false) { //if AM breaks lower threshold (0.4g)
    trigger1 = true;
    Serial.println("TRIGGER 1 ACTIVATED");
  }
  if (trigger1 == true) {
    trigger1count++;
    if (Amp >= 12) { //if AM breaks upper threshold (3g)
      trigger2 = true;
      Serial.println("TRIGGER 2 ACTIVATED");
      trigger1 = false; trigger1count = 0;
    }
  }
  if (trigger2 == true) {
    trigger2count++;
    angleChange = pow(pow(gx, 2) + pow(gy, 2) + pow(gz, 2), 0.5); Serial.println(angleChange);
    if (angleChange >= 30 && angleChange <= 400) { //if orientation changes by between 80-100 degrees
      trigger3 = true; trigger2 = false; trigger2count = 0;
      Serial.println(angleChange);
      Serial.println("TRIGGER 3 ACTIVATED");
    }
  }
  if (trigger3 == true) {
    trigger3count++;
    if (trigger3count >= 10) {
      angleChange = pow(pow(gx, 2) + pow(gy, 2) + pow(gz, 2), 0.5);
      //delay(10);
      Serial.println(angleChange);
      if ((angleChange >= 0) && (angleChange <= 10)) { //if orientation changes remains between 0-10 degrees
        fall = true; trigger3 = false; trigger3count = 0;
        Serial.println(angleChange);
      }
      else { //user regained normal orientation
        trigger3 = false; trigger3count = 0;
        Serial.println("TRIGGER 3 DEACTIVATED");
      }
    }
  }
  if (fall == true) { //in event of a fall detection
    Serial.println("FALL DETECTED");

    get_information();

    fall = false;
  }
  if (trigger2count >= 6) { //allow 0.5s for orientation change
    trigger2 = false; trigger2count = 0;
    Serial.println("TRIGGER 2 DECACTIVATED");
  }
  if (trigger1count >= 6) { //allow 0.5s for AM to break upper threshold
    trigger1 = false; trigger1count = 0;
    Serial.println("TRIGGER 1 DECACTIVATED");
  }
  delay(100);
}




void get_information(){
  
  char bssid[6];

  DynamicJsonBuffer jsonBuffer;

  Serial.println("scan start");
  
  // WiFi.scanNetworks will return the number of networks found

  int n = WiFi.scanNetworks();

  Serial.println("scan done");

  if (n == 0)

    Serial.println("no networks found");

  else

  {

    Serial.print(n);

    Serial.println(" networks found...");


    if (more_text) {

      Serial.println("\"wifiAccessPoints\": [");

      for (int i = 0; i < n; ++i)

      {

        Serial.println("{");

        Serial.print("\"macAddress\" : \"");

        Serial.print(WiFi.BSSIDstr(i));

        Serial.println("\",");

        Serial.print("\"signalStrength\": ");

        Serial.println(WiFi.RSSI(i));

        if (i < n - 1)

        {

          Serial.println("},");

        }

        else

        {

          Serial.println("}");

        }

      }

      Serial.println("]");

      Serial.println("}");

    }

    Serial.println(" ");

  }


  // now build the jsonString...

  jsonString = "{\n";

  jsonString += "\"homeMobileCountryCode\": 234,\n"; // this is a real UK MCC

  jsonString += "\"homeMobileNetworkCode\": 27,\n";  // and a real UK MNC

  jsonString += "\"radioType\": \"gsm\",\n";         // for gsm

  jsonString += "\"carrier\": \"Vodafone\",\n";      // associated with Vodafone

  jsonString += "\"wifiAccessPoints\": [\n";

  for (int j = 0; j < n; ++j)

  {

    jsonString += "{\n";

    jsonString += "\"macAddress\" : \"";

    jsonString += (WiFi.BSSIDstr(j));

    jsonString += "\",\n";

    jsonString += "\"signalStrength\": ";

    jsonString += WiFi.RSSI(j);

    jsonString += "\n";

    if (j < n - 1)

    {

      jsonString += "},\n";

    }

    else

    {

      jsonString += "}\n";

    }

  }

  jsonString += ("]\n");

  jsonString += ("}\n");


  //-------------------------------------------------------------------- Serial.println("");


  //Connect to the client and make the api call


  WiFiClientSecure client;

  client.setInsecure();

  Serial.print("Requesting URL: ");

  Serial.println("https://" + (String)Host + thisPage + key);

  delay(500);

  Serial.println(" ");

  if (client.connect(Host, 443)) {

    Serial.println("Connected");

    client.println("POST " + thisPage + key + " HTTP/1.1");

    client.println("Host: " + (String)Host);

    client.println("Connection: close");

    client.println("Content-Type: application/json");

    client.println("User-Agent: Arduino/1.0");

    client.print("Content-Length: ");

    client.println(jsonString.length());

    client.println();

    client.print(jsonString);

    delay(2000);

  }


  //Read and parse all the lines of the reply from server

  Serial.print(client.available());
  Serial.print("client");

  while (client.available()) {

    String line = client.readStringUntil('\r');

    if (more_text) {

      Serial.print(line);

    }

    JsonObject& root = jsonBuffer.parseObject(line);

    Serial.print(root.success());

    if (root.success()) {

      latitude    = root["location"]["lat"];

      longitude   = root["location"]["lng"];

      accuracy   = root["accuracy"];

    }

  }


  Serial.println("closing connection");

  Serial.println();

  client.stop();

  


  Serial.print("Latitude = ");

  Serial.println(latitude, 6);

  Serial.print("Longitude = ");

  Serial.println(longitude, 6);

  Serial.print("Accuracy = ");

  Serial.println(accuracy);

  send_data(longitude, latitude);

}




void send_data(double longitude ,double  latitude) {
  
  timeClient.update();

  Serial.println(timeClient.getFormattedTime());
  unsigned long epochTime = timeClient.getEpochTime();
  struct tm *ptm = gmtime ((time_t *)&epochTime);
  int monthDay = ptm->tm_mday;
  int currentMonth = ptm->tm_mon+1;
  int currentYear = ptm->tm_year+1900;
  String currentDate = String(currentYear) + "-" + String(currentMonth) + "-" + String(monthDay) + " " + timeClient.getFormattedTime() ;
  String key = "/FallDetector/DetectAt" + String(timeClient.getEpochTime()) + "/";
  Firebase.setString(key + "time",currentDate );
  Firebase.setString(key + "longitude",String(longitude,6));
  Firebase.setString(key + "latitude",String(latitude,6));
  if (Firebase.failed())
  {
    Serial.print("pushing /logs failed:");
    Serial.println(Firebase.error());
    delay(500);
    return;
  }
}


void mpu_read() {
  Wire.beginTransmission(MPU_addr);
  Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_addr, 14, true); // request a total of 14 registers
  AcX = Wire.read() << 8 | Wire.read(); // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)
  AcY = Wire.read() << 8 | Wire.read(); // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
  AcZ = Wire.read() << 8 | Wire.read(); // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
  Tmp = Wire.read() << 8 | Wire.read(); // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
  GyX = Wire.read() << 8 | Wire.read(); // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
  GyY = Wire.read() << 8 | Wire.read(); // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
  GyZ = Wire.read() << 8 | Wire.read(); // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
}
