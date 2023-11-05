#ifndef WATCHY_7_SEG_H
#define WATCHY_7_SEG_H

#include <Watchy.h>
#include "Seven_Segment10pt7b.h"
#include "DSEG7_Classic_Regular_15.h"
#include "DSEG7_Classic_Bold_25.h"
#include "DSEG7_Classic_Regular_39.h"
#include "Fonts/FreeMono9pt7b.h"
#include "Fonts/FreeMono12pt7b.h"
#include "Fonts/FreeSans24pt7b.h"
#include "icons.h"

typedef struct calendar_t {
  uint32_t dt;
  char text[10];
} calendar_t;

typedef struct weather_t {
  uint32_t dt;
  uint8_t rain;
  double temp;
} weather_t;

class Watchy7SEG : public Watchy{
    using Watchy::Watchy;
    public:
        void drawWatchFace();
        void loadData();
        void drawDate();
        void drawHours();
        void drawMinutes();
        void drawSteps();
        void drawWeather();
        void drawBattery();

        void slashedRectangle(int left, int top, int width, int height);
};

#endif
