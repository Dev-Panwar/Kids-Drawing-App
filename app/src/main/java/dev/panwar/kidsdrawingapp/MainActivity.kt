package dev.panwar.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private  var drawingView:DrawingView?=null //class we have created made an object
    private var mImageButtonCurrentPaint: ImageButton?=null

//    create a variable for the dialog
    var customProgressDialog: Dialog? = null

//    for opening gallery
    val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
         result->
    if (result.resultCode== RESULT_OK && result.data!=null){
        val imageBackGround: ImageView = findViewById(R.id.iv_background)
//setting new BackGround
        imageBackGround.setImageURI(result.data?.data)//result ka data is location,location ka data image...means hum app ko srf path de rahe hain naaki uss image to apne app ne daal rahe hain
    }
}

    val requestPermission:ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permissions ->
        permissions.entries.forEach {
            val permissionName=it.key
            val isGranted=it.value

            if (isGranted){
                Toast.makeText(this,"Permission to access Storage is Granted",Toast.LENGTH_LONG).show()
//               to use intent to go to another application
                val pickIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)

            }
            else{
                if (permissionName==android.Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this,"Oops you just denied the permission to access the storage",Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView=findViewById(R.id.drawing_view)
        val linearLayoutPaintColors=findViewById<LinearLayout>(R.id.ll_paint_colors)
//        above variable contains all colors in Linear layout
        mImageButtonCurrentPaint=linearLayoutPaintColors[1] as ImageButton
//        by this we can access anything from linear layout
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))

        val ibGallery:ImageButton=findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {
             requestStoragePermission()
        }

        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            brushSizeChooserDialog()
        }

        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibRedo : ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener {
            drawingView?.onClickRedo()
        }

        //reference the save button from the layout
        val ibSave:ImageButton = findViewById(R.id.ib_save)
        //set onclick listener
        // Adding an click event to save or exporting the image to your phone storage.)
        ibSave.setOnClickListener{
            //check if permission is allowed
            if (isReadStorageAllowed()){
//                show dialog before launching coroutine
                showProgressDialog()
                //launch a coroutine block
                lifecycleScope.launch{
                    //reference the frame layout
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    //Save the image to the device
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }




    }

    private fun brushSizeChooserDialog(){//to create Brush size dialog
        val brushDialog= Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)//for what to display after clicking brush dialog
        brushDialog.setTitle("Brush Size: ")
        val smallBtn=brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()//to close dialog after selecting specific size
        }

        val mediumBtn=brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()//to close dialog after selecting specific size
        }

        val largeBrush=brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBrush.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()//to close dialog after selecting specific size
        }

        brushDialog.show()

    }

    private fun isReadStorageAllowed(): Boolean {
        //Getting the permission status
        // Here the checkSelfPermission is
        /**
         * Determine whether <em>you</em> have been granted a particular permission.
         *
         * @param permission The name of the permission being checked.
         *
         */
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        /**
         *
         * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
         * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
         *
         */
        //If permission is granted returning true and If permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
         if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
             showRationaleDialog("Kids Drawing App ","Kids Drawing App needs to Access Your External Storage")
         }
        else{//ask for permission
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE))
         }
    }

    fun paintClicked(view: View){
        if(view!==mImageButtonCurrentPaint){
             val imageButton=view as ImageButton
            //to get the color we use tag...it will give us the value written at tag...like for @color/any color #037FF
             val colorTag= imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            //to show the selected color different
            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))
//to change previously selected color to normal
            mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_normal))
            //so that this code run again
            mImageButtonCurrentPaint=view
        }
    }

    /**
     * Shows rationale dialog for displaying why the app needs permission
     * Only shown if the user has denied the permission request previously
     */
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

//    we have view....but we want to save image in gallery....So we convert it into bitmap as a sandwich of background+view+what we have drawn
    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    // TODO(Step 2 : A method to save the image.)
    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream() // Creates a new byte array output stream.
                    // The buffer capacity is initially 32 bytes, though its size increases if necessary.

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                   //file name
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fo =
                        FileOutputStream(f) // Creates a file output stream to write to the file represented by the specified object.
                    fo.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                    fo.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.
                    result = f.absolutePath // The file absolute path is return as a result.
                    //We switch from io to ui thread to show a toast
                    runOnUiThread {
//                        cancel dialog after the file is saved
                        cancelProgressDialog()
                        if (!result.isEmpty()) {
                            Toast.makeText(this@MainActivity, "File saved successfully :$result", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

//    Sharing the downloaded Image file)
    private fun shareImage(result:String){

        /*MediaScannerConnection provides a way for applications to pass a
        newly created or downloaded media file to the media scanner service.
        The media scanner service will read metadata from the file and add
        the file to the media content provider.
        The MediaScannerConnectionClient provides an interface for the
        media scanner service to return the Uri for a newly scanned file
        to the client of the MediaScannerConnection class.*/

        /*scanFile is used to scan the file when the connection is established with MediaScanner.*/
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(result), null) {
                path, uri ->
            // This is used for sharing the image after it has being stored in the storage.
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            ) // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
            shareIntent.type =
                "image/png" // The MIME type of the data being handled by this intent.
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share"
                )
            )// Activity Action: Display an activity chooser,
            // allowing the user to pick what they want to before proceeding.
            // This can be used as an alternative to the standard activity picker
            // that is displayed by the system when you try to start an activity with multiple possible matches,
            // with these differences in behavior:
        }
        // END
    }

    /** Todo 3: create function to cancel dialog
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

}