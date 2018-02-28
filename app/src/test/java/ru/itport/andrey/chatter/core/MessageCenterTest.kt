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
        assertFalse("Should not add empty requests",result)
        result = msgCenter.addRequest( hashMapOf(
                "request_id" to ""
        ))
        assertFalse("Should not add requests with empty request_id",result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123"
        ))
        assertFalse("Should not add requests without action",result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to ""
        ))
        assertFalse("Should not add requests with empty action",result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user"
        ))
        assertFalse("Should not add requests without sender",result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user",
                "sender" to ""
        ))
        assertFalse("Should not add requests with empty sender",result)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user",
                "sender" to ArrayList<String>()
        ))
        assertFalse("Should not add requests with sender which does not implement MessageCenterResponseReceiver interface",result)
        var responseHandler = ResponseHandler(this)
        result = msgCenter.addRequest(hashMapOf(
                "request_id" to "123",
                "action" to "register_user",
                "sender" to responseHandler
        ))
        assertTrue("Should return true if request conforms to standard",result)
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