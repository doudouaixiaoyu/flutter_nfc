package com.yu.flutter_nfc


import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Handler
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.nio.charset.Charset

class FlutterNfcPlugin(registrar: Registrar) : MethodCallHandler, EventChannel.StreamHandler, NfcAdapter.ReaderCallback {

    // 创建Activity
    private val activity = registrar.activity()

    // 创建NfcAdapter 对象，可为空
    private var nfcAdapter: NfcAdapter? = null

    // 创建EventChannel.EventSink 对象，可为空
    private var events: EventChannel.EventSink? = null

    private var status: String? = ""

    companion object {
        // 创建支持的卡的类型
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_BARCODE or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            // 注册通道
            val channel = MethodChannel(registrar.messenger(), "flutter_nfc")
            // 注册数据传输通道
            val eventChannel = EventChannel(registrar.messenger(), "flutter_nfc/message")
            // 获得类
            val plugin = FlutterNfcPlugin(registrar)
            // 设置通道的实现类
            channel.setMethodCallHandler(plugin)
            // 设置数据通道的实现类
            eventChannel.setStreamHandler(plugin)
        }
    }

    // 实现对应的函数，或执行相应的事件
    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isNFCSupported" -> {
                // 当为isNFCSupported的时候，执行isNFCSupported()并设置result.success
                result.success(isNFCSupported())
            }
            "startReading" -> {
                // 当为startReading的时候，开始执行startReading()函数
                status = call.argument("status") ?: "空"
                startReading()
            }
            else -> {
                // 没有找到，提示没有对应的方法
                result.notImplemented()
            }
        }
    }

    private fun isNFCSupported(): Boolean {
        // 得到默认的NfcAdapter
        val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
        // 若adapter 为空，则不执行isEnabled()，返回false
        return adapter?.isEnabled ?: false
    }

    private fun startReading() {
        // 得到默认NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        // 如果nfcAdapter 为Null,则返回
        if (nfcAdapter == null) {
            return
        }
        // 开启读卡模式
        nfcAdapter?.enableReaderMode(activity, this, READER_FLAGS, null)
    }


    override fun onTagDiscovered(p0: Tag?) {
        // 读取标签
        val nDef = Ndef.get(p0)
        // nDef 为空，则返回
        // 连接卡
        nDef?.connect()
        // 获得nDef的nDefMessage，如果为空，就获得cachedNDefMessage
        val nDEFMessage = nDef?.ndefMessage ?: nDef?.cachedNdefMessage
        // 把nDEFMessage转化为String 类型的message
        val message = nDEFMessage?.toByteArray()?.toString(Charset.forName("UTF-8")) ?: ""
        // 获得ID
        val id = bytesToHexString(p0?.id?.reversedArray()) ?: ""
        // 关闭卡
        nDef?.close()
        // map 化返回数据
        val data = mapOf("yId" to id, "yMessage" to message, "yStatus" to status)

        // 把数据存到events.success里返回
        // 开启读卡功能后可以读取多次卡
        val mainThread = Handler(activity.mainLooper)
        val runnable = Runnable {
            run {
                if (events != null) {
                    events?.success(data)
                }
            }
        }
        mainThread.post(runnable)
        // 开启读卡功能后只能读取一次卡
/*        val mainHandler = Handler(activity.mainLooper)
        mainHandler.post {
            events?.success(data)
            events = null
        }*/

    }

    private fun bytesToHexString(src: ByteArray?): String? {
        val stringBuilder = StringBuilder("0x")
        if (src == null || src.isEmpty()) {
            return null
        }

        val buffer = CharArray(2)
        for (i in src.indices) {
            buffer[0] = Character.forDigit(src[i].toInt().ushr(4).and(0x0F), 16)
            buffer[1] = Character.forDigit(src[i].toInt().and(0x0F), 16)
            stringBuilder.append(buffer)
        }

        return stringBuilder.toString()
    }

    override fun onListen(p0: Any?, p1: EventChannel.EventSink?) {
        events = p1
    }

    override fun onCancel(p0: Any?) {
        nfcAdapter?.disableReaderMode(activity)
        events = null
    }
}
