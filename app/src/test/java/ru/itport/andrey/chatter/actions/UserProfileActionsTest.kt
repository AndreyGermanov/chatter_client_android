package ru.itport.andrey.chatter.actions

import org.json.simple.JSONArray
import org.json.simple.JSONObject

import org.junit.Assert.*
import org.junit.Test
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.core.MessageCenterResponseReceiver
import ru.itport.andrey.chatter.store.AppScreens
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.store.getStateOf
import java.io.FileInputStream
import java.util.zip.CRC32

/**
 * Created by Andrey Germanov on 3/16/18.
 */
class UserProfileActionsTest {

    @Test
    fun changeProperty() {
        val currentState = appStore.state["UserProfile"] as JSONObject
        UserProfileActions.changeProperty("","")
        assertTrue("Should not change anything if property name not provided",currentState.equals(appStore.state["UserProfile"] as JSONObject))
        appStore.dispatch(UserProfileActions.changeProperty("not_exist","Cool setup"))
        var state = appStore.state["UserProfile"] as JSONObject
        assertNull("Should not set property which is not initially defined when Store initialized",state["not_exist"])
        appStore.dispatch(UserProfileActions.changeProperty("default_room","r5"))
        state = appStore.state["UserProfile"] as JSONObject
        appStore.dispatch(assertEquals("Should change property correctly","r5",state["default_room"].toString()))
    }

    @Test
    fun update() {

        var default_profile_img_path = System.getProperty("user.dir")+"/app/src/main/res/drawable/user.png"
        var updated_profile_img_path = System.getProperty("user.dir")+"/app/src/main/res/drawable/profile.png"

        // Populate default user data
        var rooms = JSONArray()
        rooms.add(JSONObject(mapOf("_id" to "r1","name" to "Room 1")))
        rooms.add(JSONObject(mapOf("_id" to "r2","name" to "Room 2")))
        rooms.add(JSONObject(mapOf("_id" to "r3","name" to "Room 3")))

        appStore.dispatch(UserActions.changeProperty("first_name","Bob"))

        appStore.dispatch(UserActions.changeProperty("last_name","Johnson"))
        appStore.dispatch(UserActions.changeProperty("gender", "M"))
        appStore.dispatch(UserActions.changeProperty("birthDate",1234567890))
        appStore.dispatch(UserActions.changeProperty("default_room",""))
        appStore.dispatch(UserProfileActions.changeProperty("rooms",rooms))
        appStore.dispatch(UserProfileActions.changeProperty("default_room",""))
        appStore.dispatch(Actions.changeActivity(AppScreens.USER_PROFILE))
        var stream = FileInputStream(default_profile_img_path)
        appStore.dispatch(UserActions.changeProperty("profileImage",stream.readBytes()))

        val msgCenter = MessageCenter()
        msgCenter.testingMode = true
        msgCenter.onCreate()
        UserProfileActions.messageCenter = msgCenter

        // Test authentication part
        UserProfileActions.update()
        var state = appStore.state["UserProfile"] as JSONObject
        var errors = state["errors"] as JSONObject

        assertEquals("Should return INCORRECT USER if no login",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_USER_ID,
                errors["general"])

        appStore.dispatch(UserActions.changeProperty("user_id","12345"))
        UserProfileActions.update()
        state = appStore.state["UserProfile"] as JSONObject
        errors = state["errors"] as JSONObject
        assertEquals("Should return INCORRECT SESSION if no user session available",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_SESSION_ID,
                errors["general"] as UserProfileActions.UserProfileErrors)

        // Test empty requests
        appStore.dispatch(UserActions.changeProperty("session_id","1123"))
        UserProfileActions.update()
        state = appStore.state["UserProfile"] as JSONObject
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if no data provided",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_EMPTY_REQUEST,
                errors["general"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("first_name"," Bob  "))
        appStore.dispatch(UserProfileActions.changeProperty("last_name","JOHNSON  "))
        appStore.dispatch(UserProfileActions.changeProperty("gender","M"))
        appStore.dispatch(UserProfileActions.changeProperty("birthDate",1234567890))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if no changed data provided",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_EMPTY_REQUEST,
                errors["general"] as UserProfileActions.UserProfileErrors)

