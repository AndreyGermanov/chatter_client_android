/**
 * Created by Andrey Germanov on 2/28/18.
 */

package ru.itport.andrey.chatter.actions

import org.json.simple.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.itport.andrey.chatter.reducers.LoginScreenReducer
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

        return
        // Server side response handling
        appStore.dispatch(LoginScreenActions.register())
        state = appStore.state["LoginForm"] as JSONObject
        errors = state["errors"] as JSONObject
        assertNotNull("Should contain general error",errors["general"])
        assertEquals("General error should be related to activation email",
                LoginScreenActions.LoginScreenRegisterErrors.RESULT_ERROR_ACTIVATION_EMAIL,
                errors["login"] as LoginScreenActions.LoginScreenRegisterErrors)


    }
}