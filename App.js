import React from 'react';
import { StyleSheet, Text, View, AsyncStorage } from 'react-native';
import StreamC from './views/Stream'

export default class App extends React.Component {
  constructor() {
    super()

    this.state = {
      path: 'Stream'
    }
    this.router = path => {
      this.setState({ path })
    }
  }
  render() {
    let component;
    if (this.state.path == 'Stream') {
      component = StreamC;
    } else if (this.state.path == 'Settings') {

    } else {
      component = View;
    }
    return (
      <component router={this.router} />
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
