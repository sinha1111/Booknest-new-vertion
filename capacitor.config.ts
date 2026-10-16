import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  // Play Store e publish korle appId ar bodlano jay na â€” upload er age eta thik kore nao.
  appId: "com.tricosoft.booknest",
  appName: "BookNest",
  webDir: "dist",
  server: { androidScheme: "https" },
};

export default config;

