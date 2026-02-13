package com.meta.wearable.dat.externalsampleapps.bridgehub.bridge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PhotoAttachmentBridge(private val context: Context) {
  fun shareBitmapAttachment(bitmap: Bitmap, title: String = "Share Image"): Result<String> {
    return runCatching {
      val uri = writeBitmap(bitmap)
      val chooser = createChooser(uri, title)
      context.startActivity(chooser)
      "Chooser opened"
    }
  }

  private fun writeBitmap(bitmap: Bitmap): Uri {
    val imagesFolder = File(context.cacheDir, "images")
    imagesFolder.mkdirs()
    val file = File(imagesFolder, "bridge_shared_image.png")
    FileOutputStream(file).use { stream ->
      if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
        throw IOException("Failed to compress bitmap")
      }
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }

  private fun createChooser(uri: Uri, title: String): Intent {
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
          putExtra(Intent.EXTRA_STREAM, uri)
          type = "image/png"
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    return Intent.createChooser(sendIntent, title).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
  }
}
