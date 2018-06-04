import React from 'react'
import { StyleSheet, Text, View, AsyncStorage, Button, TextInput, Picker, NativeModules } from 'react-native'
import BackgroundTimer from 'react-native-background-timer'
import CheckBox from 'react-native-check-box'
import axios from 'axios'
import update from 'immutability-helper'

const { BatteryManager } = NativeModules

export default class Stream extends React.Component {
	constructor() {
		super()

		this.state = {
			interval: 1000,
			host: "",
			streaming: false,
			data: {
				gps: true,
				battery: false
			}
		}
		AsyncStorage.getItem("host").then(item => {
			item && this.setState({ host: item })
		})
		AsyncStorage.getItem("sendData").then(item => {
			item && this.setState({ data: JSON.parse(item) })
		})
	}
	changeHost = newHost => {
		this.setState({ host: newHost })
		AsyncStorage.setItem("host", newHost)
	}
	updateSendData = name => {
		let newData = update(this.state.data, { [name]: { $set: !this.state.data[name] } })
		this.setState({ data: newData })
		AsyncStorage.setItem("sendData", JSON.stringify(newData)).catch(err => {
			console.error(err)
		})
	}
	render() {
		return (
			<View style={{ margin: 5 }}>
				<Text style={{ fontSize: 36, color: "#333" }}>Stream</Text>
				<View style={{ display: "flex", flexDirection: "row", alignItems: "center" }}>
					<Text style={{ marginRight: 5, flexGrow: 0 }}>http://</Text>
					<TextInput style={{ flexGrow: 1 }} placeholder="192.168.0.1:5555" value={this.state.host} onChangeText={this.changeHost} />
				</View>
				<View style={{ display: "flex", flexDirection: "row", alignItems: "center" }}>
					<Text style={{ marginRight: 5, flexGrow: 0 }}>Interval</Text>
					<Picker
						style={{ flexGrow: 1 }}
						selectedValue={this.state.interval}
						onValueChange={(value) => this.setState({ interval: value })}
					>
						<Picker.Item label="10 s" value={10000} />
						<Picker.Item label="1 s" value={1000} />
						<Picker.Item label="200 ms" value={200} />
					</Picker>
				</View>

				<View style={{ display: "flex", flexWrap: "wrap", flexDirection: "row" }}>
					<CheckBox
						style={{ padding: 10, flexBasis: "50%" }}
						leftTextStyle={{ color: "#111" }}
						checkBoxColor="#111"
						leftText="GPS"
						isChecked={this.state.data.gps}
						onClick={() => this.updateSendData("gps")}
					/>
					<CheckBox
						style={{ padding: 10, flexBasis: "50%" }}
						leftText="Battery"
						leftTextStyle={{ color: "#111" }}
						checkBoxColor="#111"
						isChecked={this.state.data.battery}
						onClick={() => this.updateSendData("battery")}
					// leftTextStyle={{ color: "#999" }}
					// checkBoxColor="#999"
					// disabled={true}
					/>
				</View>
				<Button color="#FFB300" onPress={this.state.streaming ? this.stopStream : this.startStream} title={this.state.streaming ? "Stop streaming" : "Start streaming"}></Button>
			</View>
		)
	}
	startStream = () => {
		let interval = this.state.interval
		let component = this
		let host = this.state.host || "192.168.0.1:5555"
		let dataToSend = Object.assign({}, this.state.data)
		this.setState({ streaming: true })

		BackgroundTimer.stopBackgroundTimer()

		BackgroundTimer.runBackgroundTimer(() => {
			let gpsPromise = new Promise((resolve, reject) => {
				if (!dataToSend.gps) resolve(undefined)

				navigator.geolocation.getCurrentPosition(data => {
					resolve({
						long: data.coords.longitude,
						lat: data.coords.latitude,
						alt: data.coords.altitude
					})
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
				}, {

					})
			})
		}, interval)
	}
	stopStream = () => {
		BackgroundTimer.stopBackgroundTimer()
		this.setState({ streaming: false })
	}
}