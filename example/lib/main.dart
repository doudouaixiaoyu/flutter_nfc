import 'dart:ffi';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter_nfc/flutter_nfc.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _supportsNFC = false;
  StreamSubscription<NFCMessage>? _stream;

  String nfcId = "空";

  @override
  void initState() {
    super.initState();
    FlutterNfc.isNFCSupported.then((supported) {
      setState(() {
        _supportsNFC = supported;
      });
    });
  }

  void _readNFC(BuildContext context) {
    try {
      // ignore: cancel_subscriptions
      StreamSubscription<NFCMessage> subscription =
          FlutterNfc.startReading().listen((tag) {
        setState(() {
          nfcId = int.parse(tag.id).toRadixString(10);
        });
      }, onDone: () {
        setState(() {
          _stream = null;
        });
      }, onError: (e) {
        setState(() {
          _stream = null;
        });
        print(e);
      });
      setState(() {
        _stream = subscription;
      });
    } catch (err) {
      print("error: $err");
    }
  }

  void _stopReading() {
    _stream?.cancel();
    setState(() {
      _stream = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              _supportsNFC ? Text('点击去读NFC') : Text('NFC未开启'),
              FlatButton(
                child: Text(_stream == null ? "Start reading" : "Stop reading"),
                onPressed: () {
                  if (_stream == null) {
                    _readNFC(context);
                  } else {
                    _stopReading();
                  }
                },
              ),
              Text(nfcId),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _stream?.cancel();
    super.dispose();
  }
}
