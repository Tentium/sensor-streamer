# Sensor Streamer (working title)

The intended use of this mobile application is to enable the user to stream the sensor data of their handheld mobile device over a network.
The application will mainly be using the TCP network protocol to achieve this.

## Screens

Main screen
- Stream
- Settings

Stream screen
- Choose what data to stream
- Choose network protocol
- Choose interval period
- IP and port

Settings screen
- Idk

## What it can do

Sensor data
- GPS
- Battery percentage

Network protocols
- TCP

## Implementations

TCP

Send a POST request with data formatted with JSON
```json
{
	gps: "*data*",
	timestamp: "2018-06-03T16:09:21.391Z"
}
```