        // Test incorrect data on client side
        appStore.dispatch(UserProfileActions.changeProperty("first_name","JAck  "))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject

        assertEquals("Should not submit if no default_room provided",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["default_room"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("default_room","r8"))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if provided default room is incorrect",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE,
                errors["default_room"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("default_room","r2"))
        appStore.dispatch(UserProfileActions.changeProperty("password","12345"))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if confirm password not provided",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_PASSWORDS_SHOULD_MATCH,errors["password"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("confirm_password","12345  "))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if incorrect confirm_password provided",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_PASSWORDS_SHOULD_MATCH,errors["password"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("confirm_password","12345"))
        appStore.dispatch(UserProfileActions.changeProperty("last_name",""""<E8>^\\D^@^@H^A^D\$<83>=<E9><B5>!^@^A^O<85>k<FF><FF><FF><83>=Ե!^@<FF>^O<85>^<FF><FF><FF><80>=õ!^@^@^O<85>Q<FF><FF><FF><80>=<80><B5>!^@^@^O<85>D<FF><FF><FF><E8><EF>^S^@^@<E8><EA>;^@^@<E8>5^S^@^@<E9>0<FF><FF><FF>A<8B>^T\$<85><D2>t+1<FF><BA>^E^@^@^@<BE><98>gA^@<E8>^G<E9><FF><FF><8B>|\$^PL<89><F2>H<89><C6><E8><98>^]^@^@A<83><K^O<84><FD><FE><FF><FF>L<89><EF><E8>E<EA><FF><FF><85><C0>^O<85>8^A^@^@<E8><98>^S^@^@<80>=^W<B5>!\n""" +
                """^@^@^O<85>^W^A^@^@<8B>^EM<B5>!^@<85><C0>t^M<80>=6<B5>!^@^@^O<84><D3>^@^@^@<80>=^U<B5>!^@^@^O<85>(^A^@^@<BA>^E^@^@^@1<FF><BE><C2>gA^@<E8><96><E8><FF><FF>H<8B>5ǩ!^@H<89><C3>H<89><C7><E8>\$<EA><FF><FF>H<89><DF><E8><9C><E8><FF><FF>H<8B>=<AD><A9>!^@H^A^E<B6><B3>!^@H<8B>G(H;G0^O<83><86>^C^@^@H<8D>P^AH<89>W(<C6>^@ L<8B>^E<BE><B4>!^@<"))"""))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if incorrect value provided for field",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE, errors["last_name"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("last_name"," JOHNSON   "))
        appStore.dispatch(UserProfileActions.changeProperty("birthDate",""""<E8>^\\D^@^@H^A^D\$<83>=<E9><B5>!^@^A^O<85>k<FF><FF><FF><83>=Ե!^@<FF>^O<85>^<FF><FF><FF><80>=õ!^@^@^O<85>Q<FF><FF><FF><80>=<80><B5>!^@^@^O<85>D<FF><FF><FF><E8><EF>^S^@^@<E8><EA>;^@^@<E8>5^S^@^@<E9>0<FF><FF><FF>A<8B>^T\$<85><D2>t+1<FF><BA>^E^@^@^@<BE><98>gA^@<E8>^G<E9><FF><FF><8B>|\$^PL<89><F2>H<89><C6><E8><98>^]^@^@A<83><K^O<84><FD><FE><FF><FF>L<89><EF><E8>E<EA><FF><FF><85><C0>^O<85>8^A^@^@<E8><98>^S^@^@<80>=^W<B5>!\n""" +
                """^@^@^O<85>^W^A^@^@<8B>^EM<B5>!^@<85><C0>t^M<80>=6<B5>!^@^@^O<84><D3>^@^@^@<80>=^U<B5>!^@^@^O<85>(^A^@^@<BA>^E^@^@^@1<FF><BE><C2>gA^@<E8><96><E8><FF><FF>H<8B>5ǩ!^@H<89><C3>H<89><C7><E8>\$<EA><FF><FF>H<89><DF><E8><9C><E8><FF><FF>H<8B>=<AD><A9>!^@H^A^E<B6><B3>!^@H<8B>G(H;G0^O<83><86>^C^@^@H<8D>P^AH<89>W(<C6>^@ L<8B>^E<BE><B4>!^@<"))"""))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if garbage provided as birthDate value",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE,
                errors["birthDate"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserProfileActions.changeProperty("birthDate",9999999999))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not submit if incorrect birthDate provided",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE,
                errors["birthDate"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserActions.changeProperty("default_room","r2"))
        appStore.dispatch(UserProfileActions.changeProperty("default_room",""))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertNull("Should not check empty default_room if User already have default_room",errors["default_room"])

        // Test request format on client side
        appStore.dispatch(UserProfileActions.changeProperty("first_name"," JAck  "))
        appStore.dispatch(UserProfileActions.changeProperty("last_name","JOHNSON  "))
        appStore.dispatch(UserActions.changeProperty("default_room","r3"))
        appStore.dispatch(UserProfileActions.changeProperty("gender","M"))
        appStore.dispatch(UserProfileActions.changeProperty("birthDate",1233445670))
        UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return connection error, if not connected to server",UserProfileActions.UserProfileErrors.RESULT_ERROR_CONNECTION_ERROR,
                errors["general"] as UserProfileActions.UserProfileErrors)

