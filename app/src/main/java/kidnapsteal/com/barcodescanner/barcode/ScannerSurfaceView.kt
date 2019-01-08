package kidnapsteal.com.barcodescanner.barcode

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kidnapsteal.com.barcodescanner.R

class ScannerSurfaceView : FrameLayout {
    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(context)
    }

    private fun initView(context: Context) {
        View.inflate(context, R.layout.layout_camera_view, this)
    }
}