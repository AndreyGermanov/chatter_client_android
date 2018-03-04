/**
 * Created by Andrey Germanov on 2/28/18.
 */

package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.reducers.LoginScreenReducer
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.utils.toJSONString

class LoginScreenActionsTest {

    @Test
    fun switchMode() {
        appStore.dispatch(LoginScreenActions.switchMode(LoginFormMode.REGISTER))
        var state = appStore.state["LoginForm"] as JSONObject
        assertEquals("First mode switch works as expected",state["mode"] as LoginFormMode,LoginFormMode.REGISTER)
        appStore.dispatch(LoginScreenActions.switchMode(LoginFormMode.LOGIN))
        state = appStore.state["LoginForm"] as JSONObject
        assertEquals("Second mode switch works as expected",state["mode"] as LoginFormMode,LoginFormMode.LOGIN)
    }

    @Test
    fun changeFormFields() {
        appStore.dispatch(LoginScreenActions.changeProperty("login","Test"))
        appStore.dispatch(LoginScreenActions.changeProperty("password","pass1"))
        appStore.dispatch(LoginScreenActions.changeProperty("confirm_password","pass2"))
        appStore.dispatch(LoginScreenActions.changeProperty("email","andrey@it-port.ru"))
        val state = appStore.state["LoginForm"] as JSONObject
        assertEquals("Should set correct login","Test",state["login"].toString())
        assertEquals("Should set correct password","pass1",state["password"].toString())
        assertEquals("Should set correct confirm_password","pass2",state["confirm_password"].toString())
        assertEquals("Should set correct email","andrey@it-port.ru",state["email"].toString())
    }

    @Test
    fun register() {
        // Client side response handling
        appStore.dispatch(LoginScreenActions.register())
        var state = appStore.state["LoginForm"] as JSONObject
        assertNotNull("Should contain errors object",state["errors"])
        var errors = state["errors"] as JSONObject
        assertNotNull("Should contain error for login field",errors["login"])
        assertNotNull("Should contain error for password field",errors["password"])
        assertNotNull("Should contain error for email field",errors["email"])
        assertEquals("Error for login field should be empty field error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["login"] as LoginScreenActions.LoginScreenRegisterErrors)
        assertEquals("Error for password field should be empty field error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["password"] as LoginScreenActions.LoginScreenRegisterErrors)
        assertEquals("Error for email field should be empty field error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["email"] as LoginScreenActions.LoginScreenRegisterErrors)

        appStore.dispatch(LoginScreenActions.changeProperty("login","test"))
        appStore.dispatch(LoginScreenActions.changeProperty("password","123"))
        appStore.dispatch(LoginScreenActions.register())
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertNotNull("Should contain error for password field",errors["password"])
        assertNotNull("Should contain error for email field",errors["email"])
        assertEquals("Error for password field should be incorrect confirm password error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT,
                errors["password"] as LoginScreenActions.LoginScreenRegisterErrors)
        assertEquals("Error for email field should be empty field error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["email"] as LoginScreenActions.LoginScreenRegisterErrors)

        appStore.dispatch(LoginScreenActions.changeProperty("confirm_password","356"))
        appStore.dispatch(LoginScreenActions.changeProperty("email","andrey"))
        appStore.dispatch(LoginScreenActions.register())
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertNotNull("Should contain error for password field",errors["password"])
        assertNotNull("Should contain error for email field",errors["email"])
        assertEquals("Error for password field should be incorrect confirm password error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_CONFIRM_PASSWORD_INCORRECT,
                errors["password"] as LoginScreenActions.LoginScreenRegisterErrors)
        assertEquals("Error for email field should be incorrect email error",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_INCORRECT_EMAIL,
                errors["email"] as LoginScreenActions.LoginScreenRegisterErrors)
        appStore.dispatch(LoginScreenActions.changeProperty("confirm_password","123"))
        appStore.dispatch((LoginScreenActions.changeProperty("email","andrey@it-port.ru")))
        val msgCenter = MessageCenter()
        LoginScreenActions.messageCenter = msgCenter
        LoginScreenActions.register()
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertNotNull("Should return general error",errors["general"])
        assertEquals("Should return server connection error",LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_CONNECTION_ERROR,
                errors["general"] as LoginScreenActions.LoginScreenRegisterErrors)
        msgCenter.messageListener.onConnected(null,null)
        LoginScreenActions.register()
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertEquals("Should not return errors",0,errors.count())
        assertTrue("Should show load progress indicator, before make request to server",state["show_progress_indicator"] as Boolean)
        assertEquals("Should add request to requests queue",1,msgCenter.getRequestsQueueLength())
        LoginScreenActions.register()
        assertEquals("Should not add duplicate request while current request is in progress", 1, msgCenter.getRequestsQueueLength())
        msgCenter.onCreate()
        msgCenter.connected = true
        Thread.sleep(2000)
        assertEquals("Should send request and remove it from requests queue",0,msgCenter.getRequestsQueueLength())
        assertEquals("Should add request to pending responses queue",1,msgCenter.getPendingResponsesQueueLength())
        Thread.sleep(1000)
        var pendingRequests = msgCenter.getPendingResponsesQueue()
        var it = pendingRequests.iterator()
        var pendingRequest = it.next().value as HashMap<String,Any>
        var message = """{"request_id":""""+pendingRequest.get("request_id").toString()+"""","status":"ok","status_code":"RESULT_OK"}"""
        msgCenter.messageListener.onTextMessage(null,message)
        state = appStore.state["LoginForm"] as JSONObject
        assertFalse("Should remove progress indicator",state["show_progress_indicator"] as Boolean)
        assertTrue("Should set message about successful registration to popup",!state["popup_message"].toString().isEmpty())
        assertEquals("Should move to login screen",LoginFormMode.LOGIN,state["mode"] as LoginFormMode)
        assertEquals("Should remove item from pending responses queue",0,msgCenter.getPendingResponsesQueueLength())
        LoginScreenActions.register()
        Thread.sleep(2000)
        pendingRequests = msgCenter.getPendingResponsesQueue()
        it = pendingRequests.iterator()
        pendingRequest = it.next().value as HashMap<String,Any>
        message = """{"request_id":""""+pendingRequest.get("request_id").toString()+"""","status":"error","status_code":"RESULT_WEIRD"}"""
        msgCenter.messageListener.onTextMessage(null,message)
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertNotNull("Should return general error",errors["general"])
        assertEquals("Error should be RESULT_ERROR_UNKNOWN",LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_UNKNOWN,
                errors["general"] as LoginScreenActions.LoginScreenRegisterErrors)
        LoginScreenActions.register()
        Thread.sleep(2000)
        pendingRequests = msgCenter.getPendingResponsesQueue()
        it = pendingRequests.iterator()
        pendingRequest = it.next().value as HashMap<String,Any>
        message = """{"request_id":""""+pendingRequest.get("request_id").toString()+"""","status":"error","status_code":"RESULT_ERROR_LOGIN_EXISTS"}"""
        msgCenter.messageListener.onTextMessage(null,message)
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertEquals("Error should be RESULT_ERROR_LOGIN_EXISTS",LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_LOGIN_EXISTS,
                errors["general"] as LoginScreenActions.LoginScreenRegisterErrors)
        assertEquals("Should stay on REGISTER screen in case of error",LoginFormMode.REGISTER,state["mode"] as LoginFormMode)
    }
}