        msgCenter.connected = true
        var result = UserProfileActions.update()
        state = getStateOf("UserProfile")!!
        var userState = getStateOf("User")!!
        assertNull("Should not sent properties, which did not change",result["last_name"])
        assertEquals("Should transform property values to correct format when possible before sending","Jack", result["first_name"].toString())
        assertNotNull("Should send correct request_id",result["request_id"])
        assertEquals("Should send correct user_id",userState["user_id"].toString(),result["user_id"].toString())
        assertEquals("Should send correct session_id",userState["session_id"].toString(),result["session_id"].toString())
        assertNotNull("Should contain sender object",result["sender"])
        assertTrue("Sender object should implement MessageCenterResponseReceiver interface",result["sender"] is MessageCenterResponseReceiver)

        assertEquals("Should send correct action","update_user",result["action"].toString())
        assertTrue("Should show progress indicator",state["show_progress_indicator"] as Boolean)
        assertEquals("Should add profile update request to requests queue",1,msgCenter.getRequestsQueueLength())
        UserProfileActions.update();
        assertEquals("Should not run request if previous did not finish",1,msgCenter.getRequestsQueueLength())
        Thread.sleep(1000)
        assertEquals("Should send request and remove it from requests queue",0,msgCenter.getRequestsQueueLength())
        assertEquals("Should send request and move it to pending responses queue",1,msgCenter.getPendingResponsesQueueLength())

        stream = FileInputStream(default_profile_img_path)
        appStore.dispatch(UserProfileActions.changeProperty("profileImage",stream.readBytes()))
        result = UserProfileActions.update()
        assertNull("Should not send image binary data and it checksum if it does not change",result["profile_image_checksum"])

        // Force clean requests and pending responses queues
        msgCenter.PENDING_RESPONSES_QUEUE_TIMEOUT = 1
        Thread.sleep(2000)
        msgCenter.PENDING_RESPONSES_QUEUE_TIMEOUT = 5000

        stream = FileInputStream(updated_profile_img_path)
        var img = stream.readBytes()
        var checksumEngine = CRC32()
        checksumEngine.update(img)
        var updated_img_checksum = checksumEngine.value
        Thread.sleep(1000)
        appStore.dispatch(UserProfileActions.changeProperty("profileImage",img))
        Thread.sleep(2000)
        appStore.dispatch(UserProfileActions.changeProperty("show_progress_indicator",false))
        result = UserProfileActions.update()
        assertNotNull("Should post updated profile image to queue",result["profileImage"])
        assertEquals("Should send correct checksum of updated profile image",
                updated_img_checksum,
                result["profile_image_checksum"].toString().toLong())
        Thread.sleep(2000)
        var pendingRequest = msgCenter.getPendingResponsesQueue().iterator().next().value as HashMap<String,Any>
        var request_id = pendingRequest.get("request_id") as String
        assertEquals("Request id in pending responses queue should be the same as sent request_id",request_id,result["request_id"].toString())

