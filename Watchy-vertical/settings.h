#ifndef SETTINGS_H
#define SETTINGS_H

//Weather Settings
#define CITY_ID "1234"
#define OPENWEATHERMAP_APIKEY "not actually used"
#define OPENWEATHERMAP_URL "not actually used"
#define EPAPER_SERVER "http://10.x.y.z:8080/watchy"
#define TEMP_UNIT "metric" //metric = Celsius , imperial = Fahrenheit
#define TEMP_LANG "en"
#define UPDATE_INTERVAL 30 //must be greater than 5, measured in minutes
//NTP Settings
#define NTP_SERVER "pool.ntp.org"
#define GMT_OFFSET_SEC 3600 * 2 // CEST: 1 in winter, 2 in summer

watchySettings settings{
    CITY_ID,
    OPENWEATHERMAP_APIKEY,
    OPENWEATHERMAP_URL,
    TEMP_UNIT,
    TEMP_LANG,
    UPDATE_INTERVAL,
    NTP_SERVER,
    GMT_OFFSET_SEC
};

#endif
