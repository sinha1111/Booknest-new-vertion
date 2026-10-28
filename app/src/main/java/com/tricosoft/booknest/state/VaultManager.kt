package com.tricosoft.booknest.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tricosoft.booknest.crypto.VaultCrypto
import com.tricosoft.booknest.data.Api
import com.tricosoft.booknest.data.Repo
import com.tricosoft.booknest.data.VaultRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey
import kotlin.coroutines.resume

enum class VaultStatus { Loading, None, Locked, Unlocked }
enum class VaultMode { Setup, Unlock, Recovery, ShowCode }

/**
 * Draft encryption vault (web er VaultContext er port). Key shudhu memory te thake; app bondho hole / lock korle muche jay.
 */
class VaultManager(
    @Suppress("unused") private val api: Api,
    private val repo: Repo,
    private val toaster: Toaster,
    @Suppress("unused") private val scope: CoroutineScope,
) {
    var status by mutableStateOf(VaultStatus.Loading)
        private set
    var dialog by mutableStateOf<VaultMode?>(null)
        private set
    var recoveryCode by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set

    private var record: VaultRecord? = null
    private var dek: SecretKey? = null
    private var userId: String? = null
    private var pending: kotlinx.coroutines.CancellableContinuation<SecretKey?>? = null

    fun key(): SecretKey? = dek

    suspend fun onUserChanged(uid: String?) {
        dek = null
        record = null
        userId = uid
        closeDialog(null)
        if (uid == null) {
            status = VaultStatus.Loading
            return
        }
        status = VaultStatus.Loading
        try {
            val r = repo.vaultRecord(uid)
            record = r
            status = if (r != null) VaultStatus.Locked else VaultStatus.None
        } catch (e: Exception) {
            status = VaultStatus.None
        }
    }

    /** Unlock na thakle dialog dekhay; unlock hole key, cancel korle null. */
    suspend fun ensureUnlocked(): SecretKey? {
        dek?.let { return it }
        if (userId == null) return null
        dialog = if (record != null) VaultMode.Unlock else VaultMode.Setup
        return suspendCancellableCoroutine { cont ->
            pending?.resume(null)
            pending = cont
            cont.invokeOnCancellation {
                if (pending === cont) {
                    pending = null
                    dialog = null
                }
            }
        }
    }

    fun lock() {
        dek = null
        status = if (record != null) VaultStatus.Locked else VaultStatus.None
    }

    private fun closeDialog(result: SecretKey?) {
        dialog = null
        val p = pending
        pending = null
        p?.resume(result)
    }

    fun cancelDialog() = closeDialog(dek)
    fun finishDialog() = closeDialog(dek)
    fun openRecovery() { dialog = VaultMode.Recovery }
    fun backToUnlock() { dialog = VaultMode.Unlock }

    suspend fun setup(passphrase: String, repeat: String) {
        val uid = userId ?: return
        val weak = VaultCrypto.checkPassphraseStrength(passphrase)
        if (weak != null) { toaster.error(weak); return }
        if (passphrase != repeat) { toaster.error("Passphrases do not match"); return }
        busy = true
        try {
            val created = withContext(Dispatchers.Default) { VaultCrypto.createVault(passphrase, uid) }
            repo.insertVault(uid, created.record)
            record = created.record
            dek = created.dek
            status = VaultStatus.Unlocked
            recoveryCode = created.recoveryCode
            dialog = VaultMode.ShowCode
        } catch (e: Exception) {
            toaster.error("Could not set up draft encryption")
        } finally {
            busy = false
        }
    }

    suspend fun unlock(passphrase: String) {
        val uid = userId ?: return
        val r = record ?: return
        busy = true
        try {
            val key = withContext(Dispatchers.Default) { VaultCrypto.unlockWithPassphrase(r, passphrase, uid) }
            dek = key
            status = VaultStatus.Unlocked
            closeDialog(key)
        } catch (e: Exception) {
            toaster.error("Wrong passphrase")
        } finally {
            busy = false
        }
    }

    suspend fun recover(code: String, newPassphrase: String, repeat: String) {
        val uid = userId ?: return
        val r = record ?: return
        val weak = VaultCrypto.checkPassphraseStrength(newPassphrase)
        if (weak != null) { toaster.error(weak); return }
        if (newPassphrase != repeat) { toaster.error("Passphrases do not match"); return }
        busy = true
        try {
            val res = withContext(Dispatchers.Default) {
                VaultCrypto.resetPassphraseWithRecovery(r, code, newPassphrase, uid)
            }
            repo.updateVaultPassphrase(uid, res.record)
            record = res.record
            dek = res.dek
            status = VaultStatus.Unlocked
            toaster.show("Passphrase reset")
            closeDialog(res.dek)
        } catch (e: Exception) {
            toaster.error("Invalid recovery code")
        } finally {
            busy = false
        }
    }

}
