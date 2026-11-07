package com.example.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

object ImageDownloadHelper {

    private const val TAG = "ImageDownloadHelper"
    private val mainHandler = Handler(Looper.getMainLooper())

    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .header("Referer", "https://www.google.com/")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage doesn't require WRITE_EXTERNAL_STORAGE for MediaStore.Images
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun downloadImage(
        context: Context,
        imageUrl: String,
        title: String = "Instagram_Photo"
    ) {
        if (!hasStoragePermission(context)) {
            Toast.makeText(
                context,
                "Permissão de armazenamento necessária para baixar fotos no Android 9 ou inferior.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Toast.makeText(context, "Iniciando download da foto...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
                val fileName = "ScrollSearch_${cleanTitle}_${System.currentTimeMillis()}.jpg"

                val bytes: ByteArray = if (imageUrl.startsWith("data:image")) {
                    val base64Data = imageUrl.substringAfter(",")
                    Base64.decode(base64Data, Base64.DEFAULT)
                } else {
                    val request = Request.Builder()
                        .url(imageUrl)
                        .build()
                    val response = downloadClient.newCall(request).execute()
                    if (!response.isSuccessful || response.body == null) {
                        throw Exception("Falha HTTP: ${response.code}")
                    }
                    response.body!!.bytes()
                }

                saveBytesToPictures(context, bytes, fileName)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Foto salva com sucesso na Galeria (Fotos/ScrollSearch)!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading photo: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Erro ao salvar foto: ${e.localizedMessage ?: "Tente novamente"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun saveBytesToPictures(context: Context, bytes: ByteArray, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScrollSearch")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Não foi possível criar registro no MediaStore")

            resolver.openOutputStream(uri).use { outputStream ->
                if (outputStream == null) throw Exception("Não foi possível abrir OutputStream")
                outputStream.write(bytes)
                outputStream.flush()
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val scrollSearchDir = File(picturesDir, "ScrollSearch")
            if (!scrollSearchDir.exists()) {
                scrollSearchDir.mkdirs()
            }

            val file = File(scrollSearchDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(bytes)
                out.flush()
            }

            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
        }
    }
}
