import { useState } from "react";
import { Search, ChevronDown, ChevronUp } from "lucide-react";
import BookCard from "@/components/BookCard";
import { usePublishedBooks } from "@/hooks/useBooks";
import { genres } from "@/data/mockData";

const Explore = () => {
  const [search, setSearch] = useState("");
  const [selectedGenre, setSelectedGenre] = useState<string | null>(null);
  const [showAllGenres, setShowAllGenres] = useState(false);
  const { books, loading } = usePublishedBooks();

  const filtered = books.filter((b) => {
    const matchesSearch = !search || b.title.toLowerCase().includes(search.toLowerCase()) || (b.author_name || "").toLowerCase().includes(search.toLowerCase());
    const matchesGenre = !selectedGenre || b.genre === selectedGenre;
    return matchesSearch && matchesGenre;
  });

  return (
    <div className="min-h-screen pb-20">
      <header className="sticky top-0 z-40 bg-background/80 backdrop-blur-xl">
        <div className="mx-auto max-w-lg px-5 pt-safe pb-3">
          <h1 className="font-display text-xl font-bold mb-3">Explore</h1>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="text"
              placeholder="Search books or authors..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary/30"
            />
          </div>
        </div>
        <div className="mx-auto max-w-lg px-5 pb-3">
          <div className={`flex flex-wrap gap-2 ${!showAllGenres ? "max-h-8 overflow-hidden" : ""}`}>
            <button
              onClick={() => setSelectedGenre(null)}
              className={`flex-shrink-0 rounded-full px-3 py-1 text-xs font-medium transition-all ${
                !selectedGenre ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground"
              }`}
            >
              All
            </button>
            {genres.map((g) => (
              <button
                key={g}
                onClick={() => setSelectedGenre(selectedGenre === g ? null : g)}
                className={`flex-shrink-0 rounded-full px-3 py-1 text-xs font-medium transition-all ${
                  selectedGenre === g ? "bg-primary text-primary-foreground" : "bg-secondary text-secondary-foreground"
                }`}
              >
                {g}
              </button>
            ))}
          </div>
          <button
            onClick={() => setShowAllGenres(!showAllGenres)}
            className="mt-2 flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
          >
            {showAllGenres ? (
              <>Less <ChevronUp className="h-3 w-3" /></>
            ) : (
              <>More <ChevronDown className="h-3 w-3" /></>
            )}
          </button>
        </div>
      </header>

      <section className="mx-auto max-w-lg px-5">
        {loading ? (
          <div className="py-16 text-center">
            <p className="text-muted-foreground">Loading...</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {filtered.length > 0 ? (
              filtered.map((book) => <BookCard key={book.id} book={book} />)
            ) : (
              <div className="py-16 text-center">
                <p className="font-display text-lg text-muted-foreground">No books found</p>
                <p className="mt-1 text-sm text-muted-foreground/60">Try a different search or genre</p>
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
};

export default Explore;
