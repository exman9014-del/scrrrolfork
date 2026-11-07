package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloader {
    private const val TAG = "ImageDownloader"
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    private const val DEFAULT_REFERRER = "https://www.google.com/"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun downloadPhoto(context: Context, photoUrl: String, title: String? = null) {
        downloadPhoto(context, photoUrl, DEFAULT_REFERRER, title)
    }

    fun downloadPhoto(context: Context, photoUrl: String, referrer: String?, title: String?) {
        if (photoUrl.isBlank()) {
            showToast(context, "URL da imagem inválida")
            return
        }

        val effectiveReferrer = referrer?.ifBlank { DEFAULT_REFERRER } ?: DEFAULT_REFERRER

        if (photoUrl.startsWith("data:image")) {
            // Direct Base64 captured from WebView Canvas - 100% immune to 403 Forbidden!
            downloadBase64(context, photoUrl, title)
        } else if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            // Standard HTTP/HTTPS with required Google Referer and Cookie headers
            showToast(context, "Iniciando download da foto...")
            scope.launch {
                downloadHttpImage(context, photoUrl, effectiveReferrer, title)
            }
        } else {
            showToast(context, "Formato de imagem não suportado")
        }
    }

    fun downloadBase64(context: Context, base64Data: String, title: String? = null) {
        showToast(context, "Salvando imagem capturada...")
        scope.launch {
            saveBase64Image(context, base64Data, title)
        }
    }

    private suspend fun downloadHttpImage(
        context: Context,
        photoUrl: String,
        referrer: String,
        title: String?
    ) {
        try {
            withContext(Dispatchers.IO) {
                val url = URL(photoUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 18000
                connection.instanceFollowRedirects = true

                // CRITICAL: Google blocks image requests without the proper Referer and User-Agent
                connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
                connection.setRequestProperty("Referer", referrer)
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                connection.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                connection.setRequestProperty("sec-fetch-dest", "image")
                connection.setRequestProperty("sec-fetch-mode", "no-cors")
                connection.setRequestProperty("sec-fetch-site", "cross-site")

                // Pass cookies from WebView session if available
                try {
                    val cookies = CookieManager.getInstance().getCookie(photoUrl)
                        ?: CookieManager.getInstance().getCookie(referrer)
                    if (!cookies.isNullOrBlank()) {
                        connection.setRequestProperty("Cookie", cookies)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cookie lookup failed: ${e.message}")
                }

                connection.connect()

                if (connection.responseCode in 200..299) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        saveBitmapToGallery(context, bitmap, title ?: "scrollsearch")
                    } else {
                        enqueueDownloadManager(context, photoUrl, referrer, title)
                    }
                } else {
                    Log.w(TAG, "HTTP code ${connection.responseCode}, trying DownloadManager with Referer")
                    enqueueDownloadManager(context, photoUrl, referrer, title)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct download failed, falling back to DownloadManager: ${e.message}")
            enqueueDownloadManager(context, photoUrl, referrer, title)
        }
    }

    private fun enqueueDownloadManager(
        context: Context,
        photoUrl: String,
        referrer: String,
        title: String?
    ) {
        try {
            val fileName = "scrollsearch_${System.currentTimeMillis()}.jpg"
            val request = DownloadManager.Request(Uri.parse(photoUrl)).apply {
                setTitle(title ?: "ScrollSearch Imagem")
                setDescription("Baixando foto")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "ScrollSearch/$fileName")

                // Provide Referer and User-Agent to avoid 403 Forbidden from Google
                addRequestHeader("Referer", referrer)
                addRequestHeader("User-Agent", DEFAULT_USER_AGENT)
                try {
                    val cookies = CookieManager.getInstance().getCookie(photoUrl)
                        ?: CookieManager.getInstance().getCookie(referrer)
                    if (!cookies.isNullOrBlank()) {
                        addRequestHeader("Cookie", cookies)
                    }
                } catch (_: Exception) {}
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(request)
            showToast(context, "Download agendado com sucesso!")
        } catch (e: Exception) {
            Log.e(TAG, "DownloadManager failed: ${e.message}")
            showToast(context, "Erro ao baixar imagem: ${e.message}")
        }
    }

    private fun saveBase64Image(context: Context, dataUrl: String, title: String?) {
        try {
            val base64Data = dataUrl.substringAfter("base64,")
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                saveBitmapToGallery(context, bitmap, title ?: "scrollsearch_captured")
            } else {
                showToast(context, "Erro ao processar imagem capturada")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64: ${e.message}")
            showToast(context, "Erro ao salvar imagem: ${e.message}")
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String = "scrollsearch") {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScrollSearch")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val scrollSearchDir = File(picturesDir, "ScrollSearch")
                if (!scrollSearchDir.exists()) scrollSearchDir.mkdirs()
                val imageFile = File(scrollSearchDir, fileName)
                outputStream = FileOutputStream(imageFile)
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                outputStream.flush()
                outputStream.close()
                showToast(context, "Foto salva com sucesso na galeria!")
            } else {
                showToast(context, "Não foi possível abrir destino para salvar")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap: ${e.message}")
            showToast(context, "Erro ao salvar na galeria: ${e.message}")
        }
    }

    private fun showToast(context: Context, message: String) {
        mainHandler.post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }
}
