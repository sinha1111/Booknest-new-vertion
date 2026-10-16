/**
 * Client-side end-to-end encryption for DRAFT chapters.
 *
 * Model
 * -----
 *  - Browser e ekta random 256-bit DEK (data key) toiri hoy. Chapter gulo AES-256-GCM diye DEK dia encrypt hoy.
 *  - DEK ke 2 vabe wrap kore server e rakha hoy (server shudhu wrapped/encrypted DEK dekhe):
 *      1) Vault passphrase -> PBKDF2-SHA256 (600k iterations) -> KEK -> AES-GCM(DEK)
 *      2) Recovery code (160-bit random) -> HKDF-SHA256 -> KEK -> AES-GCM(DEK)
 *  - Passphrase / recovery code / DEK kokhono server e jay na.
 *  - Ciphertext user ar book er shathe bound (AES-GCM AAD) — onno user/book e copy-paste kore khola jay na.
 *
 * Limitation (je tothyo ta shotti): passphrase + recovery code duto-i hariye gele draft ar uddhar kora jabe na.
 */

const enc = new TextEncoder();
const dec = new TextDecoder();

export const KDF_ITERATIONS = 600_000;
export const CIPHER_PREFIX = "e2ee.v1.";

export interface VaultRecord {
  kdf_salt: string;
  kdf_iterations: number;
  wrapped_dek: string;
  wrap_iv: string;
  recovery_salt: string;
  recovery_wrapped_dek: string;
  recovery_iv: string;
}

// ---------- helpers ----------
const toB64 = (buf: ArrayBuffer | Uint8Array): string => {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
  let bin = "";
  for (let i = 0; i < bytes.length; i += 0x8000) {
    bin += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  }
  return btoa(bin);
};

const fromB64 = (b64: string): Uint8Array => {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
};

const rand = (n: number): Uint8Array => crypto.getRandomValues(new Uint8Array(n));

const BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
const toBase32 = (bytes: Uint8Array): string => {
  let bits = 0;
  let value = 0;
  let out = "";
  for (const b of bytes) {
    value = (value << 8) | b;
    bits += 8;
    while (bits >= 5) {
      out += BASE32[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }
  if (bits > 0) out += BASE32[(value << (5 - bits)) & 31];
  return out;
};

const fromBase32 = (s: string): Uint8Array => {
  const clean = s.toUpperCase().replace(/[^A-Z2-7]/g, "");
  let bits = 0;
  let value = 0;
  const out: number[] = [];
  for (const ch of clean) {
    value = (value << 5) | BASE32.indexOf(ch);
    bits += 5;
    if (bits >= 8) {
      out.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }
  return new Uint8Array(out);
};

const formatRecoveryCode = (bytes: Uint8Array): string =>
  (toBase32(bytes).match(/.{1,4}/g) ?? []).join("-");

const aadDek = (userId: string) => enc.encode(`booknest:dek:v1:${userId}`);
const aadChapter = (userId: string, bookId: string) =>
  enc.encode(`booknest:chapter:v1:${userId}:${bookId}`);

// ---------- key derivation ----------
async function kekFromPassphrase(passphrase: string, salt: Uint8Array, iterations: number): Promise<CryptoKey> {
  const base = await crypto.subtle.importKey(
    "raw",
    enc.encode(passphrase.normalize("NFKC")),
    "PBKDF2",
    false,
    ["deriveKey"],
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", hash: "SHA-256", salt, iterations },
    base,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

async function kekFromRecovery(code: string, salt: Uint8Array): Promise<CryptoKey> {
  const raw = fromBase32(code);
  if (raw.length !== 20) throw new Error("Invalid recovery code");
  const base = await crypto.subtle.importKey("raw", raw, "HKDF", false, ["deriveKey"]);
  return crypto.subtle.deriveKey(
    { name: "HKDF", hash: "SHA-256", salt, info: enc.encode("booknest:recovery:v1") },
    base,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

const importDek = (raw: Uint8Array): Promise<CryptoKey> =>
  crypto.subtle.importKey("raw", raw, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);

async function wrap(kek: CryptoKey, dekRaw: Uint8Array, userId: string) {
  const iv = rand(12);
  const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv, additionalData: aadDek(userId) }, kek, dekRaw);
  return { iv: toB64(iv), ct: toB64(ct) };
}

async function unwrap(kek: CryptoKey, ct: string, iv: string, userId: string): Promise<Uint8Array> {
  const raw = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: fromB64(iv), additionalData: aadDek(userId) },
    kek,
    fromB64(ct),
  );
  return new Uint8Array(raw);
}

// ---------- vault lifecycle ----------
export function checkPassphraseStrength(p: string): string | null {
  if (p.length < 10) return "Passphrase must be at least 10 characters";
  if (new Set(p).size < 5) return "Passphrase is too repetitive";
  return null;
}

export async function createVault(passphrase: string, userId: string) {
  const dekRaw = rand(32);

  const salt = rand(16);
  const kek = await kekFromPassphrase(passphrase, salt, KDF_ITERATIONS);
  const w = await wrap(kek, dekRaw, userId);

  const recoveryBytes = rand(20);
  const recoverySalt = rand(16);
  const rkek = await kekFromRecovery(formatRecoveryCode(recoveryBytes), recoverySalt);
  const rw = await wrap(rkek, dekRaw, userId);

  const record: VaultRecord = {
    kdf_salt: toB64(salt),
    kdf_iterations: KDF_ITERATIONS,
    wrapped_dek: w.ct,
    wrap_iv: w.iv,
    recovery_salt: toB64(recoverySalt),
    recovery_wrapped_dek: rw.ct,
    recovery_iv: rw.iv,
  };
  const dek = await importDek(dekRaw);
  dekRaw.fill(0);
  return { record, dek, recoveryCode: formatRecoveryCode(recoveryBytes) };
}

export async function unlockWithPassphrase(record: VaultRecord, passphrase: string, userId: string): Promise<CryptoKey> {
  const kek = await kekFromPassphrase(passphrase, fromB64(record.kdf_salt), record.kdf_iterations);
  const raw = await unwrap(kek, record.wrapped_dek, record.wrap_iv, userId); // throws on wrong passphrase
  const dek = await importDek(raw);
  raw.fill(0);
  return dek;
}

/** Passphrase bhule gele: recovery code diye DEK ber kore notun passphrase e re-wrap kora hoy. */
export async function resetPassphraseWithRecovery(
  record: VaultRecord,
  recoveryCode: string,
  newPassphrase: string,
  userId: string,
): Promise<{ record: VaultRecord; dek: CryptoKey }> {
  const rkek = await kekFromRecovery(recoveryCode, fromB64(record.recovery_salt));
  const raw = await unwrap(rkek, record.recovery_wrapped_dek, record.recovery_iv, userId);

  const salt = rand(16);
  const kek = await kekFromPassphrase(newPassphrase, salt, KDF_ITERATIONS);
  const w = await wrap(kek, raw, userId);

  const next: VaultRecord = {
    ...record,
    kdf_salt: toB64(salt),
    kdf_iterations: KDF_ITERATIONS,
    wrapped_dek: w.ct,
    wrap_iv: w.iv,
  };
  const dek = await importDek(raw);
  raw.fill(0);
  return { record: next, dek };
}

/** Purono passphrase jana thakle notun passphrase set. */
export async function changePassphrase(
  record: VaultRecord,
  oldPassphrase: string,
  newPassphrase: string,
  userId: string,
): Promise<VaultRecord> {
  const okek = await kekFromPassphrase(oldPassphrase, fromB64(record.kdf_salt), record.kdf_iterations);
  const raw = await unwrap(okek, record.wrapped_dek, record.wrap_iv, userId);
  const salt = rand(16);
  const kek = await kekFromPassphrase(newPassphrase, salt, KDF_ITERATIONS);
  const w = await wrap(kek, raw, userId);
  raw.fill(0);
  return { ...record, kdf_salt: toB64(salt), kdf_iterations: KDF_ITERATIONS, wrapped_dek: w.ct, wrap_iv: w.iv };
}

// ---------- chapter encryption ----------
export const isEncryptedPayload = (s: string | null | undefined): boolean =>
  typeof s === "string" && s.startsWith(CIPHER_PREFIX);

export async function encryptChapter(
  dek: CryptoKey,
  ctx: { userId: string; bookId: string },
  chapter: { title: string; content: string },
): Promise<string> {
  const iv = rand(12);
  const plaintext = enc.encode(JSON.stringify({ t: chapter.title, c: chapter.content }));
  const ct = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv, additionalData: aadChapter(ctx.userId, ctx.bookId) },
    dek,
    plaintext,
  );
  return `${CIPHER_PREFIX}${toB64(iv)}.${toB64(ct)}`;
}

export async function decryptChapter(
  dek: CryptoKey,
  ctx: { userId: string; bookId: string },
  payload: string,
): Promise<{ title: string; content: string }> {
  if (!isEncryptedPayload(payload)) throw new Error("Not an encrypted payload");
  const [ivB64, ctB64] = payload.slice(CIPHER_PREFIX.length).split(".");
  if (!ivB64 || !ctB64) throw new Error("Malformed ciphertext");
  const pt = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: fromB64(ivB64), additionalData: aadChapter(ctx.userId, ctx.bookId) },
    dek,
    fromB64(ctB64),
  );
  const obj = JSON.parse(dec.decode(pt));
  return { title: String(obj.t ?? ""), content: String(obj.c ?? "") };
}
