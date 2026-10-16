import { createClient } from "@supabase/supabase-js";
import type { Database } from "./types";

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL as string | undefined;
const SUPABASE_PUBLISHABLE_KEY = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY as string | undefined;

if (!SUPABASE_URL || !SUPABASE_PUBLISHABLE_KEY) {
  throw new Error("Missing VITE_SUPABASE_URL or VITE_SUPABASE_PUBLISHABLE_KEY — .env.example dekho");
}

/**
 * BookNest shared Supabase project er "booknest" schema te kaj kore.
 * `db.schema` er karone .from() ar .rpc() duto-i booknest schema te jay; auth ar storage project-wide-ই thake.
 * storageKey alada, jate onno app (same origin e, jemon localhost) er session er shathe mishe na jay.
 */
export const supabase = createClient<Database, "booknest">(SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY, {
  db: { schema: "booknest" },
  auth: {
    storageKey: "booknest-auth",
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
});
