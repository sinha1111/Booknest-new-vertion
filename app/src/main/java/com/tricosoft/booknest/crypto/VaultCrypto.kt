package com.tricosoft.booknest.crypto

import android.util.Base64
import com.tricosoft.booknest.data.VaultRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Draft chapter er client-side end-to-end encryption. Web er src/lib/crypto.ts er shathe BYTE-COMPATIBLE:
 * web e likha encrypted draft app e khole, app e likha draft web e khole.
 *
 *  - DEK: random 256-bit key. Chapter AES-256-GCM e encrypt (AAD = booknest:chapter:v1:<userId>:<bookId>)
 *  - DEK ke 2 vabe wrap kore server e rakha hoy:
 *      passphrase -> PBKDF2-HMAC-SHA256 (600k) -> KEK,   recovery code (160-bit) -> HKDF-SHA256 -> KEK
 *  - Passphrase / recovery code / DEK kokhono server e jay na.
 */
object VaultCrypto {
    const val KDF_ITERATIONS = 600_000
    const val CIPHER_PREFIX = "e2ee.v1."
    private const val B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val rng = SecureRandom()

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun fromB64(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)
    private fun rand(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    private fun aadDek(userId: String) = "booknest:dek:v1:$userId".toByteArray(Charsets.UTF_8)
    private fun aadChapter(userId: String, bookId: String) =
        "booknest:chapter:v1:$userId:$bookId".toByteArray(Charsets.UTF_8)

    // ------------------------------------------------------------ primitives
    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** RFC 8018 PBKDF2-HMAC-SHA256, 32 byte output (ekta block). Password UTF-8 bytes (WebCrypto er moto). */
    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        mac.update(salt)
        mac.update(byteArrayOf(0, 0, 0, 1))
        var u = mac.doFinal()
        val t = u.copyOf()
        for (i in 2..iterations) {
            u = mac.doFinal(u)
            for (j in t.indices) {
                t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
        }
        return t
    }

    /** RFC 5869 HKDF-SHA256, 32 byte output. */
    private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray): ByteArray {
        val prk = hmac(salt, ikm)
        return hmac(prk, info + byteArrayOf(1))
    }

    private fun gcmEncrypt(key: SecretKey, iv: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        c.updateAAD(aad)
        return c.doFinal(plain)
    }

    private fun gcmDecrypt(key: SecretKey, iv: ByteArray, aad: ByteArray, ct: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        c.updateAAD(aad)
        return c.doFinal(ct)
    }

    private fun toBase32(bytes: ByteArray): String {
        var bits = 0
        var value = 0L
        val sb = StringBuilder()
        for (b in bytes) {
            value = (value shl 8) or (b.toLong() and 0xFF)
            bits += 8
            while (bits >= 5) {
                sb.append(B32[((value shr (bits - 5)) and 31).toInt()])
                bits -= 5
            }
            value = value and ((1L shl bits) - 1)
        }
        if (bits > 0) sb.append(B32[((value shl (5 - bits)) and 31).toInt()])
        return sb.toString()
    }

    private fun fromBase32(s: String): ByteArray {
        val clean = s.uppercase().filter { it in B32 }
        var bits = 0
        var value = 0L
        val out = ArrayList<Byte>()
        for (ch in clean) {
            value = (value shl 5) or B32.indexOf(ch).toLong()
            bits += 5
            if (bits >= 8) {
                out.add(((value shr (bits - 8)) and 255).toByte())
                bits -= 8
                value = value and ((1L shl bits) - 1)
            }
        }
        return out.toByteArray()
    }

    private fun formatRecoveryCode(bytes: ByteArray): String = toBase32(bytes).chunked(4).joinToString("-")

    private fun kekFromPassphrase(passphrase: String, salt: ByteArray, iterations: Int): SecretKey {
        val normalized = Normalizer.normalize(passphrase, Normalizer.Form.NFKC)
        val raw = pbkdf2(normalized.toByteArray(Charsets.UTF_8), salt, iterations)
        return SecretKeySpec(raw, "AES")
    }

    private fun kekFromRecovery(code: String, salt: ByteArray): SecretKey {
        val raw = fromBase32(code)
        if (raw.size != 20) throw IllegalArgumentException("Invalid recovery code")
        return SecretKeySpec(hkdf(raw, salt, "booknest:recovery:v1".toByteArray(Charsets.UTF_8)), "AES")
    }

    // ------------------------------------------------------------ vault lifecycle
    fun checkPassphraseStrength(p: String): String? {
        if (p.length < 10) return "Passphrase must be at least 10 characters"
        if (p.toSet().size < 5) return "Passphrase is too repetitive"
        return null
    }

