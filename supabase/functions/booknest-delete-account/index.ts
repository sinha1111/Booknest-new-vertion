// BookNest account data deletion.
//
// GURUTTOPURNO: eta shudhu BookNest er data (booknest schema + booknest-avatars bucket) muche.
// auth.users er user ke delete kore NA, karon ei Supabase project e onno app gulo-o same login use kore —
// delete korle oi app gulor account-o chole jeto.
import { createClient } from "npm:@supabase/supabase-js@2";

const AVATAR_BUCKET = "booknest-avatars";
const allowedOrigin = Deno.env.get("ALLOWED_ORIGIN") ?? "*"; // production e apnar site er origin set korun

const corsHeaders = {
  "Access-Control-Allow-Origin": allowedOrigin,
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Vary": "Origin",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...corsHeaders, "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405);

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return json({ error: "Unauthorized" }, 401);

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

    // User er nijer JWT diye client: RPC te auth.uid() ei user hoy
    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
      db: { schema: "booknest" },
      auth: { persistSession: false },
    });

    const { data: userData, error: userError } = await userClient.auth.getUser();
    if (userError || !userData.user) return json({ error: "Unauthorized" }, 401);
    const userId = userData.user.id;

    // 1) DB data (booknest schema)
    const { error: rpcError } = await userClient.rpc("delete_my_data");
    if (rpcError) throw rpcError;

    // 2) Avatar file (shudhu nijer folder)
    const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
    const { data: files, error: listError } = await admin.storage.from(AVATAR_BUCKET).list(userId, { limit: 100 });
    if (listError) throw listError;
    if (files && files.length > 0) {
      const { error: rmError } = await admin.storage
        .from(AVATAR_BUCKET)
        .remove(files.map((f) => `${userId}/${f.name}`));
      if (rmError) throw rmError;
    }

    return json({ success: true });
  } catch (e) {
    console.error("booknest-delete-account error:", e); // detail shudhu server log e
    return json({ error: "Could not delete account data" }, 500);
  }
});
