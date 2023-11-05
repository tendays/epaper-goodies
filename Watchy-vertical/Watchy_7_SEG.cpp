#include "Watchy_7_SEG.h"

#define DARKMODE true

#define TEMPERATURE_SCALE 2
#define WEATHER_BUFFER 100

const uint8_t BATTERY_SEGMENT_WIDTH = 7;
const uint8_t BATTERY_SEGMENT_HEIGHT = 11;
const uint8_t BATTERY_SEGMENT_SPACING = 9;
const uint8_t WEATHER_ICON_WIDTH = 48;
const uint8_t WEATHER_ICON_HEIGHT = 32;

RTC_DATA_ATTR calendar_t calendar[40];
RTC_DATA_ATTR weather_t weather[WEATHER_BUFFER];
RTC_DATA_ATTR int loadIntervalCounter = -1;
RTC_DATA_ATTR bool loaded = false;

void Watchy7SEG::drawWatchFace() {
    loadData();
    display.fillScreen(DARKMODE ? GxEPD_BLACK : GxEPD_WHITE);
    display.setTextColor(DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    drawHours(); // 0-80
    drawMinutes(); // 82-145
    drawDate(); // 150
    //drawSteps();
    drawBattery();
    // 160-186
    display.drawBitmap(164, 77, WIFI_CONFIGURED ? wifi : wifioff, 26, 18, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    if(BLE_CONFIGURED){
        display.drawBitmap(164, 50, bluetooth, 13, 21, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    }
}

void Watchy7SEG::loadData() {
  if (loadIntervalCounter < 0) { //-1 on first run, set to updateInterval
    loadIntervalCounter = settings.weatherUpdateInterval;
  }
  if (loadIntervalCounter >=
      settings.weatherUpdateInterval) { // only update if UPDATE_INTERVAL has elapsed
                        // i.e. 30 minutes
    Serial.println("updating data");
    if (connectWiFi()) {
      Serial.println("wifi OK");
      HTTPClient http; // Use Weather API for live data if WiFi is connected
      http.setConnectTimeout(3000);
      http.setTimeout(10000);
      http.begin(EPAPER_SERVER);
      int httpResponseCode = http.GET();
      if (httpResponseCode == 200) {
        Serial.print("got data ");
        
        String payload             = http.getString();
        JSONVar responseObject     = JSON.parse(payload);
        /* Load calendar data */
        for (int n = 0; n < 40; n++) {
          if (n >= responseObject["calendar"].length()) {
            calendar[n].dt = 0;
            continue;
          }
          calendar[n].dt = long(responseObject["calendar"][n]["time"]);
          strncpy(calendar[n].text, responseObject["calendar"][n]["text"], 9);
          calendar[n].text[9] = '\0'; // make sure it is null-terminated
        }
        Serial.println("parsed calendar data");
        
        /* Load weather data */
        for (int n = 0; n < WEATHER_BUFFER; n++) {
          if (n >= responseObject["weather"].length()) {
            weather[n].dt = 0;
            weather[n].rain = 0;
            weather[n].temp = 0;
            continue;
          }
          //Serial.print("Reading data point ");
          //Serial.println(responseObject["list"][n]["dt"]);
          weather[n].dt = long(responseObject["weather"][n]["time"]);
          double rain = double(responseObject["weather"][n]["precipitation"]);
          weather[n].rain = (int)(rain * 10);
          /*Serial.print("Rain at ");
          Serial.print(weather[n].dt);
          Serial.print(" is ");
          Serial.print(rain);
          Serial.print(" rounded to ");
          Serial.println(weather[n].rain);*/
          weather[n].temp = (int) (double(responseObject["weather"][n]["temperature"]));
          Serial.print("Temp ");
          Serial.println(weather[n].temp);
        }
        Serial.println("parsed weather data");
        loaded = true;
      } else { // HTTP error
        Serial.print("Server error ");
        Serial.print(httpResponseCode);
        Serial.println();
      }
      
      http.end();
      // turn off radios
      WiFi.mode(WIFI_OFF);
      btStop();

    } else { // offline
      Serial.println("no wifi");
    }
    loadIntervalCounter = 0;
  } else {
    loadIntervalCounter++;
    Serial.println("Not ready to update yet");
  }
}

void Watchy7SEG::drawDate() {
  display.setCursor(149, 50);
  display.setFont(&FreeSans24pt7b);
  display.print(currentTime.Day);
}

void Watchy7SEG::drawHours() {
  display.setFont(&FreeMono9pt7b);
  // Let the display height correspond to 24 hours, so each hour is 25/3 pixels.
  // This is the vertical coordinate of the current day's start.
  int dayStart = - (currentTime.Hour * 200 / 24 + currentTime.Minute / 6);

  // Print current hour, and following 19 ones
  for (int h = 0; h < 48; h += 3) {
    int toPrint = h % 24;
    int y = dayStart + h*200/24;
    if (toPrint == 0) {
      display.drawFastHLine(2, y, 80, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
      // temperature points
      for (int x = -10; x <= 30; x += 10) {
        int tickSize = (x == 0) ? 4 : 2;
        display.drawFastVLine((x + 10) * TEMPERATURE_SCALE, y - tickSize, tickSize*2+1, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
      }
    } else {
      display.setCursor(2, y+6);
      if (toPrint < 10) {
        display.print("0");
      }
      display.print(toPrint);
    }
    display.drawFastHLine(75, y, 5, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    display.drawFastHLine(78, dayStart + (h+1)*200/24, 2, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    display.drawFastHLine(78, dayStart + (h+2)*200/24, 2, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
  }

  if (!loaded) { return; }

  /* Print calendar data. currentSeconds: from UTC epoch */
  const int32_t currentSeconds = makeTime(currentTime) - settings.gmtOffset;
  // find the current slot in calendar
  int offset = 0;
  Serial.print("Current time ");
  Serial.println(currentSeconds);
  Serial.print("Forecast time ");
  Serial.println(weather[0].dt);
  // show events up to an hour in the past because we may see the bottom of the text
  while (offset < 40 && calendar[offset].dt < currentSeconds - 3600) {
    offset++;
  }
  Serial.print("Starting with offset ");
  Serial.println(offset);
  while (offset < 40 && calendar[offset].dt) {
    Serial.print("Event ");
    Serial.print(offset);
    Serial.print(" at ");
    Serial.print(calendar[offset].dt);
    Serial.print(":");
    Serial.print(calendar[offset].text);
    Serial.print(" to ");
    int32_t lhs = calendar[offset].dt;
    int32_t delta = (lhs - currentSeconds);
    // int y = delta * 200 / 24 / 3600;
    int y = delta / 432;
    // int y = delta / 360;
    Serial.print(y);
    Serial.println();
    
    display.setCursor(40, y + 6);
    display.print(calendar[offset].text);
    offset++;

    if (y > 210) { break; }
  }

  /* Print temperature data */
  offset = 0;
  while (offset < WEATHER_BUFFER && weather[offset].dt < currentSeconds) {
    offset++;
  }
  Serial.print("Rendering weather data from offset ");
  Serial.print(offset);
  Serial.println();
  // start from the last entry before now
  if (offset > 0) { offset--; }
  int px=999; // 'uninitialised' marker
  int py=0;
  while (offset < WEATHER_BUFFER && weather[offset].dt && py < 200) {
    double shiftedTemperature = (weather[offset].temp + 10);
    int x = shiftedTemperature * TEMPERATURE_SCALE;
    Serial.print("Temperature ");
    Serial.print(weather[offset].temp);
    Serial.print(" shifted to ");
    Serial.print(shiftedTemperature);
    Serial.print(" scaled to ");
    Serial.println(x);

    int32_t lhs = weather[offset].dt;
    int32_t delta = lhs - currentSeconds;
    /*Serial.print(lhs);
    Serial.print(" - ");
    Serial.print(currentSeconds);
    Serial.print(" = ");
    Serial.print(delta);
    Serial.println();*/
    
    int y = delta / 432; // delta/360;
    /*Serial.print(y);
    Serial.println();
    
    Serial.print("Temperature plot point (dt=");
    Serial.print(weather[offset].dt);
    Serial.print("): ");
    
    Serial.print(x);
    Serial.print(" ");
    Serial.print(y);
    Serial.println();
    */
    // Temperature curve
    if (px != 999) {
      display.drawLine(px, py, x, y, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    }

    // Rain bars
    if (weather[offset].rain > 0) {
      /* Fill slashed rectangle: x from 60 to 70 + weather[offset].rain * 9;
       *  y from y to y+9, print diagonal lines for which (x-y) is a multiple of 4
       */

       slashedRectangle(60, y, weather[offset].rain * 9, (delta+3600) / 432 - y);
    }
    
    px = x;
    py = y;
    offset++;
  }
}

void Watchy7SEG::slashedRectangle(int left, int top, int width, int height) {
      /* 1. Lines starting from left edge. */
      int y = top + ((left - top) % 4);
      if (y < top) { y += 4; }
      while (y <= top + height) {
        int lineLen = top + height - y;
        if (lineLen > width) {
          lineLen = width;
        }
        
        display.drawLine(left, y, left+lineLen, y+lineLen, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);

        y += 4;
      }
      
      /* 2. Lines starting from top edge. */
      int x = left + ((top - left) % 4);
      if (x < left) { x += 4; }
      while (x <= left + width) {
        int lineLen = left + width - x;
        if (lineLen > height) {
          lineLen = height;
        }

        display.drawLine(x, top, x+lineLen, top+lineLen, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);

        x += 4;
      }
}

void Watchy7SEG::drawMinutes() {
  
  display.setFont(&FreeMono12pt7b);
  // Let the display height correspond to 60 minutes, so each quarter is 25 pixels.

  // This is the vertical coordinate of the current hour's start.
  int hourStart = - (currentTime.Minute *200 /60);

  // Print two hours (starting from beginning of current one) to make sure we see all
  for (int m = 0; m < 120; m += 15) {
    int toPrint = m % 60;
    int y = hourStart + m*200/60;
    if (toPrint == 0) {
      display.drawFastHLine(86, y, 60, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    } else {
      display.setCursor(82, y+6);
      if (toPrint < 10) {
        display.print("0");
      }
      display.print(toPrint);
    }

    display.drawFastHLine(140, y, 5, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    display.drawFastHLine(143, y+17, 2, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    display.drawFastHLine(143, y+33, 2, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
  }
}
/*
void Watchy7SEG::drawDate(){
    display.setFont(&Seven_Segment10pt7b);

    int16_t  x1, y1;
    uint16_t w, h;

    String dayOfWeek = dayStr(currentTime.Wday);
    display.getTextBounds(dayOfWeek, 5, 85, &x1, &y1, &w, &h);
    if(currentTime.Wday == 4){
        w = w - 5;
    }
    display.setCursor(85 - w, 85);
    display.println(dayOfWeek);

    String month = monthShortStr(currentTime.Month);
    display.getTextBounds(month, 60, 110, &x1, &y1, &w, &h);
    display.setCursor(85 - w, 110);
    display.println(month);

    display.setFont(&DSEG7_Classic_Bold_25);
    display.setCursor(5, 120);
    if(currentTime.Day < 10){
    display.print("0");
    }
    display.println(currentTime.Day);
    display.setCursor(5, 150);
    display.println(tmYearToCalendar(currentTime.Year));// offset from 1970, since year is stored in uint8_t
}
*/
void Watchy7SEG::drawSteps(){
    // reset step counter at midnight
    if (currentTime.Hour == 0 && currentTime.Minute == 0){
      sensor.resetStepCounter();
    }
    uint32_t stepCount = sensor.getCounter();
    display.drawBitmap(10, 165, steps, 19, 23, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    display.setCursor(35, 190);
    display.println(stepCount);
}

void Watchy7SEG::drawBattery(){
    display.drawBitmap(160, 103, battery, 37, 21, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    display.fillRect(165, 108, 27, BATTERY_SEGMENT_HEIGHT, DARKMODE ? GxEPD_BLACK : GxEPD_WHITE);//clear battery segments
    int8_t batteryLevel = 0;
    float VBAT = getBatteryVoltage();
    if(VBAT > 4.1){
        batteryLevel = 3;
    }
    else if(VBAT > 3.95 && VBAT <= 4.1){
        batteryLevel = 2;
    }
    else if(VBAT > 3.80 && VBAT <= 3.95){
        batteryLevel = 1;
    }
    else if(VBAT <= 3.80){
        batteryLevel = 0;
    }

    for(int8_t batterySegments = 0; batterySegments < batteryLevel; batterySegments++){
        display.fillRect(165 + (batterySegments * BATTERY_SEGMENT_SPACING), 108, BATTERY_SEGMENT_WIDTH, BATTERY_SEGMENT_HEIGHT, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    }
}
/*
void Watchy7SEG::drawWeather(){
  
  Serial.print("Offset ");
  Serial.println(offset);
  display.drawFastHLine(149, 150, 32, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
  for (int x = 0; x < 8 && offset+x < 40; x++) {
    display.drawFastVLine(149 + x*4, 150 - rain_forecast[offset+x].rain, rain_forecast[offset+x].rain,
    DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    if (offset+x < 39) {
      display.drawLine(
        149 + x*4, 150 - rain_forecast[offset+x].temp,
        151 + x*4, 150 - rain_forecast[offset+x+1].temp,
        DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    }
  }
  
    / *weatherData currentWeather = getWeatherData();

    int8_t temperature = currentWeather.temperature;
    int16_t weatherConditionCode = currentWeather.weatherConditionCode;

    display.setFont(&DSEG7_Classic_Regular_39);
    int16_t  x1, y1;
    uint16_t w, h;
    display.getTextBounds(String(temperature), 0, 0, &x1, &y1, &w, &h);
    if(159 - w - x1 > 87){
        display.setCursor(159 - w - x1, 150);
    }else{
        display.setFont(&DSEG7_Classic_Bold_25);
        display.getTextBounds(String(temperature), 0, 0, &x1, &y1, &w, &h);
        display.setCursor(159 - w - x1, 136);
    }
    display.println(temperature);
    display.drawBitmap(165, 110, currentWeather.isMetric ? celsius : fahrenheit, 26, 20, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    const unsigned char* weatherIcon;

    //https://openweathermap.org/weather-conditions
    if(weatherConditionCode > 801){//Cloudy
    weatherIcon = cloudy;
    }else if(weatherConditionCode == 801){//Few Clouds
    weatherIcon = cloudsun;
    }else if(weatherConditionCode == 800){//Clear
    weatherIcon = sunny;
    }else if(weatherConditionCode >=700){//Atmosphere
    weatherIcon = atmosphere;
    }else if(weatherConditionCode >=600){//Snow
    weatherIcon = snow;
    }else if(weatherConditionCode >=500){//Rain
    weatherIcon = rain;
    }else if(weatherConditionCode >=300){//Drizzle
    weatherIcon = drizzle;
    }else if(weatherConditionCode >=200){//Thunderstorm
    weatherIcon = thunderstorm;
    }else
    return;
    display.drawBitmap(145, 158, weatherIcon, WEATHER_ICON_WIDTH, WEATHER_ICON_HEIGHT, DARKMODE ? GxEPD_WHITE : GxEPD_BLACK);
    * /
}*/
