package com.smeiskaudio.collage

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.squareup.picasso.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

//    private lateinit var imageButton1: ImageButton  // replace with lists for the 4 photo collage
    private lateinit var imageButtons: List<ImageButton> // note the list of android classes...
    private lateinit var mainView: View

//    private var newPhotoPath: String? = null
//    private var visibleImagePath: String? = null
    private var photoPaths: ArrayList<String?> = arrayListOf(null, null, null, null)
    // arrayList is used for photoPaths instead of a kotlin List because ArrayList can be saved in
    // a savedInstanceState() function.  It is a mutable list.
    private var whichImageIndex: Int? = null
    private var currentPhotoPath: String? = null

//    private val NEW_PHOTO_PATH_KEY = "com.smeiskaudio.collage.NEW_PHOTO_PATH_KEY"
//    private val VISIBLE_IMAGE_PATH_KEY = "com.smeiskaudio.collage.VISIBLE_IMAGE_PATH_KEY"

    private val PHOTO_PATH_LIST_KEY = "new photo path key"
    private val IMAGE_INDEX_KEY = "image index key"
    private val CURRENT_PHOTO_PATH_KEY = "current photo path key"

    private val cameraActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result -> handleImage(result)
        }


    private fun handleImage(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, user took picture, image at $currentPhotoPath")
                whichImageIndex?.let { index -> photoPaths[index] = currentPhotoPath } // keep track of what belongs at which image view.
            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result canceled, no picture taken")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        whichImageIndex = savedInstanceState?.getInt(IMAGE_INDEX_KEY)
        currentPhotoPath = savedInstanceState?.getString(CURRENT_PHOTO_PATH_KEY)
        photoPaths = savedInstanceState?.getStringArrayList(PHOTO_PATH_LIST_KEY)
            ?: arrayListOf(null, null, null, null)

//        newPhotoPath = savedInstanceState?.getString(NEW_PHOTO_PATH_KEY)
//        visibleImagePath = savedInstanceState?.getString(VISIBLE_IMAGE_PATH_KEY)

        mainView = findViewById(R.id.content)

        imageButtons = listOf(
            findViewById(R.id.imageButton1),
            findViewById(R.id.imageButton2),
            findViewById(R.id.imageButton3),
            findViewById(R.id.imageButton4)
        )
//        imageButton1 = findViewById(R.id.imageButton1)
//        imageButton1.setOnClickListener {
//            takePicture()
//        }
        for (imageButton in imageButtons) {
            imageButton.setOnClickListener {ib ->
                takePictureFor(ib as ImageButton) // need to specify the lambda here
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(PHOTO_PATH_LIST_KEY, photoPaths)
        outState.putString(CURRENT_PHOTO_PATH_KEY, currentPhotoPath)
        whichImageIndex?.let { index -> outState.putInt(IMAGE_INDEX_KEY, index) }
    }

    private fun takePictureFor(imageButton: ImageButton) {

        val index = imageButtons.indexOf(imageButton)
        whichImageIndex = index

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val (photoFile, photoFilePath) = createImageFile()
        if (photoFile != null) {
            currentPhotoPath = photoFilePath
            val photoUri = FileProvider.getUriForFile( // reference work to tell camera app where to save file
                this,
                "com.smeiskaudio.collage.fileprovider",
                photoFile
            )
            Log.d(TAG, "$photoUri\n$photoFilePath")
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraActivityLauncher.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): Pair<File?, String?> {
        return try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "COLLAGE_$dateTime"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(imageFileName, ".jpg", storageDir)
            val filePath = file.absolutePath
            file to filePath
        } catch (ex: IOException) {
            null to null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "on window focus changed $hasFocus visible image at $currentPhotoPath")
        if (hasFocus) {
//            visibleImagePath?.let { imagePath ->
//                loadImage(imageButton1, imagePath) }

            // *** The below zip function did not work, no images loaded ***
                /*imageButtons.zip(photoPaths) {imageButton, photoPath -> {
                 photoPath?.let {
                     loadImage(imageButton, photoPath)
                 }
                }}*/
            for (index in imageButtons.indices) { // for (index in 0 until imageButtons.size)
                val photoPath = photoPaths[index]
                val imageButton = imageButtons[index]
                if (photoPath != null) {
                    loadImage(imageButton, photoPath)
                }
            }
        }
    }

    private fun loadImage(imageButton: ImageButton, imagePath: String) {
        // could code this ourselves, but lets use libraries available to us instead.
        Picasso.get()
            .load(File(imagePath))// need to convert to File for Picasso to .load.
            .error(android.R.drawable.stat_notify_error)
            .fit()
            .centerCrop()
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "Loaded image $imagePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image $imagePath", e)
                }
            })
    }
}