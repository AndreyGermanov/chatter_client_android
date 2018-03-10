/**
* Created by Andrey Germanov on 3/10/18.
*/
package ru.itport.andrey.chatter.views

import android.content.Intent
import android.graphics.Color
import android.text.InputType
import android.view.Window
import trikita.anvil.DSL.*
import trikita.anvil.BaseDSL.WRAP
import android.widget.LinearLayout
import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.actions.SmartEnum
import ru.itport.andrey.chatter.store.AppScreens
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.utils.showAlertDialog
import ru.itport.andrey.chatter.utils.showProgressBar
import trikita.anvil.Anvil

import trikita.anvil.BaseDSL
import trikita.anvil.RenderableView

/**
 * Activity which displays first screen of application with Login/Register
 * form
 *
 */
class LoginScreen : BaseScreen() {

    /**
     * Constructor of object
     */
    init {
        globalState = appStore.getState() as JSONObject
        state = globalState["LoginForm"] as JSONObject
    }

    /**
     * Function updates user interface on application state update
     */
    inner class UpdateUI: Runnable {
        override fun run() {
            globalState = appStore.getState() as JSONObject
            state = globalState["LoginForm"] as JSONObject
            if (state["errors"] != null) {
                val errors = state["errors"] as JSONObject
                if (errors["general"] != null) {
                    if (errors["general"] is SmartEnum) {
                        val generalError = errors["general"] as SmartEnum
                        showAlertDialog("Alert", generalError.getMessage(), this@LoginScreen)
                    }
                    errors["general"] = null
                    appStore.dispatch(LoginScreenActions.changeProperty("errors", errors))
                }
                if (state["show_progress_indicator"] as Boolean && progressBarDialog==null) {
                    progressBarDialog = showProgressBar(this@LoginScreen)
                    progressBarDialog!!.show()
                } else if (!(state["show_progress_indicator"] as Boolean) && progressBarDialog != null) {
                    progressBarDialog!!.cancel()
                    progressBarDialog = null
                }
                if (globalState["current_activity"] as AppScreens != AppScreens.LOGIN_FORM) {
                    var intent = Intent()
                    when (globalState["current_activity"] as AppScreens) {
                        AppScreens.USER_PROFILE -> intent = Intent(this@LoginScreen,UserProfileScreen::class.java)
                        AppScreens.CHAT -> intent = Intent(this@LoginScreen,ChatScreen::class.java)
                    }
                    startActivity(intent)
                }
            }
            Anvil.render()
        }
    }

    /**
     * Function used to draw layout of navigation switch between login and
     * register form
     */
    fun drawNavigation() {
        linearLayout {
            gravity(BaseDSL.CENTER_HORIZONTAL)
            size(MATCH, WRAP)
            orientation(LinearLayout.HORIZONTAL)
            button {
                text("Login")
                if (state["mode"] == LoginFormMode.LOGIN) {
                    backgroundColor(Color.BLUE)
                    textColor(Color.WHITE)
                } else {
                    backgroundColor(Color.WHITE)
                    textColor(Color.BLACK)
                }
                onClick { v ->
                    appStore.dispatch(LoginScreenActions.switchMode(LoginFormMode.LOGIN))
                }
            }
            button {
                text("Register")
                if (state["mode"] == LoginFormMode.REGISTER) {
                    backgroundColor(Color.BLUE)
                    textColor(Color.WHITE)
                } else {
                    backgroundColor(Color.WHITE)
                    textColor(Color.BLACK)
                }
                onClick { v ->
                    appStore.dispatch(LoginScreenActions.switchMode(LoginFormMode.REGISTER))
                }
            }
        }
    }

