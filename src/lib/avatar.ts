import { supabase } from "@/integrations/supabase/client";

export const AVATAR_BUCKET = "booknest-avatars";
export const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

const EXT_BY_MIME: Record<string, string> = {
  "image/jpeg": "jpg",
  "image/png": "png",
  "image/webp": "webp",
};

export function validateAvatar(file: File): string | null {
  if (!EXT_BY_MIME[file.type]) return "Only JPG, PNG or WebP images are allowed";
  if (file.size > MAX_AVATAR_BYTES) return "Image must be 2MB or smaller";
  return null;
}

/** Avatar upload kore public URL ferot dey; fail korle null. */
export async function uploadAvatar(userId: string, file: File): Promise<string | null> {
  const invalid = validateAvatar(file);
  if (invalid) return null;
  const path = `${userId}/avatar.${EXT_BY_MIME[file.type]}`;
  const { error } = await supabase.storage
    .from(AVATAR_BUCKET)
    .upload(path, file, { upsert: true, contentType: file.type, cacheControl: "3600" });
  if (error) {
    console.error("Avatar upload error:", error);
    return null;
  }
  const { data } = supabase.storage.from(AVATAR_BUCKET).getPublicUrl(path);
  return `${data.publicUrl}?v=${Date.now()}`;
}
