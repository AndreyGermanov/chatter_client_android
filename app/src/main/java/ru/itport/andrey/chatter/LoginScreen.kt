package ru.itport.andrey.chatter

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.support.annotation.UiThread
import android.support.v4.content.LocalBroadcastManager
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.text.method.TransformationMethod
import android.view.Window
import trikita.anvil.DSL.*
import trikita.anvil.BaseDSL.WRAP
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_login_screen.*
import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.store.LoginFormMode
import ru.itport.andrey.chatter.store.appStore
import ru.itport.andrey.chatter.store.oldAppState
import ru.itport.andrey.chatter.utils.showAlertDialog
import trikita.anvil.Anvil

import trikita.anvil.BaseDSL
import trikita.anvil.RenderableView

/**
 * Activity which displays first screen of application with Login/Register
 * form
 *
 * @property state The subarray of values of Activity state items from global
 * Redux Application state
 */
class LoginScreen : Activity() {

    /**
     * The subarray of values of Activity state items from global
     * Redux Application state
     */
    var state:JSONObject

    init {
        val currentState = appStore.getState() as JSONObject
        state = currentState["LoginForm"] as JSONObject
    }

    /**
     * Link to MessageCenter service, used to exchange messages
     * with server
     */
    lateinit var messageCenter: MessageCenter

    /**
     * Used to check if this activity connected to MessageCenter
     */
    var messageCenterConnected = false

    /**
     * Object which handles connection and disconnection from MessageCenter service
     */
    val messageCenterConnection = object:ServiceConnection {
        override fun onServiceConnected(className:ComponentName,service: IBinder) {
            if (!messageCenterConnected) {
                val binder = service as MessageCenter.LocalBinder
                messageCenter = binder.getService()
                LoginScreenActions.messageCenter = messageCenter
                messageCenterConnected = true
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            messageCenterConnected = false
        }
    }

    /**
     * Function updates user interface on application state update
     */
    inner class UpdateUI: Runnable {
        override fun run() {
            val currentState = appStore.getState() as JSONObject
            state = currentState["LoginForm"] as JSONObject
            if (state["errors"] != null) {
                val errors = state["errors"] as JSONObject
                if (errors["general"] != null) {
                    val generalError = errors["general"] as LoginScreenActions.LoginScreenRegisterErrors
                    showAlertDialog("Alert", generalError.getMessage(), this@LoginScreen)
                    errors["general"] = null
                    appStore.dispatch(LoginScreenActions.changeProperty("errors", errors))
                }
            }
            Anvil.render()
        }
    }

    /**
     * Function runs when activity starts or restarts
     * It draws view of current activity and subscribes to application store,
     * which provides notifications about any changes in application state,
     * which requires to update current view
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(drawView())
        appStore.subscribe {
            this.runOnUiThread(UpdateUI())
        }
    }

    /**
     * Function which runs every time when this screen becomes visible to user
     */
    override fun onStart() {
        super.onStart()
        val intent = Intent(this,MessageCenter::class.java)
        bindService(intent,messageCenterConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Function runs every time when user moves from this screen to other screen
     */
    override fun onStop() {
        super.onStop()
        unbindService(messageCenterConnection)
        messageCenterConnected = false
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
            }
            tableLayout {
                size(MATCH,WRAP)
                button {
                    size(MATCH, WRAP)
                    text("LOGIN")
                    onClick { v ->

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
                    if (state["show_progress_indicator"] as Boolean) {
                        progressBar {
                            indeterminate(true)
                        }
                    }
                    orientation(LinearLayout.HORIZONTAL)
                    textView {
                        text("Email")
                    }
                    editText {
                        text(state["email"].toString())
                        onTextChanged { text -> appStore.dispatch(LoginScreenActions.changeProperty("email",text))}
                    }
                }
                if (errors["email"]!=null) {
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
                if (errors["login"]!=null) {
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
                if (errors["password"]!=null) {
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
    fun drawView() : RenderableView {
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
