import { useState, useEffect } from "react";
import { Plus, FileText, BookOpen } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { supabase } from "@/integrations/supabase/client";

interface Draft {
  id: string;
  title: string;
  genre: string | null;
  is_published: boolean;
  created_at: string;
  updated_at: string;
}

const Write = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [drafts, setDrafts] = useState<Draft[]>([]);
  const [published, setPublished] = useState<Draft[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) {
      navigate("/auth");
      return;
    }
    loadBooks();
  }, [user]);

  const loadBooks = async () => {
    if (!user) return;
    setLoading(true);
    const { data } = await supabase
      .from("books")
      .select("id, title, genre, is_published, created_at, updated_at")
      .eq("user_id", user.id)
      .order("updated_at", { ascending: false });

    if (data) {
      setDrafts(data.filter((b) => !b.is_published));
      setPublished(data.filter((b) => b.is_published));
    }
    setLoading(false);
  };

  const timeAgo = (date: string) => {
    const diff = Date.now() - new Date(date).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  };

  return (
    <div className="min-h-screen pb-20">
      <header className="mx-auto max-w-lg px-5 pt-safe pb-4">
        <h1 className="font-display text-xl font-bold">My Writing</h1>
        <p className="mt-1 text-sm text-muted-foreground">Your drafts and published works</p>
      </header>

      <div className="mx-auto max-w-lg px-5">
        <Button onClick={() => navigate("/editor/new")} className="w-full gap-2 mb-6">
          <Plus className="h-4 w-4" /> Start a New Book
        </Button>

        {loading ? (
          <p className="text-center text-muted-foreground py-8">Loading...</p>
        ) : (
          <>
            <div>
              <h2 className="font-display text-base font-semibold mb-3 flex items-center gap-2">
                <FileText className="h-4 w-4 text-muted-foreground" /> Drafts
              </h2>
              <div className="flex flex-col gap-2">
                {drafts.length > 0 ? drafts.map((draft) => (
                  <button
                    key={draft.id}
                    onClick={() => navigate(`/editor/${draft.id}`)}
                    className="flex items-center gap-4 rounded-xl bg-card p-4 text-left shadow-sm transition-all hover:shadow-md"
                  >
                    <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                      <BookOpen className="h-5 w-5 text-primary" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="truncate font-display text-sm font-semibold">{draft.title || "Untitled"}</h3>
                      <p className="text-xs text-muted-foreground">{draft.genre || "No genre"}</p>
                    </div>
                    <span className="text-[10px] text-muted-foreground whitespace-nowrap">{timeAgo(draft.updated_at)}</span>
                  </button>
                )) : (
                  <p className="text-sm text-muted-foreground text-center py-4">No drafts yet</p>
                )}
              </div>
            </div>

            <div className="mt-8">
              <h2 className="font-display text-base font-semibold mb-3 flex items-center gap-2">
                <BookOpen className="h-4 w-4 text-muted-foreground" /> Published
              </h2>
              {published.length > 0 ? (
                <div className="flex flex-col gap-2">
                  {published.map((book) => (
                    <button
                      key={book.id}
                      onClick={() => navigate(`/editor/${book.id}`)}
                      className="flex items-center gap-4 rounded-xl bg-card p-4 text-left shadow-sm transition-all hover:shadow-md"
                    >
                      <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-accent/10">
                        <BookOpen className="h-5 w-5 text-accent" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="truncate font-display text-sm font-semibold">{book.title}</h3>
                        <p className="text-xs text-muted-foreground">{book.genre || "No genre"}</p>
                      </div>
                      <span className="text-[10px] text-muted-foreground whitespace-nowrap">{timeAgo(book.updated_at)}</span>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="rounded-xl border border-dashed border-border p-8 text-center">
                  <p className="text-sm text-muted-foreground">Your published books will appear here</p>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default Write;
