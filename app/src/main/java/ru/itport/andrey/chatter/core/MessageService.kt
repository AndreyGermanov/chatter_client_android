package ru.itport.andrey.chatter.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import kotlinx.coroutines.experimental.*
import org.json.JSONArray
import org.json.JSONObject

public class MessageService : Service() {

    lateinit var ws: WebSocket

    init {
        launch {
            this@MessageService.ws = WebSocketFactory().createSocket("ws://192.168.0.184:8080/websocket", 5000)
            this@MessageService.ws.addListener(MessageAdapter())
            this@MessageService.ws.connect()
        }
    }

    private final var mBinder: IBinder = ServiceBinder()

    inner class ServiceBinder: Binder() {

        fun getService(): MessageService {
           return this@MessageService
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    public fun sendMessage(messageObject:Map<String,String>) {
        var json = JSONObject()
        var indexes = messageObject.keys
        for (index in indexes) {
            json.put(index,messageObject.get(index)!!)
        }
        launch {
            this@MessageService.ws.sendText(json.toString())
        }
    }
}

class MessageAdapter: WebSocketAdapter() {
    override fun onTextMessage(webSocket:WebSocket,message:String) {
        println(message)
    }
}
