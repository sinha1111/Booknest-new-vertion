import { useParams, useNavigate, useSearchParams } from "react-router-dom";
import { ArrowLeft, ChevronLeft, ChevronRight, Type } from "lucide-react";
import { useState, useEffect } from "react";
import { useBookWithChapters } from "@/hooks/useBooks";
import { useAuth } from "@/contexts/AuthContext";
import { supabase } from "@/integrations/supabase/client";

const Reader = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { book, chapters, loading } = useBookWithChapters(id);
  const { user } = useAuth();
  const initialChapter = parseInt(searchParams.get("chapter") || "0", 10);
  const [chapterIndex, setChapterIndex] = useState(initialChapter);
  const [fontSize, setFontSize] = useState(16);
  const [showControls, setShowControls] = useState(true);
  const [tracked, setTracked] = useState(false);

  // Track view when book loads
  useEffect(() => {
    if (!book || tracked) return;
    setTracked(true);
    const trackRead = async () => {
      // Server nije user ke auth.uid() theke chine; count shudhu 1 bar bare (login user er jonno)
      const { error } = await supabase.rpc("track_book_read", { p_book_id: book.id });
      if (error) console.error("track_book_read failed:", error.message);
    };
    trackRead();
  }, [book, tracked, user]);

  if (loading) {
    return <div className="flex min-h-screen items-center justify-center"><p className="text-muted-foreground">Loading...</p></div>;
  }

  if (!book || chapters.length === 0) {
    return <div className="flex min-h-screen items-center justify-center"><p className="text-muted-foreground">Book not found</p></div>;
  }

  const chapter = chapters[chapterIndex];
  const hasPrev = chapterIndex > 0;
  const hasNext = chapterIndex < chapters.length - 1;

  return (
    <div className="min-h-screen bg-background" onClick={() => setShowControls(!showControls)}>
      <div
        className={`fixed top-0 left-0 right-0 z-50 bg-background/90 backdrop-blur-xl transition-transform duration-300 ${
          showControls ? "translate-y-0" : "-translate-y-full"
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto flex max-w-lg items-center gap-3 px-4 pt-safe pb-3">
          <button onClick={() => navigate(`/book/${book.id}`)} className="rounded-full p-1.5 hover:bg-secondary">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div className="flex-1 min-w-0">
            <p className="truncate text-sm font-medium">{book.title}</p>
            <p className="truncate text-xs text-muted-foreground">{chapter?.title}</p>
          </div>
          <button onClick={() => setFontSize((s) => (s >= 22 ? 14 : s + 2))} className="rounded-full p-1.5 hover:bg-secondary">
            <Type className="h-5 w-5" />
          </button>
        </div>
      </div>

      <article className="mx-auto max-w-lg px-6 pt-28 pb-32">
        <h2 className="font-display text-xl font-bold mb-6 text-center">{chapter?.title}</h2>
        <div className="font-body leading-[1.9] text-foreground/90 whitespace-pre-line" style={{ fontSize: `${fontSize}px` }}>
          {chapter?.content}
        </div>
      </article>

      <div
        className={`fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-background/90 backdrop-blur-xl transition-transform duration-300 ${
          showControls ? "translate-y-0" : "translate-y-full"
        }`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto flex max-w-lg items-center justify-between px-4 py-3 safe-bottom">
          <button onClick={() => hasPrev && setChapterIndex(chapterIndex - 1)} disabled={!hasPrev} className="flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium transition-colors hover:bg-secondary disabled:opacity-30">
            <ChevronLeft className="h-4 w-4" /> Previous
          </button>
          <span className="text-xs text-muted-foreground">{chapterIndex + 1} / {chapters.length}</span>
          <button onClick={() => hasNext && setChapterIndex(chapterIndex + 1)} disabled={!hasNext} className="flex items-center gap-1 rounded-lg px-3 py-2 text-sm font-medium transition-colors hover:bg-secondary disabled:opacity-30">
            Next <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default Reader;
