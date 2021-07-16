import 'dart:async';

import 'package:flutter/services.dart';

class FlutterNfc {
  static const MethodChannel _channel = const MethodChannel('flutter_nfc');

  static const EventChannel _eventChannel =
      const EventChannel('flutter_nfc/message');

  static Stream<dynamic>? _tagStream;

  static Stream<NFCMessage> startReading() {
    if (_tagStream == null) {
      _tagStream =
          _eventChannel.receiveBroadcastStream().map<NFCMessage>((tag) {
        return NFCMessage(tag["yId"] ?? "未读到NFC卡ID", tag["yMessage"] ?? "未读到信息",
            tag["yStatus"] ?? "未读到状态");
      });
    }
    StreamController<NFCMessage> controller = StreamController();
    final stream = _tagStream;
    final subscription = stream?.listen((message) {
      controller.add(message);
    }, onError: (error) {
      print(error);
    }, onDone: () {
      _tagStream = null;
       controller.close();
    });
    controller.onCancel = () {
      subscription?.cancel();
    };

    // 开始读取
    try {
      _channel.invokeMethod("startReading", {"status": "enable"});
    } catch (err) {
      controller.close();
      print(err);
    }
    return controller.stream;
  }

  static Future<bool> get isNFCSupported async {
    final supported = await _channel.invokeMethod("isNFCSupported");
    assert(supported is bool);
    return supported as bool;
  }
}

class NFCMessage {
  final String id;
  final String message;
  final String status;

  NFCMessage(this.id, this.message, this.status);
}
