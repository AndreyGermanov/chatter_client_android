package ru.itport.andrey.chatter.core

import org.junit.Test

import org.junit.Assert.*
import java.util.HashMap

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

}