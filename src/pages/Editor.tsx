import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Save, Plus, Trash2, Lock, Globe } from "lucide-react";
import { Button } from "@/components/ui/button";
import { genres } from "@/data/mockData";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";
import { supabase } from "@/integrations/supabase/client";
import { useVault } from "@/contexts/VaultContext";
import { decryptChapter, encryptChapter } from "@/lib/crypto";

interface EditorChapter {
  id: string;
  title: string;
  content: string;
  order: number;
  isNew?: boolean;
}

const Editor = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const vault = useVault();
  const isNew = id === "new";

  const [bookId, setBookId] = useState<string | null>(isNew ? null : id || null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [genre, setGenre] = useState("");
  const [chapters, setChapters] = useState<EditorChapter[]>([
    { id: "temp-1", title: "Chapter 1", content: "", order: 1, isNew: true },
  ]);
  const [activeChapter, setActiveChapter] = useState(0);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(!isNew);
  const [isPublished, setIsPublished] = useState(false);

  useEffect(() => {
    if (!user) { navigate("/auth"); return; }
    if (!isNew && id) loadBook(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, id]);

  const loadBook = async (bId: string) => {
    if (!user) return;
    setLoading(true);
    try {
      const { data: bookData } = await supabase.from("books").select("*").eq("id", bId).single();

      if (!bookData || bookData.user_id !== user.id) {
        toast.error("Book not found");
        navigate("/write", { replace: true });
        return;
      }

      setTitle(bookData.title);
      setDescription(bookData.description || "");
      setGenre(bookData.genre || "");
      setIsPublished(bookData.is_published);

      const { data: chaptersData } = await supabase
        .from("chapters")
        .select("*")
        .eq("book_id", bId)
        .order("order", { ascending: true });

      if (chaptersData && chaptersData.length > 0) {
        let dek: CryptoKey | null = null;
        if (chaptersData.some((c) => c.is_encrypted)) {
          dek = await vault.ensureUnlocked();
          if (!dek) {
            toast.error("Unlock draft encryption to open this draft");
            navigate("/write", { replace: true });
            return;
          }
        }
        const list: EditorChapter[] = await Promise.all(
          chaptersData.map(async (c) => {
            if (c.is_encrypted && dek) {
              const d = await decryptChapter(dek, { userId: user.id, bookId: bId }, c.content);
              return { id: c.id, title: d.title, content: d.content, order: c.order, isNew: false };
            }
            return { id: c.id, title: c.title, content: c.content, order: c.order, isNew: false };
          }),
        );
        setChapters(list);
      } else {
        setChapters([{ id: "temp-1", title: "Chapter 1", content: "", order: 1, isNew: true }]);
      }
    } catch (err) {
      console.error(err);
      toast.error("Could not open this draft (wrong key or corrupted data)");
      navigate("/write", { replace: true });
    } finally {
      setLoading(false);
    }
  };

  const addChapter = () => {
    const newCh: EditorChapter = {
      id: `temp-${Date.now()}`,
      title: `Chapter ${chapters.length + 1}`,
      content: "",
      order: chapters.length + 1,
      isNew: true,
    };
    setChapters([...chapters, newCh]);
    setActiveChapter(chapters.length);
  };

  const updateChapterContent = (content: string) => {
    const updated = [...chapters];
    updated[activeChapter] = { ...updated[activeChapter], content };
    setChapters(updated);
  };

  const updateChapterTitle = (title: string) => {
    const updated = [...chapters];
    updated[activeChapter] = { ...updated[activeChapter], title };
    setChapters(updated);
  };

  const deleteChapter = (index: number) => {
    if (chapters.length <= 1) return;
    const updated = chapters.filter((_, i) => i !== index);
    setChapters(updated);
    if (activeChapter >= updated.length) setActiveChapter(updated.length - 1);
  };

  /**
   * mode "auto":  unpublished book => chapter gulo browser e encrypt hoye jay; published => plain (karon public)
   * mode "plain": publish er age — chapter gulo plain text e save hoy
   * Returns saved book id, ba fail/cancel hole null.
   */
  const persist = async (mode: "auto" | "plain"): Promise<string | null> => {
    if (!user) return null;

    let dek: CryptoKey | null = null;
    if (mode === "auto" && !isPublished) {
      dek = await vault.ensureUnlocked();
      if (!dek) {
        toast.error("Unlock draft encryption to save");
        return null;
      }
    }

    setSaving(true);
    try {
      let currentBookId = bookId;

      if (!currentBookId) {
        const { data, error } = await supabase
          .from("books")
          .insert({ title: title || "Untitled", description, genre, user_id: user.id })
          .select("id")
          .single();
        if (error) throw error;
        currentBookId = data.id;
        setBookId(currentBookId);
        window.history.replaceState(null, "", `/editor/${currentBookId}`);
      } else {
        const { error } = await supabase
          .from("books")
          .update({ title: title || "Untitled", description, genre })
          .eq("id", currentBookId);
        if (error) throw error;
      }

      const bookIdForCrypto = currentBookId;
      const payload = await Promise.all(
        chapters.map(async (ch) => {
          if (dek) {
            const content = await encryptChapter(dek, { userId: user.id, bookId: bookIdForCrypto }, {
              title: ch.title,
              content: ch.content,
            });
            return { title: "", content, is_encrypted: true };
          }
          return { title: ch.title, content: ch.content, is_encrypted: false };
        }),
      );

      // Atomic replace (DB function) — fail korle ager chapter gulo thake
      const { error: chError } = await supabase.rpc("replace_chapters", {
        p_book_id: currentBookId,
        p_chapters: payload,
      });
      if (chError) throw chError;

      setChapters((prev) => prev.map((c) => ({ ...c, isNew: false })));
      return currentBookId;
    } catch (err: any) {
      console.error(err);
      toast.error(err?.message || "Failed to save");
      return null;
    } finally {
      setSaving(false);
    }
  };

  const handleSave = async () => {
    const savedId = await persist("auto");
    if (savedId) toast.success(isPublished ? "Changes saved" : "Encrypted draft saved 🔒");
  };

  const handlePublish = async () => {
    if (!title.trim()) {
      toast.error("Please add a title first");
      return;
    }
    if (!chapters.some((c) => c.content.trim())) {
      toast.error("Write at least one chapter first");
      return;
    }
    if (
      !window.confirm(
        "Publish this book? It becomes public, and its chapters will be stored unencrypted so readers can open them.",
      )
    ) {
      return;
    }

    const savedId = await persist("plain");
    if (!savedId) return;

    const { error } = await supabase.from("books").update({ is_published: true }).eq("id", savedId);
    if (error) {
      toast.error("Failed to publish");
      void persist("auto"); // publish na hole draft abar encrypted kore rakhi
      return;
    }
    setIsPublished(true);
    toast.success("Book published! 🎉");
  };

  if (loading) {
    return <div className="min-h-screen flex items-center justify-center"><p className="text-muted-foreground">Loading...</p></div>;
  }

  return (
    <div className="min-h-screen pb-20">
      <div className="sticky top-0 z-40 border-b border-border bg-background/90 backdrop-blur-xl">
        <div className="mx-auto flex max-w-lg items-center gap-3 px-4 pt-safe pb-3">
          <button onClick={() => navigate("/write")} className="rounded-full p-1.5 hover:bg-secondary">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <span className="flex-1 truncate font-display text-sm font-semibold">{title || "Untitled Book"}</span>
          <Button size="sm" variant="outline" onClick={handleSave} disabled={saving} className="gap-1.5">
            <Save className="h-3.5 w-3.5" /> {saving ? "..." : "Save"}
          </Button>
        </div>
      </div>

      <div className="mx-auto max-w-lg px-5 pt-4">
        <p className="mb-3 flex items-center gap-1.5 text-xs text-muted-foreground">
          {isPublished ? (
            <><Globe className="h-3 w-3" /> Published — public, stored unencrypted</>
          ) : (
            <><Lock className="h-3 w-3" /> Draft — end-to-end encrypted when you save</>
          )}
        </p>
        <div className="space-y-3 mb-6">
          <input type="text" placeholder="Book Title" value={title} onChange={(e) => setTitle(e.target.value)} className="w-full bg-transparent font-display text-xl font-bold outline-none placeholder:text-muted-foreground/40" />
          <textarea placeholder="Write a short description..." value={description} onChange={(e) => setDescription(e.target.value)} rows={2} className="w-full resize-none bg-transparent text-sm outline-none placeholder:text-muted-foreground/40" />
          <select value={genre} onChange={(e) => setGenre(e.target.value)} className="rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none">
            <option value="">Select Genre</option>
            {genres.map((g) => <option key={g} value={g}>{g}</option>)}
          </select>
        </div>

        <div className="hide-scrollbar flex gap-2 overflow-x-auto mb-4">
          {chapters.map((ch, i) => (
            <button
              key={ch.id}
              onClick={() => setActiveChapter(i)}
              className={`group flex flex-shrink-0 items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
                activeChapter === i ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
              }`}
            >
              Ch {i + 1}
              {chapters.length > 1 && (
                <button onClick={(e) => { e.stopPropagation(); deleteChapter(i); }} className="opacity-0 group-hover:opacity-100 transition-opacity">
                  <Trash2 className="h-3 w-3" />
                </button>
              )}
            </button>
          ))}
          <button onClick={addChapter} className="flex flex-shrink-0 items-center gap-1 rounded-lg bg-secondary/50 px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-secondary">
            <Plus className="h-3 w-3" /> Add
          </button>
        </div>

        <div className="space-y-3">
          <input type="text" placeholder="Chapter Title" value={chapters[activeChapter]?.title || ""} onChange={(e) => updateChapterTitle(e.target.value)} className="w-full bg-transparent font-display text-base font-semibold outline-none placeholder:text-muted-foreground/40" />
          <textarea placeholder="Start writing your chapter..." value={chapters[activeChapter]?.content || ""} onChange={(e) => updateChapterContent(e.target.value)} rows={20} className="w-full resize-none rounded-xl border border-border bg-card p-4 font-body text-sm leading-relaxed outline-none focus:border-primary/30 focus:ring-1 focus:ring-primary/20" />
        </div>

        <div className="mt-6 flex gap-3">
          <Button variant="outline" onClick={handleSave} disabled={saving} className="flex-1">
            {isPublished ? "Save changes" : "Save Draft"}
          </Button>
          {!isPublished && (
            <Button className="flex-1" onClick={handlePublish} disabled={saving}>Publish</Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default Editor;
