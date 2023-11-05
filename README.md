# ePaper goodies

This repository contains an application to render widgets on an ePaper
display, gathering information from various data sources such as:

* The current date / time, both Gregorian and Chinese calendars
* CalDAV or Google calendars
* Weather forecast
* Public transport schedules
* RSS feeds
* HTTP endpoints serving raw text
* Local file system (images and text)
* and more!

The widgets to render are parametrised in a simple text file specifying
coordinates of widgets and data sources. See the [list of available widgets](docs/widgets.md).

The application does not interact with the ePaper display directly.
It will instead produce an image file which you should then pass to a
suitable script to show on the ePaper display.

## Server Mode

If you are a lucky owner of a [Watchy smartwatch](https://watchy.sqfmi.com/),
the application can also act as a middleware serving JSON data over
HTTP, which you can then render on the watch.
