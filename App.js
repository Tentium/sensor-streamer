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
    let Cabarana;
    if (this.state.path == 'Stream') {
      Cabarana = StreamC;
    } else if (this.state.path == 'Settings') {

    } else {
      Cabarana = View;
    }
    return (
      <Cabarana router={this.router} />
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
