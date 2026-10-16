import { useState, useEffect } from "react";
import { supabase } from "@/integrations/supabase/client";

export interface DbBook {
  id: string;
  title: string;
  description: string | null;
  genre: string | null;
  cover_url: string | null;
  is_published: boolean;
  likes_count: number;
  comments_count: number;
  reads_count: number;
  created_at: string;
  user_id: string;
  author_name?: string;
  author_avatar?: string;
}

export interface DbChapter {
  id: string;
  book_id: string;
  title: string;
  content: string;
  order: number;
  is_encrypted?: boolean;
}

// Encrypted draft chapter reader e dekhano jay na (key shudhu editor e unlock hole thake)
const LOCKED_NOTE = "🔒 This draft chapter is end-to-end encrypted. Open it in the editor and unlock to read it.";

export const usePublishedBooks = () => {
  const [books, setBooks] = useState<DbBook[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchBooks = async () => {
    setLoading(true);
    const { data: booksData } = await supabase
      .from("books")
      .select("*")
      .eq("is_published", true)
      .order("created_at", { ascending: false });

    if (booksData && booksData.length > 0) {
      const userIds = [...new Set(booksData.map((b) => b.user_id))];
      const { data: profiles } = await supabase
        .from("profiles")
        .select("user_id, name, avatar_url")
        .in("user_id", userIds);

      const enriched = booksData.map((b) => {
        const profile = profiles?.find((p) => p.user_id === b.user_id);
        return {
          ...b,
          author_name: profile?.name || "Unknown Author",
          author_avatar: profile?.avatar_url || "",
        };
      });
      setBooks(enriched);
    } else {
      setBooks([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchBooks();
  }, []);

  return { books, loading, refetch: fetchBooks };
};

export const useBookWithChapters = (bookId: string | undefined) => {
  const [book, setBook] = useState<DbBook | null>(null);
  const [chapters, setChapters] = useState<DbChapter[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!bookId) return;
    const fetch = async () => {
      setLoading(true);
      const { data: bookData } = await supabase
        .from("books")
        .select("*")
        .eq("id", bookId)
        .single();

      if (bookData) {
        const { data: profile } = await supabase
          .from("profiles")
          .select("name, avatar_url")
          .eq("user_id", bookData.user_id)
          .single();

        setBook({
          ...bookData,
          author_name: profile?.name || "Unknown",
          author_avatar: profile?.avatar_url || "",
        });

        const { data: chaptersData } = await supabase
          .from("chapters")
          .select("*")
          .eq("book_id", bookId)
          .order("order", { ascending: true });

        setChapters(
          (chaptersData || []).map((c) =>
            c.is_encrypted ? { ...c, title: c.title || "Encrypted chapter", content: LOCKED_NOTE } : c,
          ),
        );
      }
      setLoading(false);
    };
    fetch();
  }, [bookId]);

  return { book, chapters, loading };
};
