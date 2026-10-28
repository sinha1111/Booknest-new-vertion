package com.tricosoft.booknest.state

/** Web er LanguageContext er hubohu translation (en / bn / hi). */
data class Labels(
    val settings: String,
    val appearance: String,
    val darkMode: String,
    val language: String,
    val editProfile: String,
    val save: String,
    val cancel: String,
    val name: String,
    val bio: String,
    val signOut: String,
    val profile: String,
    val back: String,
    val account: String,
    val selectLanguage: String,
)

private val EN = Labels(
    settings = "Settings",
    appearance = "Appearance",
    darkMode = "Dark Mode",
    language = "Language",
    editProfile = "Edit Profile",
    save = "Save",
    cancel = "Cancel",
    name = "Name",
    bio = "Bio",
    signOut = "Sign Out",
    profile = "Profile",
    back = "Back",
    account = "Account",
    selectLanguage = "Select Language",
)

private val BN = Labels(
    settings = "সেটিংস",
    appearance = "অ্যাপিয়ারেন্স",
    darkMode = "ডার্ক মোড",
    language = "ভাষা",
    editProfile = "প্রোফাইল এডিট",
    save = "সেভ করুন",
    cancel = "বাতিল",
    name = "নাম",
    bio = "বায়ো",
    signOut = "সাইন আউট",
    profile = "প্রোফাইল",
    back = "পিছনে",
    account = "অ্যাকাউন্ট",
    selectLanguage = "ভাষা নির্বাচন করুন",
)

private val HI = Labels(
    settings = "सेटिंग्स",
    appearance = "अपीयरेंस",
    darkMode = "डार्क मोड",
    language = "भाषा",
    editProfile = "प्रोफाइल एडिट",
    save = "सेव करें",
    cancel = "रद्द करें",
    name = "नाम",
    bio = "बायो",
    signOut = "साइन आउट",
    profile = "प्रोफाइल",
    back = "वापस",
    account = "अकाउंट",
    selectLanguage = "भाषा चुनें",
)

object Strings {
    val languageNames: Map<String, String> = linkedMapOf(
        "en" to "English",
        "bn" to "বাংলা",
        "hi" to "हिन्दी",
    )
    fun labels(code: String): Labels = when (code) { "bn" -> BN; "hi" -> HI; else -> EN }
}
