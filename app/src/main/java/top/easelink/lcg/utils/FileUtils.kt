package top.easelink.lcg.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import timber.log.Timber
import top.easelink.lcg.appinit.LCGApp
import java.io.*


fun saveImageToGallery(bmp: Bitmap, bitName: String): String {
    val appDir = File(LCGApp.context.externalCacheDir, "lcg")
    if (!appDir.exists()) {
        appDir.mkdir()
    }
    val fileName = "$bitName.png"
    val file = File(appDir, fileName)
    try {
        val fos = FileOutputStream(file)
        bmp.compress(Bitmap.CompressFormat.PNG, 85, fos)
        fos.flush()
        fos.close()
    } catch (e: FileNotFoundException) {
        Timber.e(e)
    } catch (e: IOException) {
        Timber.e(e)
    } finally {
        return file.path
    }
}

/**
 * 把已经落盘的图片文件登记进系统相册。
 *
 * 旧实现用 MediaStore.Images.Media.insertImage + ACTION_MEDIA_SCANNER_SCAN_FILE 广播，
 * 两者在 API 29 起都已废弃，且直写 /sdcard/DCIM 在分区存储下根本写不进去。
 * 这里按版本分流：Q 及以上走 MediaStore + RELATIVE_PATH，以下走公共目录 + MediaScanner。
 */
fun syncSystemGallery(context: Context, path: String, fileName: String) {
    val src = File(path)
    if (!src.exists()) {
        Timber.e("File not exists: %s", path)
        return
    }
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertViaMediaStore(context, src, fileName)
        } else {
            insertViaPublicDir(context, src, fileName)
        }
    }.onFailure { Timber.e(it, "syncSystemGallery failed") }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun insertViaMediaStore(context: Context, src: File, fileName: String) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, guessMimeType(fileName))
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LCG")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: throw IOException("MediaStore insert returned null")
    resolver.openOutputStream(uri).use { out ->
        if (out == null) throw IOException("openOutputStream returned null")
        src.inputStream().use { it.copyTo(out) }
    }
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
}

@Suppress("DEPRECATION")
private fun insertViaPublicDir(context: Context, src: File, fileName: String) {
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "LCG")
    if (!dir.exists()) dir.mkdirs()
    val dest = File(dir, fileName)
    src.inputStream().use { input -> FileOutputStream(dest).use { input.copyTo(it) } }
    MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), null, null)
}

private fun guessMimeType(fileName: String): String = when {
    fileName.endsWith(".png", true) -> "image/png"
    fileName.endsWith(".webp", true) -> "image/webp"
    fileName.endsWith(".gif", true) -> "image/gif"
    else -> "image/jpeg"
}


/**
 * 读取assets本地json
 * @param fileName
 * @param context
 * @return json String
 */
fun getJsonStringFromAssets(fileName: String, context: Context): String {
    val stringBuilder = StringBuilder()
    try {
        val bf = BufferedReader(InputStreamReader(context.assets.open(fileName)))
        var line: String?
        while (bf.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return stringBuilder.toString()
}