    /**
     * Function used to draw Login Form
     */
    fun drawLoginForm() {
        linearLayout {
            orientation(LinearLayout.VERTICAL)
            val errors = state["errors"] as JSONObject
            visibility(state["mode"] as LoginFormMode == LoginFormMode.LOGIN)
            tableLayout {
                size(MATCH,MATCH)
                tableRow {
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Login")
                    }
                    editText {
                        text(state["login"].toString())
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("login",text))}
                    }
                }
                if (errors["login"]!=null && errors["login"] is LoginScreenActions.LoginScreenLoginErrors) {
                    val msg = (errors["login"] as LoginScreenActions.LoginScreenLoginErrors).getMessage()
                    tableRow {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            text(msg)
                            textColor(Color.RED)
                        }
                    }
                }
                tableRow {
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Password")
                        inputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                    }
                    editText {
                        text(state["password"].toString())
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("password",text))}
                    }
                }
                if (errors["password"]!=null && errors["password"] is LoginScreenActions.LoginScreenLoginErrors) {
                    val msg = (errors["password"] as LoginScreenActions.LoginScreenLoginErrors).getMessage()
                    println(msg)
                    println("printed")
                    tableRow {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            text(msg)
                            textColor(Color.RED)
                        }
                    }
                }
            }
            tableLayout {
                size(MATCH,WRAP)
                button {
                    size(MATCH, WRAP)
                    text("LOGIN")
                    onClick { v ->
                        LoginScreenActions.login()
                    }
                }
            }
        }
    }

    /**
     * Function used to draw Register form
     */
    fun drawRegisterForm() {
        linearLayout {
            size(MATCH,MATCH)
            visibility(state["mode"] as LoginFormMode == LoginFormMode.REGISTER)
            orientation(LinearLayout.VERTICAL)
            val errors = state["errors"] as JSONObject
            tableLayout {
                tableRow {
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Email")
                    }
                    editText {
                        text(state["email"].toString())
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("email",text))}
                    }
                }
                if (errors["email"]!=null && errors["email"] is LoginScreenActions.LoginScreenRegisterErrors) {
                    val msg = (errors["email"] as LoginScreenActions.LoginScreenRegisterErrors).getMessage()
                    tableRow {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            text(msg)
                            textColor(Color.RED)
                        }
                    }
                }
                tableRow {
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Login")
                    }
                    editText {
                        text(state["login"].toString())
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("login",text))}
                    }
                }
                if (errors["login"]!=null && errors["login"] is LoginScreenActions.LoginScreenRegisterErrors) {
                    val msg = (errors["login"] as LoginScreenActions.LoginScreenRegisterErrors).getMessage()
                    tableRow {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            text(msg)
                            textColor(Color.RED)
                        }
                    }
                }
                tableRow {
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Password")
                    }
                    editText {
                        text(state["password"].toString())
                        inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("password",text))}
                    }
                }
                if (errors["password"]!=null && errors["password"] is LoginScreenActions.LoginScreenRegisterErrors) {
                    val msg = (errors["password"] as LoginScreenActions.LoginScreenRegisterErrors).getMessage()
                    tableRow {
                        orientation(LinearLayout.HORIZONTAL)
                        textView {
                            text(msg)
                            textColor(Color.RED)
                        }
                    }
                }
                tableRow {
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Confirm password")
                    }
                    editText {
                        text(state["confirm_password"].toString())
                        inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("confirm_password",text))}
                    }
                }
            }
            tableLayout {
                size(MATCH,WRAP)
                button {
                    size(MATCH, WRAP)
                    text("REGISTER")
                    onClick { v ->
                        LoginScreenActions.register()
                    }
                }
            }
        }
    }

    /**
     * Function used to draw view.
     * It provides resulted view object to setContentView function
     * @return Anvil RenderableView object
     */
    override fun drawView() : RenderableView {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        appStore.subscribe {
            this.runOnUiThread(UpdateUI())
        }
        return object: RenderableView(this) {
            override fun view() {
                linearLayout {
                    size(MATCH, MATCH)
                    padding(dip(8))
                    orientation(LinearLayout.VERTICAL)
                    drawNavigation()
                    drawLoginForm()
                    drawRegisterForm()
                }
            }
        }
    }
}
