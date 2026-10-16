// `npx cap add android` er por android/app/build.gradle ke Play Store er jonno ready kore:
//  - R8 (minify + shrink resources + optimize) release e on
//  - versionCode / versionName set
//  - signing config (keystore.properties theke)
// Idempotent: bar bar chalale ok.
//   node scripts/patch-android.mjs --version-code 2 --version-name 1.1
import fs from "node:fs";

const file = "android/app/build.gradle";
if (!fs.existsSync(file)) {
  console.error("android/app/build.gradle nai — age `npx cap add android` chalao");
  process.exit(1);
}

const args = process.argv.slice(2);
const arg = (k) => {
  const i = args.indexOf(k);
  return i >= 0 ? args[i + 1] : null;
};
const versionCode = arg("--version-code");
const versionName = arg("--version-name");

let s = fs.readFileSync(file, "utf8");
const MARK = ">>> booknest-patched";

// ---- version ----
if (versionCode) {
  if (!/versionCode\s*=?\s*\d+/.test(s)) throw new Error("versionCode line pawa jay nai");
  s = s.replace(/versionCode\s*=?\s*\d+/, `versionCode = ${Number(versionCode)}`);
}
if (versionName) {
  if (!/versionName\s*=?\s*"[^"]*"/.test(s)) throw new Error("versionName line pawa jay nai");
  s = s.replace(/versionName\s*=?\s*"[^"]*"/, `versionName = "${versionName}"`);
}

// ---- R8 + signing ----
if (!s.includes(MARK)) {
  const m = /^android\s*\{/m.exec(s);
  if (!m) throw new Error("`android {` block pawa jay nai");
  const open = m.index + m[0].length - 1; // `{` er index

  // matching `}` khuji (string ar comment skip kore)
  let depth = 0;
  let close = -1;
  for (let i = open; i < s.length; i++) {
    const c = s[i];
    if (c === "/" && s[i + 1] === "/") { while (i < s.length && s[i] !== "\n") i++; continue; }
    if (c === "/" && s[i + 1] === "*") { i = s.indexOf("*/", i + 2) + 1; continue; }
    if (c === '"' || c === "'") { const q = c; i++; while (i < s.length && s[i] !== q) { if (s[i] === "\\") i++; i++; } continue; }
    if (c === "{") depth++;
    if (c === "}") { depth--; if (depth === 0) { close = i; break; } }
  }
  if (close < 0) throw new Error("`android {` er closing brace pawa jay nai");

  const inner = `
    // ${MARK}
    signingConfigs {
        release {
            if (keystoreProperties['storeFile']) {
                storeFile = rootProject.file("../" + keystoreProperties['storeFile'])
                storePassword = keystoreProperties['storePassword']
                keyAlias = keystoreProperties['keyAlias']
                keyPassword = keystoreProperties['keyPassword']
            }
        }
    }
    buildTypes {
        release {
            minifyEnabled = true
            shrinkResources = true
            setProguardFiles([getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'])
            if (keystoreProperties['storeFile']) {
                signingConfig = signingConfigs.release
            }
        }
    }
`;
  s = s.slice(0, close) + inner + s.slice(close);

  const loader = `// ${MARK}
def keystoreProperties = new Properties()
def keystorePropertiesFile = rootProject.file("../keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.withInputStream { keystoreProperties.load(it) }
}

`;
  s = s.slice(0, m.index) + loader + s.slice(m.index);
}

fs.writeFileSync(file, s);
console.log("patched:", file);

// ---- target SDK check ----
const vars = "android/variables.gradle";
if (fs.existsSync(vars)) {
  const v = fs.readFileSync(vars, "utf8");
  const t = /targetSdkVersion\s*=\s*(\d+)/.exec(v);
  if (t) console.log("targetSdkVersion =", t[1], Number(t[1]) < 35 ? "(KOM! Capacitor 8 install hoyeche to?)" : "");
}
