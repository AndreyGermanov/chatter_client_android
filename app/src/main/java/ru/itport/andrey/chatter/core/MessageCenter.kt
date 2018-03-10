package ru.itport.andrey.chatter.core

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.neovisionaries.ws.client.*
import kotlinx.coroutines.experimental.launch
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import ru.itport.andrey.chatter.actions.SmartEnum
import ru.itport.andrey.chatter.utils.toJSONString
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.Adler32
import kotlin.collections.HashMap

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
         * Runs on connection errors
         */
        override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
            super.onError(websocket, cause)
        }

        /**
         * Runs on disconnect from server
         */
        override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
            this@MessageCenter.connected = false
            this@MessageCenter.ws.connect()
        }

        /**
         * Runs when receive text message from server
         *
         * @param websocket Remote websocket from which message came
         * @param text Text body of received message
         */
        override fun onTextMessage(websocket: WebSocket?, text: String?) {
            super.onTextMessage(websocket, text)
            val logger = Logger.getLogger("onTextMessage")
            val parser = JSONParser()
            var response = JSONObject()
            try {
                response = parser.parse(text) as JSONObject
            } catch (e:Exception) {
                logger.log(Level.SEVERE,"Error parsing response from server '"+e.message+"'")
            }
            if (!response.isEmpty()) {
                if (!response.containsKey("status")) {
                    logger.log(Level.SEVERE, "Error parsing response from server. Does not contain status")
                } else if (!response.contains("request_id")) {
                    logger.log(Level.SEVERE, "Error parsing response from server. Does not contain request_id")
                } else if (!this@MessageCenter.pending_responses_queue.contains(response.get("request_id").toString())) {
                    logger.log(Level.SEVERE, "Error parsing response from server. Could not find request_id of this response in Pending responses queue")
                } else {
                    val pending_response = this@MessageCenter.pending_responses_queue.get(response.get("request_id")) as HashMap<String, Any>
                    if (!pending_response.contains("sender")) {
                        logger.log(Level.SEVERE, "Error parsing response from server. No object to pass response to")
                    } else if (pending_response.get("sender") !is MessageCenterResponseReceiver) {
                        logger.log(Level.SEVERE, "Error parsing response from server. Object to pass response to does not implement MessageCenterResponseReceiver interface")
                    } else {
                        val sender = pending_response.get("sender") as MessageCenterResponseReceiver
                        sender.handleResponse(response.get("request_id").toString(), response)
                    }
                }
            }
        }

        /**
         * Runs when receive binary data from server
         *
         * @param websocket Remote websocket from which message came
         * @param text binary body of received message
         */
        override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
            super.onBinaryMessage(websocket, binary)
            val logger = Logger.getLogger("onBinaryMessage")
            if (binary != null) {
                val checksumEngine = Adler32()
                checksumEngine.update(binary)
                if (pending_files_queue.containsKey(checksumEngine.value)) {
                    val pending_file = pending_files_queue[checksumEngine.value] as HashMap<String,Any>
                    if (pending_file.containsKey("request")) {
                        val request = pending_file["request"] as HashMap<String,Any>
                        if (request.containsKey("sender")) {
                            if (request["sender"] is MessageCenterResponseReceiver) {
                                val sender = request["sender"] as MessageCenterResponseReceiver
                                sender.handleResponse(checksumEngine.value.toString(),binary)
                            } else {
                                logger.log(Level.SEVERE,"Pending file request sender object has incorrect type")
                            }
                        } else {
                            logger.log(Level.SEVERE,"Pending file request does not contain link to sender object")
                        }
                    } else {
                        logger.log(Level.SEVERE,"Pending file queue record does not contain 'request' item")
                    }
                } else {
                    logger.log(Level.SEVERE,"Checksum of received file not found in pending files queue")
                }
            }
        }
    }

    val messageListener = WebSocketMessageAdapter()

    /**
     * WebSocket server credentials
     */
    val SERVER_HOST = "192.168.0.184"
    val SERVER_PORT = 8080
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
     * Determines, if MessageCenter started only for testing, without real connection to the server
     */
    var testingMode = false

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
     * Result codes for addRequest operation
     */
    enum class AddRequestsQueueResult(val value:String): SmartEnum {
        REQUESTS_QUEUE_RESULT_OK("REQUESTS_QUEUE_RESULT_OK"),
        REQUESTS_QUEUE_RESULT_ERROR_EMPTY_REQUEST("REQUESTS_QUEUE_RESULT_ERROR_EMPTY_REQUEST"),
        REQUESTS_QUEUE_RESULT_ERROR_NO_REQUEST_ID("REQUESTS_QUEUE_RESULT_ERROR_NO_REQUEST_ID"),
        REQUESTS_QUEUE_RESULT_ERROR_DUPLICATE_REQUEST_ID("REQUESTS_QUEUE_RESULT_ERROR_DUPLICATE_REQUEST_ID"),
        REQUESTS_QUEUE_RESULT_ERROR_NO_ACTION("REQUESTS_QUEUE_RESULT_ERROR_NO_ACTION"),
        REQUESTS_QUEUE_RESULT_ERROR_NO_SENDER("REQUESTS_QUEUE_RESULT_ERROR_NO_SENDER"),
        REQUESTS_QUEUE_RESULT_ERROR_INCORRECT_SENDER("REQUESTS_QUEUE_RESULT_ERROR_INCORRECT_SENDER");
        override fun getMessage():String {
            var result = ""
            when (this) {
                REQUESTS_QUEUE_RESULT_OK -> result = ""
                REQUESTS_QUEUE_RESULT_ERROR_EMPTY_REQUEST -> result = "Request is empty"
                REQUESTS_QUEUE_RESULT_ERROR_NO_REQUEST_ID -> result = "Request must have unique request_id"
                REQUESTS_QUEUE_RESULT_ERROR_DUPLICATE_REQUEST_ID -> result = "Request with this request_id already exists in queue"
                REQUESTS_QUEUE_RESULT_ERROR_NO_ACTION -> result = "Request must have an action"
                REQUESTS_QUEUE_RESULT_ERROR_NO_SENDER -> result = "Request must have link to sender object"
                REQUESTS_QUEUE_RESULT_ERROR_INCORRECT_SENDER -> result = "Sender object does not implement interface MessageCenterResponseReceiver"
            }
            return result
        }
    }

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
     * Queue of files, which should be transferred from server e.g. client already received
     * checksum of file, but file is still going. Each record keyed by checksum and has following
     * Hashmap<String,Any> as a values: request - copy of request, which is waiting, timestamp -
     * timestamp which defines when this record placed to queue
     */
    private val pending_files_queue = HashMap<Long,Any>()

    /**
     * Timeout of pending file queue. If timeout exceeded, row will be removed from queue by
     * background task
     */
    var PENDING_FILES_QUEUE_TIMEOUT = 120

    /**
     * Result codes for addPendingFile operation
     */
    enum class AddPendingFileToQueueResult(val value:String): SmartEnum {
        PENDING_FILES_QUEUE_OK("PENDING_FILES_QUEUE_OK"),
        PENDING_FILES_NO_REQUEST_ID("PENDING_FILES_NO_REQUEST"),
        PENDING_FILES_ALREADY_EXISTS("PENDING_FILES_ALREADY_EXISTS");
        override fun getMessage(): String {
            var result = ""
            when(this) {
                PENDING_FILES_QUEUE_OK -> result = ""
                PENDING_FILES_NO_REQUEST_ID -> result = "Request object does not contain ID"
                PENDING_FILES_ALREADY_EXISTS -> result = "Pending file request for this file already exists"
            }
            return result
        }

    }

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
            launch {
                this@MessageCenter.runCronjob()
            }
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
        launch {
            if (!testingMode) {
                try {
                    ws = WebSocketFactory().createSocket("ws://" + SERVER_HOST + ":" + SERVER_PORT + SERVER_ENDPOINT)
                    ws.connect()
                } catch (e: Exception) {
                    connected = false
                }
            } else {
                ws = WebSocketFactory().createSocket("ws://testing")
            }
            ws.addListener(messageListener)
            timer.schedule(Cronjob(),0,1000)
        }

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
    fun addRequest(request:HashMap<String,Any>):AddRequestsQueueResult {
        if (!request.contains("request_id") || request.get("request_id").toString().isEmpty()) {
            return AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_REQUEST_ID
        }
        if (!request.contains("action") || request.get("action").toString().isEmpty()) {
            return AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_ACTION
        }
        if (!request.contains("sender")) {
            return AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_SENDER
        }
        if (request.get("sender") !is MessageCenterResponseReceiver) {
            return AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_INCORRECT_SENDER
        }
        if (!requests_queue.contains(request.get("request_id").toString())) {
            request.set("timestamp",System.currentTimeMillis()/1000)
            requests_queue.set(request.get("request_id").toString(),request)
            return AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_OK
        } else {
            return AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_DUPLICATE_REQUEST_ID
        }
    }

    /**
     * Function gets next request from requests_queue, sends it to the server, if connection established
     * and moves request to pending_responses_queue.
     *
     * @return ArrayList copy of JSON strings, sent to server as requests
     */
    fun processRequests(): ArrayList<String> {
        val result = ArrayList<String>()
        if (connected) {
            val tmp_queue = requests_queue.clone() as HashMap<String,Any>;
            val it = tmp_queue.iterator()
            for ((index,row) in it) {
                try {
                    val request = row as HashMap<String, Any>
                    val sender = request.get("sender")
                    pending_responses_queue.set(request.get("request_id").toString(),
                            mapOf("request_id" to request.get("request_id").toString(),
                                    "sender" to sender,
                                    "request" to request,
                                    "timestamp" to System.currentTimeMillis() / 1000)
                    )
                    val json_request = JSONObject()
                    for ((index, value) in request) {
                        if (index!="sender") {
                            json_request[index] = value
                        }
                    }
                    result.add(toJSONString(json_request))
                    launch {
                        ws.sendText(toJSONString(json_request))
                    }
                    requests_queue.remove(index)
                } catch (e:Exception) {
                    requests_queue.remove(index)
                }
            }
        }
        return result
    }

    /**
     * Used to add entry to pending_files_queue
     *
     * @param checksum Checksum of file which is pending
     * @param request Request, which is waiting for this file
     *
     * @return Error or success code of operation
     */
    fun addPendingFile(checksum:Long,request:HashMap<String,Any>):AddPendingFileToQueueResult {
        if (!request.containsKey("request_id")) {
            return AddPendingFileToQueueResult.PENDING_FILES_NO_REQUEST_ID
        } else if (pending_files_queue.containsKey(checksum)) {
            return AddPendingFileToQueueResult.PENDING_FILES_ALREADY_EXISTS
        } else {
            pending_files_queue[checksum] = mapOf("request" to request,"timestamp" to (System.currentTimeMillis()/1000))
            return AddPendingFileToQueueResult.PENDING_FILES_QUEUE_OK
        }
    }

    /**
     * Function returns length of requests queue
     *
     * @return Length of queue
     */
    fun getRequestsQueueLength():Int {
        return requests_queue.count()
    }

    /**
     * Function returns length of pending responses queue
     *
     * @return Length of queue
     */
    fun getPendingResponsesQueueLength():Int {
        return pending_responses_queue.count()
    }

    /**
     * Function returns length of pending files queue
     *
     * @return Length of queue
     */
    fun getPendingFilesQueueLength():Int {
        return pending_files_queue.count()
    }

    /**
     * Function returns copy of requests queue
     *
     * @return Requests queue Hashmap
     */
    fun getRequestsQueue(): HashMap<String,Any> {
        return requests_queue.clone() as HashMap<String,Any>
    }

    /**
     * Function returns copy of pending responses queue
     *
     * @return Pending responses queue HashMap
     */
    fun getPendingResponsesQueue(): HashMap<String,Any> {
        return pending_responses_queue.clone() as HashMap<String, Any>
    }

    /**
     * Function returns copy of pending files queue
     *
     * @return Pending files queue HashMap
     */
    fun getPendingFilesQueue(): HashMap<Long,Any> {
        return pending_files_queue.clone() as HashMap<Long, Any>
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
                    val timestamp = request.get("timestamp") as Long
                    if (System.currentTimeMillis()/1000 - timestamp >= REQUESTS_QUEUE_TIMEOUT) {
                        requests_queue.remove(index)
                    }
                } else {
                    requests_queue.remove(index)
                }
            }
        }
    }

    /**
     * Function removes outdated requests from pending_responses_queue
     */
    fun cleanPendingResponsesQueue() {
        if (pending_responses_queue.count()>0) {
            val q = pending_responses_queue.clone() as HashMap<String, Any>
            for ((index, value) in q) {
                val request = value as HashMap<String, Any>
                if (request.contains("timestamp")) {
                    val timestamp = request.get("timestamp") as Long
                    if (System.currentTimeMillis()/1000 - timestamp >= PENDING_RESPONSES_QUEUE_TIMEOUT) {
                        pending_responses_queue.remove(index)
                    }
                } else {
                    pending_responses_queue.remove(index)
                }
            }
        }
    }

    /**
     * Function removes outdated requests from pending_files_queue
     */
    fun cleanPendingFilesQueue() {
        if (pending_files_queue.count()>0) {
            val q = pending_files_queue.clone() as HashMap<Long, Any>
            for ((index, value) in q) {
                val request = value as HashMap<String, Any>
                if (request.contains("timestamp")) {
                    val timestamp = request.get("timestamp") as Long

                    if (System.currentTimeMillis()/1000 - timestamp >= PENDING_FILES_QUEUE_TIMEOUT) {
                        pending_files_queue.remove(index)
                    }
                } else {
                    pending_files_queue.remove(index)
                }
            }
        }
    }

    /**
     * Function removes pending response, specified by provided [request_id].
     *
     * @param request_id Request_id which record should be removed
     */
    fun removePendingResponse(request_id:String) {
        if (pending_responses_queue.contains(request_id)) {
            pending_responses_queue.remove(request_id)
        }
    }

    /**
     * Function removes pending file queue entry, specified by provided [checksum].
     *
     * @param checksum Checksum of file to remove
     */
    fun removePendingFile(checksum:Long) {
        if (pending_files_queue.contains(checksum)) {
            pending_files_queue.remove(checksum)
        }
    }

    /**
     * Function which runs every second and does background maintenance tasks
     */
    fun runCronjob() {
        val logger = Logger.getLogger("runCronjob")
        if (!connected && !testingMode) {
            try {
                ws.connect()
            } catch (e:Exception) {
                try {
                    ws = WebSocketFactory().createSocket("ws://" + SERVER_HOST + ":" + SERVER_PORT + SERVER_ENDPOINT)
                    ws.connect()
                } catch (e:Exception) {
                    logger.log(Level.SEVERE,"Failed to connect to WebSocket server - "+e.message)
                }

            }
        }
        processRequests()
        cleanRequestsQueue()
        cleanPendingResponsesQueue()
        cleanPendingFilesQueue()
    }

}