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

}