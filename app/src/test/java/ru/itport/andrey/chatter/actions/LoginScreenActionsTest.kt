/**
 * Created by Andrey Germanov on 2/28/18.
 */

package ru.itport.andrey.chatter.actions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat.getDrawable
import kotlinx.android.synthetic.main.activity_profile_settings.view.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.itport.andrey.chatter.R
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.store.AppScreens
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import java.io.File
import java.io.FileInputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.zip.Adler32

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
        msgCenter.testingMode = true
        msgCenter.onCreate()
        msgCenter.connected = true
        Thread.sleep(1000)
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

    @Test
    fun login() {
        // Client side response handling
        LoginScreenActions.login(null)
        var state = appStore.state["LoginForm"] as JSONObject
        assertNotNull("Should contain errors object",state["errors"])
        var errors = state["errors"] as JSONObject
        assertNotNull("Should contain error for login field",errors["login"])
        assertNotNull("Should contain error for password field",errors["password"])
        assertEquals("Error for login field incorrect error",
                LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_INCORRECT_LOGIN,
                errors["login"] as LoginScreenActions.LoginScreenLoginErrors)
        assertEquals("Error for password field incorrect",
                LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_INCORRECT_PASSWORD,
                errors["password"] as LoginScreenActions.LoginScreenLoginErrors)

        appStore.dispatch(LoginScreenActions.changeProperty("login","test"))
        appStore.dispatch(LoginScreenActions.changeProperty("password","123"))
        val msgCenter = MessageCenter()
        msgCenter.testingMode = true
        msgCenter.onCreate()
        LoginScreenActions.messageCenter = msgCenter
        LoginScreenActions.login()
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertNotNull("Should contain general error",errors["general"])
        var error = errors["general"] as LoginScreenActions.LoginScreenLoginErrors
        assertEquals("Should return connection error message",LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_CONNECTION_ERROR,error)
        msgCenter.messageListener.onConnected(null,null)
        LoginScreenActions.login()
        state = appStore.state["LoginForm"] as JSONObject
        assertTrue("Should show load progress indicator, before make request to server",state["show_progress_indicator"] as Boolean)
        assertEquals("Should add login request to requests queue",1,msgCenter.getRequestsQueueLength())
        Thread.sleep(1000)
        assertEquals("Should send request and remove it from requests queue",0,msgCenter.getRequestsQueueLength())
        assertEquals("Should send request and move it to pending responses queue",1,msgCenter.getPendingResponsesQueueLength())
        var pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        var request_id = pendingRequest.get("request_id") as String
        Thread.sleep(1000)
        var msg = """{"request_id":"fake","status":"error","action":"login_user","status_code":"RESULT_ERROR_NOT_ACTIVATED"}""";
        msgCenter.messageListener.onTextMessage(null,msg)
        assertEquals("Should not react to response with incorrect request_id",1,msgCenter.getPendingResponsesQueueLength())
        Thread.sleep(1000)
        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_NOT_ACTIVATED","action":"login_user"}""";
        msgCenter.messageListener.onTextMessage(null,msg)
        assertEquals("Should remove record from pending responses",0,msgCenter.getPendingResponsesQueueLength())
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertFalse("Should remove progress indicator, after make request to server",state["show_progress_indicator"] as Boolean)
        assertEquals("Should return 'Account not activated' error in correct format",
                LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_NOT_ACTIVATED,
                errors["general"] as LoginScreenActions.LoginScreenLoginErrors )
        LoginScreenActions.login()
        Thread.sleep(2000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_ALREADY_LOGIN","action":"login_user"}""";
        msgCenter.messageListener.onTextMessage(null,msg)
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertEquals("Should return 'User already login' error in correct format",
                LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_ALREADY_LOGIN,
                errors["general"] as LoginScreenActions.LoginScreenLoginErrors )
        LoginScreenActions.login()
        Thread.sleep(2000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"login_user"}""";
        msgCenter.messageListener.onTextMessage(null,msg)
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertEquals("Should return Unknown error if success response does not contain all required data, as list of rooms",
                LoginScreenActions.LoginScreenLoginErrors.RESULT_ERROR_UNKNOWN,
                errors["general"] as LoginScreenActions.LoginScreenLoginErrors )
        LoginScreenActions.login()
        Thread.sleep(2000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"login_user","rooms":[{"_id":"123456","name":"Room 1"},{"_id":"54321","name":"Room 2"}]}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = appStore.state as JSONObject
        var userState = state["User"] as JSONObject
        assertTrue("Should set 'IsLogin' of User state to true",userState["isLogin"] as Boolean)
        assertEquals("Should move to 'User profile' screen if no default room set",AppScreens.USER_PROFILE,state["current_activity"] as AppScreens)
        LoginScreenActions.login()
        Thread.sleep(2000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"login_user","user_id":"u1","session_id":"s1","login":"test","email":"test@test.com","default_room":"54321","first_name":"Bob","last_name":"Johnson",
            |"birthDate":1234567890,"gender":"M","rooms":[{"_id":"123456","name":"Room 1"},{"_id":"54321","name":"Room 2"}]}""".trimMargin()
        msgCenter.messageListener.onTextMessage(null,msg)
        state = appStore.state as JSONObject
        userState = state["User"] as JSONObject
        var userProfile = state["UserProfile"] as JSONObject
        assertTrue("Should set 'IsLogin' of User state to true",userState["isLogin"] as Boolean)
        assertEquals("Should move to 'Chat' screen if default room is set",state["current_activity"] as AppScreens,AppScreens.CHAT)
        assertEquals("Should set correct user_id to User","u1",userState["user_id"] as String)
        assertEquals("Should set correct login to User","test",userState["login"] as String)
        assertEquals("Should set correct email to User","test@test.com",userState["email"] as String)
        assertEquals("Should set correct session_id to User","s1",userState["session_id"] as String)
        assertEquals("Should set correct first_name to User","Bob",userState["first_name"] as String)
        assertEquals("Should set correct last_name to User","Johnson",userState["last_name"] as String)
        assertEquals("Should set correct gender to User","M", userState["gender"] as String)
        assertEquals("Should set correct birthDate to User",1234567890,userState["birthDate"] as Int)
        assertEquals("Should set correct login to User Profile","test",userProfile["login"] as String)
        assertEquals("Should set correct email to User Profile","test@test.com",userProfile["email"] as String)
        assertEquals("Should set correct first_name to User Profile","Bob",userProfile["first_name"] as String)
        assertEquals("Should set correct last_name to User Profile","Johnson",userProfile["last_name"] as String)
        assertEquals("Should set correct gender to User Profile","M",userProfile["gender"] as String)
        assertEquals("Should set correct birthDate to User Profile",1234567890,userProfile["birthDate"] as Int)
        assertEquals("Should set rooms to user profile",2,(userProfile["rooms"] as JSONArray).count())
        LoginScreenActions.login()
        Thread.sleep(1000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        Thread.sleep(1000)
        var bm = BitmapFactory.Options()
        bm.inJustDecodeBounds = false

        var stream = FileInputStream(System.getProperty("user.dir")+"/app/src/main/res/drawable/user.png")
        var bytes = stream.readBytes()
        var bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.size,bm)
        println(bmp)
        var buffer = ByteBuffer.allocate(bmp.width*bmp.height)
        var img = buffer.array()
        println(img.size)
        bmp = BitmapFactory.decodeFile(System.getProperty("user.dir")+"/app/src/main/res/drawable/profile.png",bm)
        buffer = ByteBuffer.allocate(bmp.width*bmp.height)
        var fake_img = buffer.array()

        var checksumEngine = Adler32()
        checksumEngine.update(img)
        var checksum = checksumEngine.value
        LoginScreenActions.login()
        Thread.sleep(1000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        Thread.sleep(1000)
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"login_user","user_id":"u1","session_id":"s1","default_room":"54321","first_name":"Bob","last_name":"Johnson",
            |"birthDate":1234567890,"gender":"M","rooms":[{"_id":"123456","name":"Room 1"},{"_id":"54321","name":"Room 2"}],"checksum":"${checksum}"}""".trimMargin()
        msgCenter.messageListener.onTextMessage(null,msg)
        state = appStore.state as JSONObject
        userState = state["User"] as JSONObject
        userProfile = state["UserProfile"] as JSONObject
        assertEquals("Should save request to pending files queue",1,msgCenter.getPendingFilesQueueLength())

        val pendingFile = msgCenter.getPendingFilesQueue().iterator().next()
        val pendingFileChecksum = pendingFile.key
        val pendingFileEntry = pendingFile.value as HashMap<String,Any>
        assertEquals("Pending file queue key should be checksum of file",checksum,pendingFileChecksum)
        assertNotNull("Pending file queue entry should contain timestamp of record",pendingFileEntry["timestamp"])
        assertNotNull("Pending file queue entry should contain request, which initiated this file transfer",pendingFileEntry["request"])
        var request = pendingFileEntry["request"] as HashMap<String,Any>
        assertEquals("ID of file response request should be equal to initial request",request_id,request["request_id"].toString())
        msgCenter.PENDING_FILES_QUEUE_TIMEOUT = 1
        Thread.sleep(2000)
        assertEquals("Should remove entry from pending files queue, if no file received during timeout",0,msgCenter.getPendingFilesQueueLength())
        msgCenter.PENDING_FILES_QUEUE_TIMEOUT = 120
        LoginScreenActions.login()
        Thread.sleep(2000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"login_user","user_id":"u1","session_id":"s1","default_room":"54321","first_name":"Bob","last_nama":"Johnson",
            |"birthDate":1234567890,"gender":"M","rooms":[{"_id":"123456","name":"Room 1"},{"_id":"54321","name":"Room 2"}],"checksum":"${checksum}"}""".trimMargin()
        msgCenter.messageListener.onTextMessage(null,msg)
        msgCenter.messageListener.onBinaryMessage(null,fake_img)
        state = appStore.state as JSONObject
        userState = state["User"] as JSONObject
        userProfile = state["UserProfile"] as JSONObject
        assertNull("Should not update profile with fake image which has different checksum",userState["profileImage"])
        assertNull("Should not update profile with fake image which has different checksum",userProfile["profileImage"])
        LoginScreenActions.login()
        Thread.sleep(2000)
        pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        request_id = pendingRequest.get("request_id") as String
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"login_user","user_id":"u1","session_id":"s1","default_room":"54321","first_name":"Bob","last_nama":"Johnson",
            |"birthDate":1234567890,"gender":"M","rooms":[{"_id":"123456","name":"Room 1"},{"_id":"54321","name":"Room 2"}],"checksum":"${checksum}"}""".trimMargin()
        msgCenter.messageListener.onTextMessage(null,msg)
        msgCenter.messageListener.onBinaryMessage(null,img)
        state = appStore.state as JSONObject
        userState = state["User"] as JSONObject
        userProfile = state["UserProfile"] as JSONObject
        assertNotNull("Should update profile with correct image",userState["profileImage"])
        assertNotNull("Should update profile with correct image",userProfile["profileImage"])
        val profileImageBitmap = userState["profileImage"] as Bitmap
        buffer = ByteBuffer.allocate(profileImageBitmap.width*profileImageBitmap.height)
        val profileImage = buffer.array()
        checksumEngine.reset()
        checksumEngine.update(profileImage)
        assertEquals("Updated profile image must have the same checksum as image, which sent by server",checksum,checksumEngine.value)
        assertEquals("Should remove entry from pending files queue after processing",0,msgCenter.getPendingFilesQueueLength())
    }
}