import { NativeModules } from 'react-native'
import axios from 'axios'

const { BatteryManager } = NativeModules

let timerId;

module.exports = async config => {
	let { dataToSend, host, interval } = config

	if (timerId) clearInterval(timerId)

	let timerId = setInterval(() => {
		send(config)
	}, interval)
}


function send({ dataToSend, host }) {
	let gpsPromise = new Promise((resolve, reject) => {
		if (!dataToSend.gps) resolve(undefined)

		navigator.geolocation.getCurrentPosition(data => {
			resolve({
				long: data.coords.longitude,
				lat: data.coords.latitude,
				alt: data.coords.altitude
			})
		}, () => {
			resolve({})
		}, {
				enableHighAccuracy: true,
			})
	})
	let batteryPromise = new Promise((resolve, reject) => {
		if (!dataToSend.battery) resolve(undefined)

		BatteryManager.updateBatteryLevel(info => {
			resolve({ level: info.level, charging: info.isPlugged })
		})
	})
	Promise.all([gpsPromise, batteryPromise]).then(([gps, battery]) => {
		axios.post(`http://${host}`, {
			gps,
			battery,
			timestamp: Date.now()
		})
	})
}