    class Created(val record: VaultRecord, val dek: SecretKey, val recoveryCode: String)

    fun createVault(passphrase: String, userId: String): Created {
        val dekRaw = rand(32)

        val salt = rand(16)
        val kek = kekFromPassphrase(passphrase, salt, KDF_ITERATIONS)
        val wrapIv = rand(12)
        val wrapped = gcmEncrypt(kek, wrapIv, aadDek(userId), dekRaw)

        val recoveryBytes = rand(20)
        val recoverySalt = rand(16)
        val rkek = kekFromRecovery(formatRecoveryCode(recoveryBytes), recoverySalt)
        val rIv = rand(12)
        val rWrapped = gcmEncrypt(rkek, rIv, aadDek(userId), dekRaw)

        val record = VaultRecord(
            kdfSalt = b64(salt),
            kdfIterations = KDF_ITERATIONS,
            wrappedDek = b64(wrapped),
            wrapIv = b64(wrapIv),
            recoverySalt = b64(recoverySalt),
            recoveryWrappedDek = b64(rWrapped),
            recoveryIv = b64(rIv),
        )
        val dek = SecretKeySpec(dekRaw.copyOf(), "AES")
        dekRaw.fill(0)
        return Created(record, dek, formatRecoveryCode(recoveryBytes))
    }

    /** Vul passphrase hole exception (AEADBadTagException). */
    fun unlockWithPassphrase(record: VaultRecord, passphrase: String, userId: String): SecretKey {
        val kek = kekFromPassphrase(passphrase, fromB64(record.kdfSalt), record.kdfIterations)
        val raw = gcmDecrypt(kek, fromB64(record.wrapIv), aadDek(userId), fromB64(record.wrappedDek))
        val dek = SecretKeySpec(raw.copyOf(), "AES")
        raw.fill(0)
        return dek
    }

    class Reset(val record: VaultRecord, val dek: SecretKey)

    /** Passphrase bhule gele: recovery code diye DEK ber kore notun passphrase e re-wrap. */
    fun resetPassphraseWithRecovery(record: VaultRecord, recoveryCode: String, newPassphrase: String, userId: String): Reset {
        val rkek = kekFromRecovery(recoveryCode, fromB64(record.recoverySalt))
        val raw = gcmDecrypt(rkek, fromB64(record.recoveryIv), aadDek(userId), fromB64(record.recoveryWrappedDek))

        val salt = rand(16)
        val kek = kekFromPassphrase(newPassphrase, salt, KDF_ITERATIONS)
        val iv = rand(12)
        val wrapped = gcmEncrypt(kek, iv, aadDek(userId), raw)

        val next = record.copy(
            kdfSalt = b64(salt),
            kdfIterations = KDF_ITERATIONS,
            wrappedDek = b64(wrapped),
            wrapIv = b64(iv),
        )
        val dek = SecretKeySpec(raw.copyOf(), "AES")
        raw.fill(0)
        return Reset(next, dek)
    }

    // ------------------------------------------------------------ chapters
    fun isEncryptedPayload(s: String?): Boolean = s != null && s.startsWith(CIPHER_PREFIX)

    fun encryptChapter(dek: SecretKey, userId: String, bookId: String, title: String, content: String): String {
        val iv = rand(12)
        val plain = buildJsonObject { put("t", title); put("c", content) }.toString().toByteArray(Charsets.UTF_8)
        val ct = gcmEncrypt(dek, iv, aadChapter(userId, bookId), plain)
        return CIPHER_PREFIX + b64(iv) + "." + b64(ct)
    }

    class Decrypted(val title: String, val content: String)

    fun decryptChapter(dek: SecretKey, userId: String, bookId: String, payload: String): Decrypted {
        if (!isEncryptedPayload(payload)) throw IllegalArgumentException("Not an encrypted payload")
        val parts = payload.substring(CIPHER_PREFIX.length).split(".")
        if (parts.size != 2) throw IllegalArgumentException("Malformed ciphertext")
        val pt = gcmDecrypt(dek, fromB64(parts[0]), aadChapter(userId, bookId), fromB64(parts[1]))
        val obj = Json.parseToJsonElement(String(pt, Charsets.UTF_8)).jsonObject
        return Decrypted(
            obj["t"]?.jsonPrimitive?.contentOrNull ?: "",
            obj["c"]?.jsonPrimitive?.contentOrNull ?: "",
        )
    }
}
