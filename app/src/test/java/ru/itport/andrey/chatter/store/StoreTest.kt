/**
 * Created by andrey on 2/27/18.
 */

package ru.itport.andrey.chatter.store

import org.json.simple.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StoreTest {

    @Test
    fun checkAppStateStructure() {
        assertEquals("Should return initial screen","LoginForm",appState["current_activity"])
        val loginForm = appState["LoginForm"] as JSONObject
        assertEquals("Should be able to read on second or more levels of hierarchy",LoginFormMode.LOGIN,loginForm["mode"])
        loginForm["login"] = "test"
        val loginForm2 = appState["LoginForm"] as JSONObject
        assertEquals("Should be able to change values and read them from different places","test",loginForm2["login"])
    }

    @Test
    fun loadStateFromStore() {
        val state = appStore.state as JSONObject
        assertTrue("Initial state is JSONObject",state is JSONObject)
        val loginForm = state["LoginForm"] as JSONObject
        assertEquals("Initial state has correct inital values",LoginFormMode.LOGIN,loginForm["mode"] as LoginFormMode)
    }

    @Test
    fun checkDefaultLoginScreenState() {
        val state = appStore.state["LoginForm"] as JSONObject
        assertEquals("Should contain empty 'login' field","",state["login"].toString())
        assertEquals("Should contain empty 'password' field","",state["password"].toString())
        assertEquals("Should contain empty 'confirm_password' field","",state["confirm_password"].toString())
        assertEquals("Should contain empty 'email' field","",state["email"].toString())
        assertTrue("Should contain 'errors' JSONObject",state["errors"] is JSONObject)
        assertEquals("Should be on 'Login' page by default",LoginFormMode.LOGIN,state["mode"] as LoginFormMode)
        assertEquals("Should contain empty 'popup_message' field","",state["popup_message"].toString())
        assertFalse("Should contain show_progress_indicator which is disabled",state["show_progress_indicator"] as Boolean)
    }

    @Test
    fun checkDefaultUserProfileScreenState() {
        val state = appStore.state["UserProfile"] as JSONObject
        assertEquals("Should contain empty 'login' field","",state["login"].toString())
        assertEquals("Should contain empty 'password' field","",state["password"].toString())
        assertEquals("Should contain empty 'confirm_password' field","",state["confirm_password"].toString())
        assertEquals("Should contain empty 'email' field","",state["email"].toString())
        assertTrue("Should contain 'errors' JSONObject",state["errors"] is JSONObject)
        assertEquals("Should contain empty 'first_name' field","",state["first_name"].toString())
        assertEquals("Should contain empty 'last_name' field","",state["last_name"].toString())
        assertEquals("Should contain 'gender' field which is set to 'M'","",state["gender"].toString())
        assertEquals("Should contain 'birthDate' field defaulted to current date",System.currentTimeMillis()/1000, state["birthDate"].toString().toInt())
        assertEquals("Should contain empty 'default_room' field","",state["default_room"].toString())
        assertEquals("Should contain empty 'popup_message' field","",state["popup_message"].toString())
        assertFalse("Should contain show_progress_indicator which is disabled",state["show_progress_indicator"] as Boolean)
        assertFalse("Should contain show_date_picker_dialog which is disabled",state["show_date_picker_dialog"] as Boolean)
    }


}