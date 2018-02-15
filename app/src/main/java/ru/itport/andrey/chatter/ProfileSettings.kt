package ru.itport.andrey.chatter

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_profile_settings.*
import ru.itport.andrey.chatter.core.MessageService
import ru.itport.andrey.chatter.models.User
import ru.itport.andrey.chatter.utils.getRealPathFromURI
import ru.itport.andrey.chatter.utils.showAlertDialog
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileSettings : AppCompatActivity(), DatePicker.OnDateChangedListener, View.OnClickListener {

    lateinit var profileImageView: ImageView
    lateinit var firstNameField: EditText
    lateinit var lastNameField: EditText
    lateinit var maleRadioButton: RadioButton
    lateinit var femaleRadioButton: RadioButton
    lateinit var dateOfBirthField: DatePicker
    lateinit var defaultRoomField: Spinner
    var mCurrentPhotoPath = ""
    var mCurrentPhotoRequest = 1
    var mCurrentGalleryRequest = 2

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
        User.context = this
        setContentView(R.layout.activity_profile_settings)
        this.profileImageView = findViewById(R.id.profileImage)
        this.firstNameField = findViewById(R.id.firstNameField)
        this.lastNameField = findViewById(R.id.lastNameField)
        this.maleRadioButton = findViewById(R.id.maleRadioButton)
        this.femaleRadioButton = findViewById(R.id.femaleRadioButton)
        this.dateOfBirthField = findViewById(R.id.birthDateField)
        this.defaultRoomField = findViewById(R.id.defaultRoomField)

        this.firstNameField.setText(User.firstName,TextView.BufferType.EDITABLE)
        this.lastNameField.setText(User.lastName,TextView.BufferType.EDITABLE)
        if (User.gender == "M") {
            this.maleRadioButton.isChecked = true
        }
        if (User.gender == "F") {
            this.femaleRadioButton.isChecked = false
        }
        var dateOfBirth = Calendar.getInstance()

        dateOfBirth.timeInMillis = User.birthDate.toLong()*1000;

        this.dateOfBirthField.init(dateOfBirth.get(Calendar.YEAR),dateOfBirth.get(Calendar.MONTH),dateOfBirth.get(Calendar.DAY_OF_MONTH),this)
        this.dateOfBirthField.calendarViewShown = false
        this.defaultRoomField.adapter = ArrayAdapter<String>(this,R.layout.support_simple_spinner_dropdown_item,User.rooms)
        this.defaultRoomField.setSelection(User.rooms.indexOf(User.defaultRoom))
        this.profileImageView.setImageBitmap(User.image)
        this.profileImageView.setOnClickListener(this)
        this.saveProfileChangesBtn.setOnClickListener(this)

        val serviceIntent = Intent(this,MessageService::class.java)
        bindService(serviceIntent,mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDateChanged(p0: DatePicker?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onClick(view:View) {
        when (view.id) {
            R.id.profileImage -> {
                var dialog = AlertDialog.Builder(this)
                dialog.setTitle("Image source").setNeutralButton("Camera",{ dialogInterface: DialogInterface, i: Int ->
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                    } catch (e:IOException) {

                    }
                    if (photoFile != null) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        val photoURI = FileProvider.getUriForFile(this,"ru.itport.andrey.chatter.fileprovider",photoFile)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI)
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivityForResult(intent, mCurrentPhotoRequest)
                        }
                    }
                }).setPositiveButton("Gallery", { dialogInterface: DialogInterface, i: Int ->
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.setType("image/*")
                    startActivityForResult(intent,mCurrentGalleryRequest)
                }).create().show()
            }
            R.id.saveProfileChangesBtn -> {
                var bitmap: Bitmap? = null
                if (User.imageChanged) {
                   bitmap = User.image
                }
                User.updateProfile(mapOf("first_name" to this.firstNameField.text.toString(),
                        "last_name" to this.lastNameField.text.toString(),
                        "gender" to if (this.maleRadioButton.isChecked)  "M" else if (this.femaleRadioButton.isChecked) "F" else "",
                        "birthDate" to (GregorianCalendar(this.birthDateField.year,this.birthDateField.month,this.birthDateField.dayOfMonth).timeInMillis/1000).toString(),
                        "default_room" to this.defaultRoomField.selectedItem.toString()
                        ), bitmap,this)
            }
        }
    }

    override protected fun onActivityResult(requestCode:Int, resultCode: Int, data:Intent) {
        if (resultCode == RESULT_OK) {
            var bitmap: Bitmap
            if (requestCode == mCurrentGalleryRequest) {
               bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,data.data)
            } else {
                bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath)

            }
            User.image = bitmap
            profileImageView.setImageBitmap(bitmap)
            User.imageChanged = true
        }
    }

    fun createImageFile() : File {
        val filename = UUID.randomUUID().toString()
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(filename,".jpg",storageDir)
        mCurrentPhotoPath = image.absolutePath
        return image
    }



}
