package com.dozingcatsoftware.boojiecam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.renderscript.RenderScript

class ViewImageActivity : Activity() {
    private val photoLibrary = PhotoLibrary.defaultLibrary()
    private lateinit var rs : RenderScript
    private lateinit var effect: Effect
    private lateinit var imageId: String
    private lateinit var overlayView: OverlayView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_image)
        rs = RenderScript.create(this)

        effect = EdgeLuminanceEffect(rs)
        //imageProcessor = SolidColorEffect.withFixedColors(
        //        rs, 0x000000, 0xffffff)

        imageId = intent.getStringExtra("imageId")
        overlayView = findViewById(R.id.overlayView)
        loadImage()
    }

    private fun loadImage() {
        val metadata = photoLibrary.metadataForItemId(imageId)
        val width = metadata.get("width") as Int
        val height = metadata.get("height") as Int
        val planarYuv = photoLibrary.rawFileInputStreamForItemId(imageId).use {
            PlanarYuvAllocations.fromInputStream(rs, it, width, height)
        }
        val xFlipped = (metadata.get("xFlipped") == true)
        val orientation = if (xFlipped) ImageOrientation.ROTATED_180 else ImageOrientation.NORMAL
        val inputImage = CameraImage(null, planarYuv,
                orientation, CameraStatus.CAPTURING_PHOTO, 0)

        val bitmap = effect.createBitmap(inputImage)
        val paintFn = effect.createPaintFn(inputImage)
        overlayView.processedBitmap = ProcessedBitmap(inputImage, bitmap, paintFn)
        overlayView.invalidate()
    }

    companion object {
        fun startActivityWithImageId(parent: Activity, imageId: String): Intent {
            val intent = Intent(parent, ViewImageActivity::class.java)
            intent.putExtra("imageId", imageId)
            parent.startActivityForResult(intent, 0)
            return intent
        }
    }
}