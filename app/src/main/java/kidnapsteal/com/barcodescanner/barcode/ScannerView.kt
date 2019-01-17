package kidnapsteal.com.barcodescanner.barcode

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kidnapsteal.com.barcodescanner.R
import java.io.IOException

class ScannerView {
    private var cameraSource: CameraSource? = null
    private var scannerListener: ScannerListener? = null
    private lateinit var barcodeDetector: BarcodeDetector

    private lateinit var context: Context

    private var width = 800
    private var height = 800
    private var surfaceCreated = false
    private var isCameraRunning = false
    private var autoFocusEnabled = false
    private var facing = CameraSource.CAMERA_FACING_BACK

    private lateinit var overlayView: View
    private lateinit var surfaceView: SurfaceView
    private var scannerSurfaceView: ScannerSurfaceView? = null
    private var surfaceCallback: SurfaceHolder.Callback? = null


    //region Should Create Builder to manage properties/ drawback to custom View with xml properties
    fun setBarcodeDetector(barcodeDetector: BarcodeDetector) {
        this.barcodeDetector = barcodeDetector
    }

    fun setWidth(width: Int) {
        this.width = width
    }

    fun setHeight(height: Int) {
        this.height = height
    }

    fun setAutoFocus(autoFocus: Boolean) {
        this.autoFocusEnabled = autoFocus
    }

    fun setFacing(facing: Int) {
        this.facing = facing
    }

    fun setListener(scannerListener: ScannerListener?) {
        this.scannerListener = scannerListener
    }

    /**
     * Fixme Temporary needed, should be remove once CustomView created
     *
     */
    fun setActivity(activity: Activity) {
        this.context = activity
    }
    //endregion

    fun startScanner(scannerSurfaceView: ScannerSurfaceView) {
        this.scannerSurfaceView = scannerSurfaceView
        this.surfaceView = scannerSurfaceView.findViewById(R.id.surface_view)
        this.overlayView = scannerSurfaceView.findViewById(R.id.overlay)
        this.scannerSurfaceView!!.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                initSurfaceCallback()
                initCameraSource()

                //to prevent initialized called multiple time, need to remove listener after initialization
                removeOnGlobalLayoutListener(this@ScannerView.scannerSurfaceView, this)
            }
        })
    }

    fun releaseAndCleanup() {
        stopCameraView()
        if (cameraSource != null) {
            cameraSource!!.release()
            cameraSource = null
            surfaceView.holder.removeCallback(surfaceCallback)
        }
    }

    private fun initCameraSource() {

        if (!hasAutoFocus(context)) {
          Log.e(TAG, "No Auto focus feature")
          autoFocusEnabled = false
        }
        if (!hasCameraHardware(context)) {
          Log.e(TAG, "No camera hardware")
          scannerListener?.onError(NoCameraHardware)
            return
        }

        if (!checkCameraPermission(context)) {
          Log.e(TAG, "Camera permission denied")
          scannerListener?.onError(PermissionDenied())
            return
        }

        if (barcodeDetector.isOperational) {
            val newDetector = AppBarcodeDetector(barcodeDetector, overlayView.width, overlayView.height)

            newDetector.setProcessor(object : Detector.Processor<Barcode> {
                override fun release() = Unit

                override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                    val barcodes = detections.detectedItems
                    if (barcodes.size() != 0 && scannerListener != null) {
                        scannerListener!!.onDetected(barcodes.valueAt(0))
                    }
                }
            })

            cameraSource = CameraSource.Builder(context, newDetector)
                    .setAutoFocusEnabled(autoFocusEnabled)
                    .setFacing(facing)
                    .setRequestedFps(15.0f)
                    .setRequestedPreviewSize(width, height)
                    .build()

            if (scannerSurfaceView != null && surfaceCallback != null) {
                if (surfaceCreated) {
                    startCameraView(context, cameraSource, surfaceView)
                } else {
                    surfaceView.holder.addCallback(surfaceCallback)
                }
            }
        } else {
          Log.e(TAG, "Barcode recognition does not working")
          scannerListener?.onError(GeneralError)
        }

    }

    private fun initSurfaceCallback() {
        surfaceCallback = object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceCreated = true
                startCameraView(context, cameraSource, scannerSurfaceView!!.findViewById(R.id.surface_view))
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceCreated = false
                stopCameraView()
                holder.removeCallback(this)
            }
        }
    }

    private fun startCameraView(context: Context, cameraSource: CameraSource?, surfaceView: SurfaceView) {
        if (isCameraRunning) return

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission not granted")
            } else {
                cameraSource!!.start(surfaceView.holder)
                isCameraRunning = true
            }

        } catch (e: IOException) {
            Log.e(TAG, e.message)
            e.printStackTrace()

        }

    }

    private fun stopCameraView() {
        try {
            if (isCameraRunning && cameraSource != null) {
                cameraSource!!.stop()
                isCameraRunning = false
            }
        } catch (e: Exception) {
            Log.e(TAG, e.message)
            e.printStackTrace()
        }

    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun removeOnGlobalLayoutListener(view: View?, listener: ViewTreeObserver.OnGlobalLayoutListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view!!.viewTreeObserver.removeGlobalOnLayoutListener(listener)
        } else {
            view!!.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    /**
     * Checking if device camera has autofocus or not
     */
    private fun hasAutoFocus(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)
    }

    /**
     * checking [Manifest.permission.CAMERA] permission status
     */
    private fun checkCameraPermission(context: Context): Boolean {
        val permission = Manifest.permission.CAMERA
        val res = context.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    /**
     * checking device camera availability
     */
    private fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    companion object {
        private val TAG = ScannerView::class.java.simpleName
    }
}

interface ScannerListener {
    fun onDetected(barcode: Barcode)
    fun onError(scannerError: ScannerError)
}

sealed class ScannerError(val message: String)
class PermissionDenied(val requiredPermission: String = Manifest.permission.CAMERA) : ScannerError("To Use scanner, need camera permission granted 1st")
object NoCameraHardware : ScannerError("No Camera hardware found in your device")
object GeneralError : ScannerError("Forgot to set BarcodeDetector? / barcodeDetector isOperational == false")
