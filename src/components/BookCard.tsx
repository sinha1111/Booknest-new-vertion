import { Heart, MessageCircle, Eye } from "lucide-react";
import type { DbBook } from "@/hooks/useBooks";
import { useNavigate } from "react-router-dom";

interface BookCardProps {
  book: DbBook;
  variant?: "default" | "compact" | "featured";
}

const BookCard = ({ book, variant = "default" }: BookCardProps) => {
  const navigate = useNavigate();

  const formatCount = (n: number) => {
    if (n >= 1000) return (n / 1000).toFixed(1) + "k";
    return n.toString();
  };

  if (variant === "compact") {
    return (
      <button
        onClick={() => navigate(`/book/${book.id}`)}
        className="flex gap-3 rounded-lg p-2 text-left transition-colors hover:bg-secondary/60"
      >
        <div className="h-20 w-14 flex-shrink-0 overflow-hidden rounded-md bg-gradient-to-br from-primary/20 to-primary/5">
          {book.cover_url ? (
            <img src={book.cover_url} alt={book.title} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full items-center justify-center px-1">
              <span className="font-display text-[8px] font-semibold leading-tight text-primary/70">{book.title}</span>
            </div>
          )}
        </div>
        <div className="flex min-w-0 flex-col justify-center">
          <h4 className="truncate font-display text-sm font-semibold">{book.title}</h4>
          <p className="text-xs text-muted-foreground">{book.author_name}</p>
          <div className="mt-1 flex items-center gap-2 text-[10px] text-muted-foreground">
            <span className="flex items-center gap-0.5"><Heart className="h-3 w-3" /> {formatCount(book.likes_count)}</span>
            <span>Free</span>
          </div>
        </div>
      </button>
    );
  }

  if (variant === "featured") {
    return (
      <button
        onClick={() => navigate(`/book/${book.id}`)}
        className="group relative w-44 flex-shrink-0 text-left"
      >
        <div className="aspect-[3/4] w-full overflow-hidden rounded-xl bg-gradient-to-br from-primary/30 via-primary/10 to-accent/20 shadow-md transition-transform group-hover:scale-[1.02]">
          {book.cover_url ? (
            <img src={book.cover_url} alt={book.title} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full flex-col items-center justify-center p-4">
              <span className="font-display text-sm font-bold leading-tight text-center text-primary/80">{book.title}</span>
              <span className="mt-2 text-[10px] text-muted-foreground">{book.author_name}</span>
            </div>
          )}
        </div>
        <div className="mt-2 px-0.5">
          <h3 className="truncate font-display text-sm font-semibold">{book.title}</h3>
          <p className="text-xs text-muted-foreground">{book.author_name}</p>
          <div className="mt-1 flex items-center gap-2">
            <span className="rounded-full bg-accent/10 px-2 py-0.5 text-[10px] font-semibold text-accent">Free</span>
            <span className="flex items-center gap-0.5 text-[10px] text-muted-foreground">
              <Heart className="h-3 w-3" /> {formatCount(book.likes_count)}
            </span>
          </div>
        </div>
      </button>
    );
  }

  return (
    <button
      onClick={() => navigate(`/book/${book.id}`)}
      className="group flex gap-4 rounded-xl bg-card p-3 text-left shadow-sm transition-all hover:shadow-md animate-fade-in"
    >
      <div className="h-28 w-20 flex-shrink-0 overflow-hidden rounded-lg bg-gradient-to-br from-primary/25 via-primary/10 to-accent/15 shadow-sm">
        {book.cover_url ? (
          <img src={book.cover_url} alt={book.title} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full flex-col items-center justify-center p-2">
            <span className="font-display text-[10px] font-bold leading-tight text-center text-primary/80">{book.title}</span>
          </div>
        )}
      </div>
      <div className="flex min-w-0 flex-1 flex-col">
        <h3 className="font-display text-base font-semibold leading-snug">{book.title}</h3>
        <p className="mt-0.5 text-xs text-muted-foreground">{book.author_name}</p>
        <p className="mt-1 line-clamp-2 text-xs text-muted-foreground/80">{book.description}</p>
        <div className="mt-auto flex items-center gap-3 pt-2">
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <Heart className="h-3.5 w-3.5" /> {formatCount(book.likes_count)}
          </span>
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <MessageCircle className="h-3.5 w-3.5" /> {formatCount(book.comments_count)}
          </span>
          <span className="flex items-center gap-1 text-xs text-muted-foreground">
            <Eye className="h-3.5 w-3.5" /> {formatCount(book.reads_count)}
          </span>
          <span className="ml-auto">
            <span className="rounded-full bg-accent/10 px-2.5 py-0.5 text-xs font-semibold text-accent">Free</span>
          </span>
        </div>
      </div>
    </button>
  );
};

export default BookCard;
