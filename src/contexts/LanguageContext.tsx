import { createContext, useContext, useState, ReactNode } from "react";

export type Language = "en" | "bn" | "hi";

interface LanguageLabels {
  settings: string;
  appearance: string;
  darkMode: string;
  language: string;
  editProfile: string;
  save: string;
  cancel: string;
  name: string;
  bio: string;
  signOut: string;
  profile: string;
  back: string;
  account: string;
  selectLanguage: string;
}

const translations: Record<Language, LanguageLabels> = {
  en: {
    settings: "Settings",
    appearance: "Appearance",
    darkMode: "Dark Mode",
    language: "Language",
    editProfile: "Edit Profile",
    save: "Save",
    cancel: "Cancel",
    name: "Name",
    bio: "Bio",
    signOut: "Sign Out",
    profile: "Profile",
    back: "Back",
    account: "Account",
    selectLanguage: "Select Language",
  },
  bn: {
    settings: "সেটিংস",
    appearance: "অ্যাপিয়ারেন্স",
    darkMode: "ডার্ক মোড",
    language: "ভাষা",
    editProfile: "প্রোফাইল এডিট",
    save: "সেভ করুন",
    cancel: "বাতিল",
    name: "নাম",
    bio: "বায়ো",
    signOut: "সাইন আউট",
    profile: "প্রোফাইল",
    back: "পিছনে",
    account: "অ্যাকাউন্ট",
    selectLanguage: "ভাষা নির্বাচন করুন",
  },
  hi: {
    settings: "सेटिंग्स",
    appearance: "अपीयरेंस",
    darkMode: "डार्क मोड",
    language: "भाषा",
    editProfile: "प्रोफाइल एडिट",
    save: "सेव करें",
    cancel: "रद्द करें",
    name: "नाम",
    bio: "बायो",
    signOut: "साइन आउट",
    profile: "प्रोफाइल",
    back: "वापस",
    account: "अकाउंट",
    selectLanguage: "भाषा चुनें",
  },
};

const languageNames: Record<Language, string> = {
  en: "English",
  bn: "বাংলা",
  hi: "हिन्दी",
};

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: LanguageLabels;
  languageNames: Record<Language, string>;
}

const LanguageContext = createContext<LanguageContextType>({
  language: "en",
  setLanguage: () => {},
  t: translations.en,
  languageNames,
});

export const useLanguage = () => useContext(LanguageContext);

export const LanguageProvider = ({ children }: { children: ReactNode }) => {
  const [language, setLang] = useState<Language>(() => {
    const saved = localStorage.getItem("booknest-lang");
    return (saved as Language) || "en";
  });

  const setLanguage = (lang: Language) => {
    setLang(lang);
    localStorage.setItem("booknest-lang", lang);
  };

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t: translations[language], languageNames }}>
      {children}
    </LanguageContext.Provider>
  );
};
