package com.example.data.file

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionEngine {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH = 256
    private const val ITERATION_COUNT = 10000
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16
    private val HEADER_MAGIC = "AES_VAULT_V1".toByteArray(Charsets.UTF_8)

    fun encryptFile(inputFile: File, outputFile: File, password: String): Result<File> {
        return try {
            val salt = ByteArray(SALT_SIZE)
            val iv = ByteArray(IV_SIZE)
            val random = SecureRandom()
            random.nextBytes(salt)
            random.nextBytes(iv)

            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            FileOutputStream(outputFile).use { fos ->
                // Write Magic Header + Salt + IV
                fos.write(HEADER_MAGIC)
                fos.write(salt)
                fos.write(iv)

                FileInputStream(inputFile).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val outputBytes = cipher.update(buffer, 0, bytesRead)
                        if (outputBytes != null) {
                            fos.write(outputBytes)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        fos.write(finalBytes)
                    }
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun decryptFile(inputFile: File, outputFile: File, password: String): Result<File> {
        return try {
            FileInputStream(inputFile).use { fis ->
                val magic = ByteArray(HEADER_MAGIC.size)
                if (fis.read(magic) != HEADER_MAGIC.size || !magic.contentEquals(HEADER_MAGIC)) {
                    return Result.failure(Exception("الملف غير مشفر أو تنسيق التشفير غير مدعوم"))
                }

                val salt = ByteArray(SALT_SIZE)
                val iv = ByteArray(IV_SIZE)
                if (fis.read(salt) != SALT_SIZE || fis.read(iv) != IV_SIZE) {
                    return Result.failure(Exception("ملف التشفير تالف"))
                }

                val secretKey = deriveKey(password, salt)
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val outputBytes = cipher.update(buffer, 0, bytesRead)
                        if (outputBytes != null) {
                            fos.write(outputBytes)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) {
                        fos.write(finalBytes)
                    }
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("كلمة المرور غير صحيحة أو الملف تالف"))
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
