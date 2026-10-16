import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  ReactNode,
} from "react";
import { Lock, ShieldCheck, Copy, Check } from "lucide-react";
import { toast } from "sonner";
import { supabase } from "@/integrations/supabase/client";
import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  VaultRecord,
  checkPassphraseStrength,
  createVault,
  resetPassphraseWithRecovery,
  unlockWithPassphrase,
} from "@/lib/crypto";

type VaultStatus = "loading" | "none" | "locked" | "unlocked";

interface VaultContextType {
  status: VaultStatus;
  /** Unlocked na thakle dialog dekhay; unlock hole key, cancel korle null. */
  ensureUnlocked: () => Promise<CryptoKey | null>;
  getKey: () => CryptoKey | null;
  lock: () => void;
}

const VaultContext = createContext<VaultContextType>({
  status: "loading",
  ensureUnlocked: async () => null,
  getKey: () => null,
  lock: () => {},
});

export const useVault = () => useContext(VaultContext);

type Mode = "setup" | "unlock" | "recovery" | "show-code";

export const VaultProvider = ({ children }: { children: ReactNode }) => {
  const { user } = useAuth();
  const userId = user?.id ?? null;

  const [status, setStatus] = useState<VaultStatus>("loading");
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<Mode>("unlock");
  const [recoveryCode, setRecoveryCode] = useState("");

  const dekRef = useRef<CryptoKey | null>(null);
  const recordRef = useRef<VaultRecord | null>(null);
  const resolverRef = useRef<((k: CryptoKey | null) => void) | null>(null);

  // user bodlale / logout hole key memory theke muche fele
  useEffect(() => {
    dekRef.current = null;
    recordRef.current = null;
    if (!userId) {
      setStatus("loading");
      return;
    }
    let cancelled = false;
    setStatus("loading");
    supabase
      .from("user_keys")
      .select("kdf_salt, kdf_iterations, wrapped_dek, wrap_iv, recovery_salt, recovery_wrapped_dek, recovery_iv")
      .eq("user_id", userId)
      .maybeSingle()
      .then(({ data, error }) => {
        if (cancelled) return;
        if (error) {
          console.error("Vault load failed", error);
          setStatus("none");
          return;
        }
        recordRef.current = data as VaultRecord | null;
        setStatus(data ? "locked" : "none");
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const finish = useCallback((key: CryptoKey | null) => {
    resolverRef.current?.(key);
    resolverRef.current = null;
    setOpen(false);
  }, []);

  const ensureUnlocked = useCallback(async (): Promise<CryptoKey | null> => {
    if (dekRef.current) return dekRef.current;
    if (!userId) return null;
    setMode(recordRef.current ? "unlock" : "setup");
    setOpen(true);
    return new Promise((resolve) => {
      resolverRef.current = resolve;
    });
  }, [userId]);

  const lock = useCallback(() => {
    dekRef.current = null;
    setStatus(recordRef.current ? "locked" : "none");
  }, []);

  const value = useMemo(
    () => ({ status, ensureUnlocked, getKey: () => dekRef.current, lock }),
    [status, ensureUnlocked, lock],
  );

  const onUnlocked = (dek: CryptoKey) => {
    dekRef.current = dek;
    setStatus("unlocked");
  };

  return (
    <VaultContext.Provider value={value}>
      {children}
      <VaultDialog
        open={open}
        mode={mode}
        setMode={setMode}
        userId={userId}
        recordRef={recordRef}
        recoveryCode={recoveryCode}
        setRecoveryCode={setRecoveryCode}
        onUnlocked={onUnlocked}
        onDone={() => finish(dekRef.current)}
        onCancel={() => finish(dekRef.current)}
      />
    </VaultContext.Provider>
  );
};

interface DialogProps {
  open: boolean;
  mode: Mode;
  setMode: (m: Mode) => void;
  userId: string | null;
  recordRef: React.MutableRefObject<VaultRecord | null>;
  recoveryCode: string;
  setRecoveryCode: (c: string) => void;
  onUnlocked: (dek: CryptoKey) => void;
  onDone: () => void;
  onCancel: () => void;
}

const VaultDialog = ({
  open,
  mode,
  setMode,
  userId,
  recordRef,
  recoveryCode,
  setRecoveryCode,
  onUnlocked,
  onDone,
  onCancel,
}: DialogProps) => {
  const [pass, setPass] = useState("");
  const [pass2, setPass2] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!open) {
      setPass("");
      setPass2("");
      setCode("");
      setSaved(false);
      setCopied(false);
      setRecoveryCode("");
    }
  }, [open, setRecoveryCode]);

  const handleSetup = async () => {
    if (!userId) return;
    const weak = checkPassphraseStrength(pass);
    if (weak) return toast.error(weak);
    if (pass !== pass2) return toast.error("Passphrases do not match");
    setBusy(true);
    try {
      const { record, dek, recoveryCode } = await createVault(pass, userId);
      const { error } = await supabase.from("user_keys").insert({ user_id: userId, ...record });
      if (error) throw error;
      recordRef.current = record;
      onUnlocked(dek);
      setRecoveryCode(recoveryCode);
      setMode("show-code");
    } catch (e) {
      console.error(e);
      toast.error("Could not set up draft encryption");
    } finally {
      setBusy(false);
    }
  };

  const handleUnlock = async () => {
    if (!userId || !recordRef.current) return;
    setBusy(true);
    try {
      const dek = await unlockWithPassphrase(recordRef.current, pass, userId);
      onUnlocked(dek);
      onDone();
    } catch {
      toast.error("Wrong passphrase");
    } finally {
      setBusy(false);
    }
  };

  const handleRecovery = async () => {
    if (!userId || !recordRef.current) return;
    const weak = checkPassphraseStrength(pass);
    if (weak) return toast.error(weak);
    if (pass !== pass2) return toast.error("Passphrases do not match");
    setBusy(true);
    try {
      const { record, dek } = await resetPassphraseWithRecovery(recordRef.current, code, pass, userId);
      const { error } = await supabase
        .from("user_keys")
        .update({
          kdf_salt: record.kdf_salt,
          kdf_iterations: record.kdf_iterations,
          wrapped_dek: record.wrapped_dek,
          wrap_iv: record.wrap_iv,
        })
        .eq("user_id", userId);
      if (error) throw error;
      recordRef.current = record;
      onUnlocked(dek);
      toast.success("Passphrase reset");
      onDone();
    } catch {
      toast.error("Invalid recovery code");
    } finally {
      setBusy(false);
    }
  };

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(recoveryCode);
      setCopied(true);
    } catch {
      toast.error("Copy failed — please write it down manually");
    }
  };

  const blockClose = mode === "show-code"; // recovery code na dekhe close kora jabe na

  return (
    <Dialog
      open={open}
      onOpenChange={(o) => {
        if (!o && !blockClose) onCancel();
      }}
    >
      <DialogContent className="max-w-[92vw] sm:max-w-md">
        {mode === "setup" && (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <ShieldCheck className="h-5 w-5" /> Encrypt your drafts
              </DialogTitle>
              <DialogDescription>
                Choose a vault passphrase. Your unpublished chapters are encrypted on this device before upload, so
                nobody — not even the server — can read them. This is separate from your login password.
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-3">
              <Input type="password" autoComplete="new-password" placeholder="Vault passphrase (min 10 chars)" value={pass} onChange={(e) => setPass(e.target.value)} />
              <Input type="password" autoComplete="new-password" placeholder="Repeat passphrase" value={pass2} onChange={(e) => setPass2(e.target.value)} />
              <Button className="w-full" onClick={handleSetup} disabled={busy}>
                {busy ? "Setting up..." : "Enable encryption"}
              </Button>
            </div>
          </>
        )}

        {mode === "show-code" && (
          <>
            <DialogHeader>
              <DialogTitle>Save your recovery code</DialogTitle>
              <DialogDescription>
                If you forget your passphrase, this code is the ONLY way to recover your drafts. It is shown once and
                is never stored on the server. Keep it somewhere safe (password manager or paper).
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-3">
              <div className="select-all break-all rounded-lg border border-border bg-secondary p-3 text-center font-mono text-sm tracking-wider">
                {recoveryCode}
              </div>
              <Button variant="outline" className="w-full gap-2" onClick={copyCode}>
                {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />} {copied ? "Copied" : "Copy code"}
              </Button>
              <label className="flex items-start gap-2 text-sm">
                <input type="checkbox" checked={saved} onChange={(e) => setSaved(e.target.checked)} className="mt-1" />
                I have saved my recovery code
              </label>
              <Button className="w-full" disabled={!saved} onClick={onDone}>
                Continue
              </Button>
            </div>
          </>
        )}

        {mode === "unlock" && (
          <>
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                <Lock className="h-5 w-5" /> Unlock your drafts
              </DialogTitle>
              <DialogDescription>Enter your vault passphrase to read and save encrypted drafts.</DialogDescription>
            </DialogHeader>
            <form
              className="space-y-3"
              onSubmit={(e) => {
                e.preventDefault();
                handleUnlock();
              }}
            >
              <Input type="password" autoComplete="current-password" autoFocus placeholder="Vault passphrase" value={pass} onChange={(e) => setPass(e.target.value)} />
              <Button type="submit" className="w-full" disabled={busy || !pass}>
                {busy ? "Unlocking..." : "Unlock"}
              </Button>
              <button type="button" className="w-full text-center text-xs text-muted-foreground underline" onClick={() => { setPass(""); setMode("recovery"); }}>
                Forgot passphrase? Use recovery code
              </button>
            </form>
          </>
        )}

        {mode === "recovery" && (
          <>
            <DialogHeader>
              <DialogTitle>Reset with recovery code</DialogTitle>
              <DialogDescription>Enter your recovery code and choose a new passphrase.</DialogDescription>
            </DialogHeader>
            <div className="space-y-3">
              <Input placeholder="XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX" value={code} onChange={(e) => setCode(e.target.value)} className="font-mono" />
              <Input type="password" autoComplete="new-password" placeholder="New passphrase (min 10 chars)" value={pass} onChange={(e) => setPass(e.target.value)} />
              <Input type="password" autoComplete="new-password" placeholder="Repeat new passphrase" value={pass2} onChange={(e) => setPass2(e.target.value)} />
              <Button className="w-full" onClick={handleRecovery} disabled={busy || !code}>
                {busy ? "Resetting..." : "Reset passphrase"}
              </Button>
              <button type="button" className="w-full text-center text-xs text-muted-foreground underline" onClick={() => setMode("unlock")}>
                Back
              </button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};