        // Testing WebSocket server error responses
        var msg = """{"request_id":"bla-bla-bla","status":"ok","status_code":"RESULT_OK","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        assertTrue("Should not react on response with incorrect request_id",state["show_progress_indicator"] as Boolean)

        msg = """{"request_id":"{${request_id}","status":"ok","status_code":"RESULT_OK","action":"hack-it"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        assertTrue("Should not react on response with incorrect action",state["show_progress_indicator"] as Boolean)

        msg = """{"request_id":"${request_id}","status":"not_ok","status_code":"RESULT_OK","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        assertTrue("Should not react on response with incorrect status",state["show_progress_indicator"] as Boolean)
        msg = """{"request_id":"${request_id}","status":"error","status_code":"INTERNAL_ERROR","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertFalse("Should hide progress indicator before show error message",state["show_progress_indicator"] as Boolean)
        assertEquals("Should remove request from Pending Responses queue",0,msgCenter.getPendingResponsesQueueLength())
        assertEquals("Should return system error in correct format",UserProfileActions.UserProfileErrors.INTERNAL_ERROR,
                errors["general"] as UserProfileActions.UserProfileErrors)
        result = UserProfileActions.update()
        request_id = result["request_id"].toString()
        Thread.sleep(1000)
        msg = """{"request_id":"${request_id}","status":"error","status_code":"AUTHENTICATION_ERROR","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return authentication error in correct format",UserProfileActions.UserProfileErrors.AUTHENTICATION_ERROR,
                errors["general"] as UserProfileActions.UserProfileErrors)
        result = UserProfileActions.update()
        Thread.sleep(1000)
        request_id = result["request_id"].toString()

        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_FIELD_IS_EMPTY","action":"update_user","field":"first_name"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return field is empty error in correct format",UserProfileActions.UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["first_name"] as UserProfileActions.UserProfileErrors)
        result = UserProfileActions.update()
        Thread.sleep(2000)
        request_id = result["request_id"].toString()

        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_INCORRECT_FIELD_VALUE","action":"update_user","field":"last_name"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return incorrect field error in correct format",UserProfileActions.UserProfileErrors.RESULT_ERROR_INCORRECT_FIELD_VALUE,
                errors["last_name"] as UserProfileActions.UserProfileErrors)

        result = UserProfileActions.update()
        Thread.sleep(2000)
        request_id = result["request_id"].toString()
        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_INCORRECT_FIELD_VALUE","action":"update_user","field":"fakefld"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return unknown error if field for which error is intended is not found",UserProfileActions.UserProfileErrors.RESULT_ERROR_UNKNOWN,
                errors["general"] as UserProfileActions.UserProfileErrors)
        result = UserProfileActions.update()
        Thread.sleep(2000)
        request_id = result["request_id"].toString()

        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_IMAGE_UPLOAD","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return profile image upload error in correct format",UserProfileActions.UserProfileErrors.RESULT_ERROR_IMAGE_UPLOAD,
                errors["general"] as UserProfileActions.UserProfileErrors)
        result = UserProfileActions.update()
        Thread.sleep(2000)
        request_id = result["request_id"].toString()

        msg = """{"request_id":"${request_id}","status":"error","status_code":"RESULT_ERROR_PASSWORDS_SHOULD_MATCH","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        var globalState = appStore.state
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should return passwords should match error in correct format",UserProfileActions.UserProfileErrors.RESULT_ERROR_PASSWORDS_SHOULD_MATCH,
                errors["password"] as UserProfileActions.UserProfileErrors)
        assertEquals("Should stay on user profile screen",AppScreens.USER_PROFILE,globalState["current_activity"] as AppScreens)
        result = UserProfileActions.update()
        Thread.sleep(2000)
        request_id = result["request_id"].toString()

        // Test successful response
        msg = """{"request_id":"${request_id}","status":"ok","status_code":"RESULT_OK","action":"update_user"}"""
        msgCenter.messageListener.onTextMessage(null,msg)
        globalState = appStore.state
        userState = getStateOf("User")!!
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not contain any errors",0,errors.size)
        assertFalse("Should hide progress indicator", state["show_progress_indicator"] as Boolean)
        assertEquals("Should move to chat screen",AppScreens.CHAT, globalState["current_activity"] as AppScreens)
        assertEquals("Should set correct first_name value to User","Jack", userState["first_name"].toString())
        assertEquals("Should set correct birthDate to User",1233445670, userState["birthDate"].toString().toInt())
        assertEquals("Should set correct default_room to User","r3", userState["default_room"].toString())
        assertEquals("Should set empty password","", state["password"].toString())
        assertEquals("Should set empty confirm_password","", state["confirm_password"].toString())
        checksumEngine.reset()
        checksumEngine.update(userState["profileImage"] as ByteArray)
        assertEquals("Should set correct updated profile image", updated_img_checksum,checksumEngine.value)
    }

    @Test
    fun cancel() {
        // Populate default user data
        var default_profile_img_path = System.getProperty("user.dir")+"/app/src/main/res/drawable/user.png"

        var rooms = JSONArray()
        rooms.add(JSONObject(mapOf("_id" to "r1","name" to "Room 1")))
        rooms.add(JSONObject(mapOf("_id" to "r2","name" to "Room 2")))
        rooms.add(JSONObject(mapOf("_id" to "r3","name" to "Room 3")))

        appStore.dispatch(UserActions.changeProperty("first_name","Bob"))
        appStore.dispatch(UserActions.changeProperty("last_name","Johnson"))
        appStore.dispatch(UserActions.changeProperty("gender", "M"))
        appStore.dispatch(UserActions.changeProperty("birthDate",1234567890))
        appStore.dispatch(UserProfileActions.changeProperty("rooms",rooms))
        var stream = FileInputStream(default_profile_img_path)
        var img = stream.readBytes()
        appStore.dispatch(UserActions.changeProperty("profileImage",img))

        appStore.dispatch(Actions.changeActivity(AppScreens.USER_PROFILE))

        // Test error responses
        UserProfileActions.cancel()
        var state = getStateOf("UserProfile")!!
        var errors = state["errors"] as JSONObject
        assertEquals("Should not cancel if default room is not set for User",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["default_room"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserActions.changeProperty("default_room","r8"))
        UserProfileActions.cancel()
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject
        assertEquals("Should not cancel if default room is not set for User",
                UserProfileActions.UserProfileErrors.RESULT_ERROR_FIELD_IS_EMPTY,
                errors["default_room"] as UserProfileActions.UserProfileErrors)

        appStore.dispatch(UserActions.changeProperty("default_room","r2"))
        UserProfileActions.cancel()
        var globalState = appStore.state
        state = getStateOf("UserProfile")!!
        errors = state["errors"] as JSONObject

        // Test success response
        assertEquals("Should move to CHAT screen if success",AppScreens.CHAT,
                globalState["current_activity"] as AppScreens)
        assertEquals("Should not have any errors",0,errors.size)
        assertEquals("Should reset first_name to default","Bob",state["first_name"].toString())
        assertEquals("Should reset last_name to default","Johnson",state["last_name"].toString())
        assertEquals("Should reset gender to default","M",state["gender"].toString())
        assertEquals("Should reset birthDate to default",1234567890,state["birthDate"].toString().toInt())
        assertEquals("Should reset default_room to default","r2",state["default_room"].toString())
        assertEquals("Should rest password to blank","",state["password"].toString())
        assertEquals("Should reset confirm_password to blank","",state["confirm_password"].toString())
        assertEquals("Should reset profile image to default",img,state["profileImage"] as ByteArray)
    }
}