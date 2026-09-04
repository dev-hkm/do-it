package at.co.schwaerzler.maximilian.doit.data.backup

import android.util.Base64
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CloudBackupClient {
    const val ENDPOINT = "https://do-it-cloud-backup.cloud-backup-worker.workers.dev/v1/backup"
    private val random = SecureRandom()
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun newToken(): String = buildString {
        append("hkm-")
        repeat(36) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    fun encrypt(json: String, token: String): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(token), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(payload: String, token: String): String {
        val packed = Base64.decode(payload, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(token), GCMParameterSpec(128, packed.copyOfRange(0, 12)))
        return cipher.doFinal(packed.copyOfRange(12, packed.size)).toString(Charsets.UTF_8)
    }

    suspend fun upload(token: String, json: String) = io {
        val response = request(
            method = "POST",
            body = JSONObject().put("token", token).put("payload", encrypt(json, token)).toString()
        )
        requireSuccessfulResponse(response)
    }

    suspend fun restore(token: String): String = io {
        val response = request("GET", null, "$ENDPOINT?token=${URLEncoder.encode(token, "UTF-8")}")
        requireSuccessfulResponse(response)
        decrypt(JSONObject(response).getString("payload"), token)
    }

    private fun key(token: String): SecretKeySpec = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)), "AES"
    )

    private fun request(method: String, body: String?, url: String = ENDPOINT): String {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 20_000
                readTimeout = 30_000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                doInput = true
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                val serverMessage = runCatching { JSONObject(text).optString("error") }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                throw IOException(serverMessage ?: "Cloud backup service returned HTTP $responseCode")
            }
            return text
        } catch (error: UnknownHostException) {
            throw IOException("No internet connection. Check Wi-Fi or mobile data.", error)
        } catch (error: SocketTimeoutException) {
            throw IOException("Connection timed out. Try again.", error)
        } catch (error: ConnectException) {
            throw IOException("Could not connect to cloud backup. Try again.", error)
        } catch (error: SocketException) {
            if (error.message?.contains("Permission denied", ignoreCase = true) == true) {
                throw IOException("Internet access is unavailable for this app build.", error)
            }
            throw IOException("Network connection failed. Check Wi-Fi or mobile data.", error)
        } catch (error: SecurityException) {
            throw IOException("Internet access is unavailable for this app build.", error)
        } finally {
            connection?.disconnect()
        }
    }

    private fun requireSuccessfulResponse(response: String) {
        if (!JSONObject(response).optBoolean("success")) {
            throw IOException("Cloud backup service returned an invalid response.")
        }
    }

    private suspend fun <T> io(block: () -> T): T = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { block() }
}
