package ru.itport.andrey.chatter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.RadioGroup
import ru.itport.andrey.chatter.core.MessageService
import ru.itport.andrey.chatter.models.User
import ru.itport.andrey.chatter.utils.showAlertDialog

class MainActivity : AppCompatActivity(),LoginScreen.OnFragmentInteractionListener, signupScreen.OnFragmentInteractionListener, RadioGroup.OnCheckedChangeListener {

    lateinit var mService: MessageService

    private var mConnection = object: ServiceConnection {

        var isBound = false;

        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            if (service!=null) {
                val binder = service as MessageService.ServiceBinder
                mService = binder.getService()
                isBound = true
            }
        }
        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val loginScreen = LoginScreen()
        loginScreen.arguments = intent.extras
        supportFragmentManager.beginTransaction().replace(R.id.signinScroll,loginScreen).commit()
        var radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener(this)
        val serviceIntent = Intent(this,MessageService::class.java)
        bindService(serviceIntent,mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onRegisterButtonClick() {
        var signupScreen = supportFragmentManager.fragments[0] as signupScreen
        var errors = false

        User.register(mapOf("login" to signupScreen.signupLoginField.text.toString(),
                "password" to signupScreen.signupPasswordField.text.toString(),
                "password_again" to signupScreen.signupPasswordAgainField.text.toString(),
                "email" to signupScreen.signupEmailField.text.toString()
        ),this)
    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
        when (p1) {
            R.id.signinSwitchBtn -> {
                val signinScreen = LoginScreen()
                supportFragmentManager.beginTransaction().replace(R.id.signinScroll,signinScreen).commit()
            }
            R.id.signupSwitchBtn -> {
                val signupScreen = signupScreen()
                supportFragmentManager.beginTransaction().replace(R.id.signinScroll, signupScreen).commit()
            }
        }
    }
}
