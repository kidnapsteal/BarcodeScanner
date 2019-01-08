package kidnapsteal.com.barcodescanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kidnapsteal.com.barcodescanner.MainActivity.Constant.REQUEST_PERMISSION
import kidnapsteal.com.barcodescanner.barcode.ScannerError
import kidnapsteal.com.barcodescanner.barcode.ScannerListener
import kidnapsteal.com.barcodescanner.barcode.ScannerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var scannerView: ScannerView? = null

    private val scannerListener = object : ScannerListener {

        override fun onDetected(barcode: Barcode) {
            showData(barcode.rawValue)
            scannerView?.setListener(null)
        }

        override fun onError(scannerError: ScannerError) {
            Log.e("MainActivity", "onError ${scannerError.message}}")
        }
    }

    object Constant {
        const val REQUEST_PERMISSION = 0x99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val barcodeDetectorBuilder = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE)

        initPermissionRequest()

        scannerView = ScannerView().apply {
            setBarcodeDetector(barcodeDetectorBuilder.build())
            setWidth(dp2px(this@MainActivity, 2048))
            setHeight(dp2px(this@MainActivity, 2048))
            setAutoFocus(true)
            setFacing(CameraSource.CAMERA_FACING_BACK)
            setActivity(this@MainActivity)
            setListener(scannerListener)
        }
    }

    private fun initPermissionRequest() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                //show rationale
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            scannerView?.startScanner(scannerSurfaceView)
        }
    }

    override fun onResume() {
        super.onResume()
        scannerView?.startScanner(scannerSurfaceView)
    }

    override fun onDestroy() {
        super.onDestroy()
        scannerView?.releaseAndCleanup()
    }

    private fun showData(data: String) {
        runOnUiThread {
            AlertDialog.Builder(this)
                    .setTitle("Decoded qr value")
                    .setMessage(data)
                    .setPositiveButton("Enable Scanner") { _, _ ->
                        scannerView?.setListener(scannerListener)

                    }
                    .create()
                    .show()
        }

    }
}

fun dp2px(context: Context, dp: Int): Int {
    return TypedValue.applyDimension(1, dp.toFloat(), context.resources.displayMetrics).toInt()
}
