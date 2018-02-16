package ru.itport.andrey.chatter.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketState
import kotlinx.coroutines.experimental.*
import org.json.JSONArray
import org.json.JSONObject
import ru.itport.andrey.chatter.models.User
import java.util.*
import java.util.zip.Adler32
import kotlin.collections.HashMap

public interface WebSocketResponseHandler {
    fun handleResponse(response:JSONObject)
}


public class MessageService : Service() {

    lateinit var ws: WebSocket
    val sentRequests: HashMap<String,WebSocketResponseHandler> = HashMap<String,WebSocketResponseHandler>()
    val requestsQueue: HashMap<String,Any> = HashMap<String,Any>()
    var requestsTimer : Timer = Timer()
    val pending_files = HashMap<Long,JSONObject>()

    init {
        runBlocking {
            async {
                this@MessageService.ws = WebSocketFactory().createSocket("ws://192.168.0.184:8080/websocket", 5000)
                this@MessageService.ws.addListener(MessageAdapter())
                if (this@MessageService.ws.state == WebSocketState.CREATED) {
                    try {
                        this@MessageService.ws.connect()
                    } catch (e: Exception) {
                    }
                }
            }.await()
            requestsTimer.schedule(DataExchangeSession(),0,1)
        }
    }

    private final var mBinder: IBinder = ServiceBinder()

    inner class ServiceBinder: Binder() {

        fun getService(): MessageService {
           return this@MessageService
        }

    }

    inner class DataExchangeSession: TimerTask() {
        override fun run() {
            if (this@MessageService.ws.isOpen()) {
                val it = this@MessageService.requestsQueue.iterator()
                if (it.hasNext()) {
                    val request = it.next().value as HashMap<String,Any>
                    this@MessageService.sentRequests.put(request.get("request_id").toString(),request.get("sender") as WebSocketResponseHandler)
                    this@MessageService.sendMessage(request)
                    this@MessageService.requestsQueue.remove(request.get("request_id")!!)
                } else {
                    if (User.id.length>0 && User.isLogin) {
                        val request = HashMap<String,Any>()
                        request.put("user_id",User.id)
                        request.put("action","poll")
                        this@MessageService.sendMessage(request)
                    }
                }
            } else {
                if (this@MessageService.ws.state == WebSocketState.CLOSED || this@MessageService.ws.state == WebSocketState.CLOSING) {
                    this@MessageService.ws = WebSocketFactory().createSocket("ws://192.168.0.184:8080/websocket", 5000)
                }
                try {
                    this@MessageService.ws.connect()
                } catch (e:Exception) {
                }
            }
        }
    }

    inner class MessageAdapter: WebSocketAdapter() {
        override fun onTextMessage(webSocket:WebSocket,message:String) {
            var responseObject = JSONObject(message)
            if (responseObject.has("request_id")) {
                if (this@MessageService.sentRequests.containsKey(responseObject.getString("request_id"))) {
                    if (!responseObject.has("checksum")) {
                        this@MessageService.sentRequests.get(responseObject.getString("request_id"))!!.handleResponse(responseObject)
                        this@MessageService.sentRequests.remove(responseObject.getString("request_id"))
                    } else {
                        this@MessageService.pending_files.set(responseObject.getString("checksum").toLong(),responseObject)
                    }
                }
            }
        }

        override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
            val checksumEngine = Adler32()
            checksumEngine.update(binary)
            val checksum = checksumEngine.value
            if (this@MessageService.pending_files.containsKey(checksum) && binary != null) {
                val responseObject = this@MessageService.pending_files.get(checksum)!!
                responseObject.put("image",binary)
                this@MessageService.sentRequests.get(responseObject.getString("request_id"))!!.handleResponse(responseObject)
                this@MessageService.sentRequests.remove(responseObject.getString("request_id"))
                this@MessageService.pending_files.remove(checksum)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    protected fun sendMessage(messageObject:Map<String,Any>) {
        var json = JSONObject()
        var indexes = messageObject.keys
        var byteArray: ByteArray? = null
        for (index in indexes) {
            if (messageObject.get(index) is String) {
                json.put(index, messageObject.get(index)!!)
            } else if (index == "file") {
                byteArray = messageObject.get("file") as ByteArray
                val checksumEngine = Adler32()
                checksumEngine.update(byteArray)
                val checksum = checksumEngine.getValue()
                json.put("checksum",checksum)
            }
        }
        launch {
            this@MessageService.ws.sendText(json.toString())
            if (byteArray != null) {
                this@MessageService.ws.sendBinary(byteArray)
            }
        }
    }

    public fun scheduleRequest(messageObject:Map<String,Any>) {
        if (messageObject.get("request_id")!=null) {
            this.requestsQueue.put(messageObject.get("request_id").toString()!!,messageObject)
        }
    }
}
