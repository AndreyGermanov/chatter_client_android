package ru.itport.andrey.chatter.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import kotlinx.coroutines.experimental.selects.whileSelect
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import ru.itport.andrey.chatter.utils.toJSONString
import java.util.*

/**
 * Interface, which must implement class of any object, which want to
 * send requests to MessageCenter and receive WebSocket responses from it
 */
interface MessageCenterResponseReceiver {
    /**
     * Function, which message server invokes, when receives response
     * to request, sent by this object
     */
    fun handleResponse(request_id:String,response:Any)
}

/**
 * Service, which handles exchange between app and server via WebSocket, including
 * registration, login, logout, send/receive messages
 */
class MessageCenter : Service() {

    /**
     * Class which used as a delegate to handle WebSocket communications of MessageCenter
     */
    inner class WebSocketMessageAdapter: WebSocketAdapter() {

        /**
         * Runs when connection established with server
         */
        override fun onConnected(websocket: WebSocket?, headers: MutableMap<String, MutableList<String>>?) {
            this@MessageCenter.connected = true
        }

        /**
         * Runs on disconnect from server
         */
        override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
            this@MessageCenter.connected = false
        }

        /**
         * Runs when receive text message from server
         */
        override fun onTextMessage(websocket: WebSocket?, text: String?) {
            super.onTextMessage(websocket, text)
        }

