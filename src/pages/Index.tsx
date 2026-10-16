import { useState, useEffect } from "react";
import { BookOpen } from "lucide-react";
import appIcon from "@/assets/app-icon.png";

import BookCard from "@/components/BookCard";
import { usePublishedBooks } from "@/hooks/useBooks";
import { useAuth } from "@/contexts/AuthContext";
import { supabase } from "@/integrations/supabase/client";
import type { DbBook } from "@/hooks/useBooks";

const tabs = ["For You", "Trending", "Latest", "Following"];

const Index = () => {
  const [activeTab, setActiveTab] = useState("For You");
  const { books, loading } = usePublishedBooks();
  const { user } = useAuth();
  const [followingIds, setFollowingIds] = useState<string[]>([]);
  const [likedGenres, setLikedGenres] = useState<string[]>([]);

  useEffect(() => {
    if (user) {
      supabase
        .from("follows")
        .select("following_id")
        .eq("follower_id", user.id)
        .then(({ data }) => {
          if (data) setFollowingIds(data.map((f) => f.following_id));
        });

      // Fetch user's liked books to determine preferred genres
      supabase
        .from("likes")
        .select("book_id")
        .eq("user_id", user.id)
        .then(async ({ data: likesData }) => {
          if (likesData && likesData.length > 0) {
            const bookIds = likesData.map((l) => l.book_id);
            const { data: likedBooks } = await supabase
              .from("books")
              .select("genre")
              .in("id", bookIds);
            if (likedBooks) {
              const genres = likedBooks
                .map((b) => b.genre)
                .filter((g): g is string => !!g && g !== "");
              setLikedGenres(genres);
            }
          }
        });
    }
  }, [user]);

  const trendingBooks = [...books].sort((a, b) => (b.likes_count + b.comments_count) - (a.likes_count + a.comments_count));
  const latestBooks = [...books].sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
  const followingBooks = books.filter((b) => followingIds.includes(b.user_id));

  const forYouBooks = (() => {
    if (!user || likedGenres.length === 0) return trendingBooks;
    // Score books by how many times their genre appears in user's likes
    const genreCount: Record<string, number> = {};
    likedGenres.forEach((g) => { genreCount[g] = (genreCount[g] || 0) + 1; });
    return [...books].sort((a, b) => {
      const scoreA = (a.genre && genreCount[a.genre]) ? genreCount[a.genre] : 0;
      const scoreB = (b.genre && genreCount[b.genre]) ? genreCount[b.genre] : 0;
      if (scoreB !== scoreA) return scoreB - scoreA;
      return (b.likes_count + b.comments_count) - (a.likes_count + a.comments_count);
    });
  })();

  const getBooks = (): DbBook[] => {
    switch (activeTab) {
      case "Trending": return trendingBooks;
      case "Latest": return latestBooks;
      case "Following": return followingBooks;
      default: return forYouBooks;
    }
  };

  const displayBooks = getBooks();

  return (
    <div className="min-h-screen pb-20">
      <header className="sticky top-0 z-40 bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-lg items-center justify-between px-5 pt-safe pb-3">
          <div className="flex items-center gap-2">
            <img src={appIcon} alt="BookNest app icon" width={24} height={24} className="h-6 w-6 rounded-md" />

            <h1 className="font-display text-xl font-bold">BookNest</h1>
          </div>
        </div>
        <div className="hide-scrollbar mx-auto flex max-w-lg gap-1 overflow-x-auto px-5 pb-3">
          {tabs.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-shrink-0 rounded-full px-4 py-1.5 text-sm font-medium transition-all ${
                activeTab === tab
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>
      </header>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <p className="text-muted-foreground">Loading...</p>
        </div>
      ) : (
        <>
          {activeTab === "For You" && trendingBooks.length > 0 && (
            <section className="mb-4">
              <div className="mx-auto max-w-lg px-5">
                <h2 className="font-display text-lg font-semibold mb-3">Popular This Week</h2>
              </div>
              <div className="hide-scrollbar flex gap-3 overflow-x-auto px-5">
                {trendingBooks.slice(0, 4).map((book) => (
                  <BookCard key={book.id} book={book} variant="featured" />
                ))}
              </div>
            </section>
          )}

          <section className="mx-auto max-w-lg px-5">
            {activeTab === "For You" && displayBooks.length > 0 && (
              <h2 className="font-display text-lg font-semibold mb-3 mt-4">All Books</h2>
            )}
            <div className="flex flex-col gap-3">
              {displayBooks.length > 0 ? (
                displayBooks.map((book) => <BookCard key={book.id} book={book} />)
              ) : (
                <div className="py-16 text-center">
                  <BookOpen className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                  <p className="text-sm text-muted-foreground">No books found</p>
                </div>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
};

export default Index;
