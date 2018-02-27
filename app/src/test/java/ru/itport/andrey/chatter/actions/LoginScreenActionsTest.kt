/**
 * Created by Andrey Germanov on 2/28/18.
 */

package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore

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
}