        /**
         * Runs when receive binary data from server
         */
        override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
            super.onBinaryMessage(websocket, binary)
        }
    }


    /**
     * WebSocket server credentials
     */
    val SERVER_HOST = "192.168.0.184"
    val SERVER_PORT = 8081
    val SERVER_ENDPOINT = "/websocket"


    /**
     * Link to WebSocket connection to server
     */
    lateinit var ws: WebSocket

    /**
     * Determines if connection to WebSocket server is established
     */
    var connected = false

    /**
     * Queue of requests, which senders sent to MessageCenter. Key is unique id of request
     * Value is any object, which must have at least following fields:
     * request_id - Unique ID of request, which used to identify request among others
     * action - action which this request should do (register_user, send_message and so on)
     * sender - link to an object, which sent request. This object must implement "MessageCenterResponseReceiver"
     * interface. When MessageCenter processed request, it calls "handleResponse" method of sender with provided
     * request_id and body of response
     */
    private val requests_queue = HashMap<String,Any>()

    /**
     * Timeout of request in requests queue. If timeout exceeded,
     * request will be removed from queue by background cronjob
     */
    private val REQUESTS_QUEUE_TIMEOUT = 10

    /**
     * Queue of requests, which was moved from "requests_queue" after sending to the server.
     * These queues waiting for response. When response received, WebSocketMessageAdapter processes
     * response and removes this request from pending_requests_queue
     */
    private val pending_responses_queue = HashMap<String,Any>()

    /**
     * Timeout of pending response. If timeout exceeded,
     * request will be removed from queue by background job
     */
    private val PENDING_RESPONSES_QUEUE_TIMEOUT = 60

    /**
     * Timer object, used to run requests queue processing cronjob
     * in separate thread
     */
    private val timer: Timer = Timer()

    /**
     * Timer task implementation for timer
     */
    inner class Cronjob: TimerTask() {
        override fun run() {
            this@MessageCenter.runCronjob()
        }
    }

    /**
     * Internal class, implementing interface, which activities used
     * to bind to this service
     */
    inner class LocalBinder: Binder() {
        fun getService() : MessageCenter {
            return this@MessageCenter
        }
    }

    /**
     * Instance of LocalBinder class
     */
    val binder = LocalBinder()

    /**
     * Function runs when service initialized first time
     */
    override fun onCreate() {
        super.onCreate()
        ws = WebSocketFactory().createSocket("ws://"+SERVER_HOST+":"+SERVER_PORT+SERVER_ENDPOINT)
        ws.connect()
        ws.addListener(WebSocketMessageAdapter())
        timer.schedule(Cronjob(),0,1000)
    }

    /**
     * This method invoked when activity initiates connection to
     * Message center
     */
    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    /**
     * Function adds provided request to queue
     *
     * @param request Body of request. Should contain at least the following fields:
     * request_id - Unique ID of this request
     * action - action which is a purpose of this request
     * sender - link to an object, which sends request
     *
     * @return True if request successfully added or false otherwise
     */
    fun addRequest(request:HashMap<String,Any>):Boolean {
        if (!request.contains("request_id") || request.get("request_id").toString().isEmpty()) {
            return false
        }
        if (!request.contains("action") || request.get("action").toString().isEmpty()) {
            return false
        }
        if (!request.contains("sender") || request.get("sender") !is MessageCenterResponseReceiver) {
            return false
        }
        if (!requests_queue.contains(request.get("request_id").toString())) {
            request.set("timestamp",System.currentTimeMillis()/1000)
            requests_queue.set(request.get("request_id").toString(),request)
            return true
        } else {
            return false
        }
    }

    /**
     * Function gets next request from requests_queue, sends it to the server, if connection established
     * and moves request to pending_responses_queue.
     */
    fun processRequests(): ArrayList<String> {
        val result = ArrayList<String>()
        if (connected) {
            val it = requests_queue.iterator()
            while (it.hasNext()) {
                val request = it.next() as HashMap<String, Any>
                val sender = request.get("sender")
                pending_responses_queue.set(request.get("request_id").toString(),
                        mapOf("request_id" to request.get("request_id").toString(),
                                "sender" to sender,
                                "timestamp" to System.currentTimeMillis()/1000)
                )
                request.remove("sender")
                val json_request = JSONObject()
                for ((index, value) in request) {
                    json_request[index] = value
                }
                result.add(toJSONString(json_request))
                ws.sendText(toJSONString(json_request))
            }
        }
        return result
    }

    /**
     * Function returns length of requests queue
     */
    fun getRequestsQueueLength():Int {
        return requests_queue.count()
    }

    /**
     * Function returns length of pending responses queue
     */
    fun getPendingResponsesQueueLength():Int {
        return pending_responses_queue.count()
    }

    /**
     * Function returns copy of requests queue
     */
    fun getRequestsQueue(): HashMap<String,Any> {
        return requests_queue.clone() as HashMap<String,Any>
    }

    /**
     * Function returns copy of pending responses queue
     */
    fun getPendingResponsesQueue(): HashMap<String,Any> {
        return pending_responses_queue.clone() as HashMap<String, Any>
    }

    /**
     * Function removes outdated requests from requests_queue
     */
    fun cleanRequestsQueue() {
        if (requests_queue.count()>0) {
            val q = requests_queue.clone() as HashMap<String, Any>
            for ((index, value) in q) {
                val request = value as HashMap<String, Any>
                if (request.contains("timestamp")) {
                    val timestamp = request.get("timestamp") as Int
                    if (System.currentTimeMillis()/1000 - timestamp > REQUESTS_QUEUE_TIMEOUT) {
                        requests_queue.remove(index)
                    }
                } else {
                    requests_queue.remove(index)
                }
            }
        }
    }

    /**
     * Function removes outdated requests from pending_responses_queue ]
     */
    fun cleanPendingResponsesQueue() {
        if (pending_responses_queue.count()>0) {
            val q = pending_responses_queue.clone() as HashMap<String, Any>
            for ((index, value) in q) {
                val request = value as HashMap<String, Any>
                if (request.contains("timestamp")) {
                    val timestamp = request.get("timestamp") as Int
                    if (System.currentTimeMillis()/1000 - timestamp > PENDING_RESPONSES_QUEUE_TIMEOUT) {
                        pending_responses_queue.remove(index)
                    }
                } else {
                    pending_responses_queue.remove(index)
                }
            }
        }
    }

    /**
     * Function which runs every second to send all requests from requests_queue
     * and clean outdated requests
     */
    fun runCronjob() {
        processRequests()
        cleanRequestsQueue()
        cleanPendingResponsesQueue()
    }

}