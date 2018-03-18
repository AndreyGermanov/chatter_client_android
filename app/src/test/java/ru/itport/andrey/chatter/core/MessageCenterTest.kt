package ru.itport.andrey.chatter.core

import org.junit.Test

import org.junit.Assert.*
import java.util.HashMap
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.FileInputStream
import java.util.zip.Adler32

/**
 * Created by Andrey Germanov on 2/28/18.
 */
class MessageCenterTest {

    inner class ResponseHandler(delegate:MessageCenterTest) : MessageCenterResponseReceiver {

        val delegate: MessageCenterTest

        init {
            this.delegate = delegate
        }

        override fun handleResponse(request_id:String,response:Any) {

        }

        override lateinit var messageCenter: MessageCenter
    }

    val msgCenter: MessageCenter = MessageCenter()

    @Test
    fun onCreate() {
    }

    @Test
    fun onBind() {
    }

    @Test
    fun addRequest() {
        var result = msgCenter.addRequest(HashMap())
        assertEquals("Should not add empty requests",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_REQUEST_ID,result)
        result = msgCenter.addRequest( hashMapOf(
                "request_id" to ""
        ))
        assertEquals("Should not add requests with empty request_id",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_REQUEST_ID,result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123"
        ))
        assertEquals("Should not add requests without action",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_ACTION,result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to ""
        ))
        assertEquals("Should not add requests with empty action",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_ACTION,result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user"
        ))
        assertEquals("Should not add requests without sender",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_NO_SENDER,result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user",
                "sender" to ""
        ))
        assertEquals("Should not add requests with empty sender",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_INCORRECT_SENDER,result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user",
                "sender" to ArrayList<String>()
        ))
        assertEquals("Should not add requests with sender which does not implement MessageCenterResponseReceiver interface",
                MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_ERROR_INCORRECT_SENDER,result)
        var responseHandler = ResponseHandler(this)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user",
                "sender" to responseHandler
        ))
        assertEquals("Should return true if request conforms to standard",MessageCenter.AddRequestsQueueResult.REQUESTS_QUEUE_RESULT_OK,result)
        assertEquals("Should add request to queue",1,msgCenter.getRequestsQueueLength())
        val queue = msgCenter.getRequestsQueue()
        assertNotNull("Requests queue should contain item, identified by request_id",queue["123"])
        val req = queue["123"] as HashMap<String,Any>
        assertNotNull("Timestamp of request must be added",req["timestamp"])
    }

    @Test
    fun processRequests() {
    }

    @Test
    fun getRequestsQueueLength() {
    }

    @Test
    fun getPendingResponsesQueueLength() {
    }

    @Test
    fun cleanRequestsQueue() {
    }

    @Test
    fun cleanPendingResponsesQueue() {
    }

    @Test
    /**
     * Integration test, which sends profile image to server. Requires real server connection
     */
    fun sendBinaryDataTest() {
        msgCenter.onCreate()
        Thread.sleep(1000)
        var msg = """{"request_id":"12345","action":"login_user","login":"andrey","password":"123"}"""
        msgCenter.ws.sendText(msg)
        Thread.sleep(2000)
        val parser = JSONParser()
        val response = parser.parse(msgCenter.lastResponse) as JSONObject
        val user_id = response["user_id"].toString()
        val session_id = response["session_id"].toString()

        var updated_profile_img_path = System.getProperty("user.dir")+"/app/src/main/res/drawable/profile.png"

        val stream = FileInputStream(updated_profile_img_path)
        val img = stream.readBytes()
        val checksumEngine = Adler32()
        checksumEngine.update(img)
        val profile_image_checksum = checksumEngine.value

        val request = JSONObject(mapOf(
                "user_id" to user_id,
                "session_id" to session_id,
                "request_id" to "12345",
                "action" to "update_user",
                "first_name" to "Andrew",
                "profile_image_checksum" to profile_image_checksum
        ))

        msgCenter.ws.sendText(request.toJSONString())
        msgCenter.ws.sendBinary(img)
        Thread.sleep(2000)
        println(msgCenter.lastResponse)



    }

}