/**
 * Created by Andrey Germanov on 3/10/18.
 */
package ru.itport.andrey.chatter.views

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Window
import android.widget.LinearLayout
import org.json.simple.JSONObject
import ru.itport.andrey.chatter.actions.LoginScreenActions
import ru.itport.andrey.chatter.core.MessageCenter
import ru.itport.andrey.chatter.store.appStore
import trikita.anvil.DSL
import trikita.anvil.RenderableView

/**
 * Parent class of all application screens. Contains functions related to WebSocket
 * service connection and state management
**/
open class BaseScreen : Activity() {

    /**
     * The subarray of values of Activity state items from global
     * Redux Application state
     */
    lateinit var state: JSONObject

    /**
     * Link to full application state
     */
    lateinit var globalState: JSONObject

    /**
     * Link to progress bar dialog, which is showing in current moment
     */
    var progressBarDialog: AlertDialog? = null


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
    val messageCenterConnection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
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
        override fun run() {}
    }

    /**
     * Function runs when activity starts or restarts
     * It draws view of current activity and subscribes to application store,
     * which provides notifications about any changes in application state,
     * which requires to update current view
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(drawView())
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
     * Function used to draw view.
     * It provides resulted view object to setContentView function
     * @return Anvil RenderableView object
     */
    open fun drawView() : RenderableView {
        return object: RenderableView(this) {
            override fun view() {}
        }
    }
}
