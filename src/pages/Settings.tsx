import { ArrowLeft, Moon, Sun, Globe, User, ChevronRight, LogOut, Shield, Trash2, Lock, ShieldCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import PrivacyPolicyLink, { PRIVACY_URL } from "@/components/PrivacyPolicyLink";
import { useTheme } from "@/contexts/ThemeContext";
import { useLanguage, Language } from "@/contexts/LanguageContext";
import { useAuth } from "@/contexts/AuthContext";
import { useVault } from "@/contexts/VaultContext";
import { supabase } from "@/integrations/supabase/client";
import { useState, useEffect } from "react";
import { toast } from "sonner";


const SettingsPage = () => {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();
  const { language, setLanguage, t, languageNames } = useLanguage();
  const { user, signOut } = useAuth();
  const vault = useVault();

  const [editingProfile, setEditingProfile] = useState(false);
  const [name, setName] = useState("");
  const [bio, setBio] = useState("");
  const [saving, setSaving] = useState(false);
  const [showLangPicker, setShowLangPicker] = useState(false);
  const [deleting, setDeleting] = useState(false);


  useEffect(() => {
    if (user) {
      loadProfile();
    }
  }, [user]);

  const loadProfile = async () => {
    const { data } = await supabase
      .from("profiles")
      .select("name, bio")
      .eq("user_id", user!.id)
      .single();
    if (data) {
      setName(data.name || "");
      setBio(data.bio || "");
    }
  };

  const saveProfile = async () => {
    setSaving(true);
    const { error } = await supabase
      .from("profiles")
      .update({ name, bio })
      .eq("user_id", user!.id);
    setSaving(false);
    if (error) {
      toast.error("Failed to save");
    } else {
      toast.success("Profile updated!");
      setEditingProfile(false);
    }
  };

  const handleSignOut = async () => {
    await signOut();
    navigate("/auth");
  };

  const handleDeleteAccount = async () => {
    setDeleting(true);
    try {
      const { error } = await supabase.functions.invoke("booknest-delete-account");
      if (error) throw error;
      toast.success("Your account has been deleted");
      await supabase.auth.signOut();
      navigate("/auth", { replace: true });
    } catch (e: any) {
      toast.error(e.message || "Failed to delete account");
    } finally {
      setDeleting(false);
    }
  };



  return (
    <div className="min-h-screen pb-20 bg-background">
      <header className="mx-auto max-w-lg px-5 pt-safe pb-4">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate(-1)} className="rounded-full p-1.5 hover:bg-secondary">
            <ArrowLeft className="h-5 w-5 text-foreground" />
          </button>
          <h1 className="font-display text-xl font-bold">{t.settings}</h1>
        </div>
      </header>

      <div className="mx-auto max-w-lg px-5 space-y-6">
        {/* Appearance */}
        <section>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
            {t.appearance}
          </h2>
          <div className="rounded-xl bg-card shadow-sm overflow-hidden">
            <div className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                {theme === "dark" ? (
                  <Moon className="h-5 w-5 text-muted-foreground" />
                ) : (
                  <Sun className="h-5 w-5 text-muted-foreground" />
                )}
                <span className="text-sm font-medium">{t.darkMode}</span>
              </div>
              <Switch checked={theme === "dark"} onCheckedChange={toggleTheme} />
            </div>
          </div>
        </section>

        {/* Language */}
        <section>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
            {t.language}
          </h2>
          <div className="rounded-xl bg-card shadow-sm overflow-hidden">
            <button
              onClick={() => setShowLangPicker(!showLangPicker)}
              className="flex items-center justify-between w-full p-4"
            >
              <div className="flex items-center gap-3">
                <Globe className="h-5 w-5 text-muted-foreground" />
                <span className="text-sm font-medium">{languageNames[language]}</span>
              </div>
              <ChevronRight className={`h-4 w-4 text-muted-foreground transition-transform ${showLangPicker ? "rotate-90" : ""}`} />
            </button>
            {showLangPicker && (
              <div className="border-t border-border">
                {(Object.keys(languageNames) as Language[]).map((lang) => (
                  <button
                    key={lang}
                    onClick={() => {
                      setLanguage(lang);
                      setShowLangPicker(false);
                    }}
                    className={`w-full text-left px-4 py-3 text-sm transition-colors ${
                      language === lang
                        ? "bg-primary/10 text-primary font-medium"
                        : "hover:bg-secondary text-foreground"
                    }`}
                  >
                    {languageNames[lang]}
                  </button>
                ))}
              </div>
            )}
          </div>
        </section>

        {/* Profile Edit */}
        <section>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
            {t.account}
          </h2>
          <div className="rounded-xl bg-card shadow-sm overflow-hidden">
            {!editingProfile ? (
              <button
                onClick={() => setEditingProfile(true)}
                className="flex items-center justify-between w-full p-4"
              >
                <div className="flex items-center gap-3">
                  <User className="h-5 w-5 text-muted-foreground" />
                  <span className="text-sm font-medium">{t.editProfile}</span>
                </div>
                <ChevronRight className="h-4 w-4 text-muted-foreground" />
              </button>
            ) : (
              <div className="p-4 space-y-4">
                <div>
                  <label className="text-xs font-medium text-muted-foreground mb-1 block">{t.name}</label>
                  <Input value={name} onChange={(e) => setName(e.target.value)} />
                </div>
                <div>
                  <label className="text-xs font-medium text-muted-foreground mb-1 block">{t.bio}</label>
                  <Input value={bio} onChange={(e) => setBio(e.target.value)} />
                </div>
                <div className="flex gap-2">
                  <Button size="sm" onClick={saveProfile} disabled={saving} className="flex-1">
                    {saving ? "..." : t.save}
                  </Button>
                  <Button size="sm" variant="outline" onClick={() => setEditingProfile(false)} className="flex-1">
                    {t.cancel}
                  </Button>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Draft encryption (E2EE) */}
        <section>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
            Draft encryption
          </h2>
          <div className="rounded-xl bg-card shadow-sm p-4 space-y-3">
            <div className="flex items-center gap-3">
              {vault.status === "unlocked" ? (
                <ShieldCheck className="h-5 w-5 text-primary" />
              ) : (
                <Lock className="h-5 w-5 text-muted-foreground" />
              )}
              <span className="text-sm font-medium">
                {vault.status === "unlocked" && "Unlocked on this device"}
                {vault.status === "locked" && "Locked"}
                {vault.status === "none" && "Not set up"}
                {vault.status === "loading" && "Checking..."}
              </span>
            </div>
            <p className="text-xs text-muted-foreground">
              Unpublished chapters are encrypted in your browser before upload. Published books are public by design
              and are not encrypted.
            </p>
            {vault.status === "unlocked" ? (
              <Button variant="outline" className="w-full" onClick={vault.lock}>
                Lock now
              </Button>
            ) : (
              (vault.status === "locked" || vault.status === "none") && (
                <Button variant="outline" className="w-full" onClick={() => void vault.ensureUnlocked()}>
                  {vault.status === "none" ? "Set up encryption" : "Unlock"}
                </Button>
              )
            )}
          </div>
        </section>

        {/* Privacy */}
        <section>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-3">
            Legal
          </h2>
          <div className="rounded-xl bg-card shadow-sm overflow-hidden">
            <button
              onClick={() => window.open(PRIVACY_URL, "_blank", "noopener,noreferrer")}
              className="flex items-center justify-between w-full p-4"
            >
              <div className="flex items-center gap-3">
                <Shield className="h-5 w-5 text-muted-foreground" />
                <span className="text-sm font-medium">Privacy Policy</span>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            </button>
          </div>
        </section>

        {/* Sign Out */}
        <Button variant="ghost" className="w-full text-destructive" onClick={handleSignOut}>
          <LogOut className="h-4 w-4 mr-2" /> {t.signOut}
        </Button>

        {/* Danger zone */}
        <section>
          <h2 className="text-xs font-semibold uppercase tracking-wider text-destructive mb-3">
            Danger Zone
          </h2>
          <div className="rounded-xl bg-card shadow-sm p-4 space-y-3">
            <p className="text-sm text-muted-foreground">
              Delete all your BookNest data (profile, books, comments, drafts) permanently. This cannot be undone.
            </p>
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="destructive" className="w-full" disabled={!user}>
                  <Trash2 className="h-4 w-4 mr-2" /> Delete Account
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete your account?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This permanently deletes your BookNest profile, books, comments and encryption keys.
                    This action cannot be undone.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>{t.cancel}</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={handleDeleteAccount}
                    disabled={deleting}
                    className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                  >
                    {deleting ? "Deleting..." : "Delete Forever"}
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </section>

        <div className="pb-4 text-center">
          <PrivacyPolicyLink />
        </div>
      </div>

    </div>
  );
};

export default SettingsPage;
