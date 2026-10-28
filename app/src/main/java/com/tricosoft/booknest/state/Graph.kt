package com.tricosoft.booknest.state

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tricosoft.booknest.BuildConfig
import com.tricosoft.booknest.data.Api
import com.tricosoft.booknest.data.Repo
import com.tricosoft.booknest.data.SecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** App er shob singleton ekhane (web er Context Provider gulor jaygay). */
class Graph(app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val prefs = UiPrefs(app)
    val toaster = Toaster()
    val store = SecureStore(app)
    val api = Api(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY, store)
    val repo = Repo(api)
    val vault = VaultManager(api, repo, toaster, scope)

    init {
        // Login hole BookNest profile ensure kori (web AuthContext er moto), ar vault status load kori
        scope.launch {
            api.session.map { it?.userId }.distinctUntilChanged().collect { uid ->
                if (uid != null) {
                    try {
                        repo.ensureProfile()
                    } catch (e: Exception) {
                        // profile page retry dekhabe
                    }
                }
                vault.onUserChanged(uid)
            }
        }
    }
}

val LocalGraph = staticCompositionLocalOf<Graph> { error("Graph not provided") }

/** Theme (light/dark) ar language, persist kora (web er localStorage booknest-theme / booknest-lang). */
class UiPrefs(context: Context) {
    private val sp = context.getSharedPreferences("booknest_prefs", Context.MODE_PRIVATE)

    var dark by mutableStateOf(sp.getString("theme", "light") == "dark")
        private set

    var language by mutableStateOf(sp.getString("lang", "en") ?: "en")
        private set

    fun toggleDark() {
        dark = !dark
        sp.edit().putString("theme", if (dark) "dark" else "light").apply()
    }

    fun changeLanguage(code: String) {
        language = code
        sp.edit().putString("lang", code).apply()
    }
}

/** sonner toast er moto chhoto message. */
class Toaster {
    var message by mutableStateOf<String?>(null)
        private set
    var isError by mutableStateOf(false)
        private set
    var serial by mutableStateOf(0)
        private set

    fun show(text: String) {
        message = text
        isError = false
        serial++
    }

    fun error(text: String) {
        message = text
        isError = true
        serial++
    }

    fun clear() {
        message = null
    }
}
