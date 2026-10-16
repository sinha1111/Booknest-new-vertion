import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Heart, MessageCircle, Eye, Share2, BookOpen, UserPlus, UserCheck, Send, Trash2 } from "lucide-react";
import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useBookWithChapters } from "@/hooks/useBooks";
import { useAuth } from "@/contexts/AuthContext";
import { supabase } from "@/integrations/supabase/client";
import { toast } from "sonner";

interface Comment {
  id: string;
  user_id: string;
  content: string;
  created_at: string;
  profile?: {
    name: string;
    avatar_url: string;
  };
}

const BookDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { book, chapters, loading } = useBookWithChapters(id);
  const { user } = useAuth();
  const [liked, setLiked] = useState(false);
  const [following, setFollowing] = useState(false);
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [likesCount, setLikesCount] = useState(0);
  const [commentsCount, setCommentsCount] = useState(0);

  useEffect(() => {
    if (!book) return;
    setLikesCount(book.likes_count);
    setCommentsCount(book.comments_count);
    loadComments();

    if (!user) return;
    supabase.from("likes").select("id").eq("user_id", user.id).eq("book_id", book.id).maybeSingle()
      .then(({ data }) => setLiked(!!data));
    supabase.from("follows").select("id").eq("follower_id", user.id).eq("following_id", book.user_id).maybeSingle()
      .then(({ data }) => setFollowing(!!data));
  }, [user, book]);

  const loadComments = async () => {
    if (!book) return;
    const { data: commentsData } = await (supabase as any)
      .from("comments")
      .select("id, user_id, content, created_at")
      .eq("book_id", book.id)
      .order("created_at", { ascending: false });

    if (commentsData && commentsData.length > 0) {
      const userIds = [...new Set(commentsData.map((c: any) => c.user_id))] as string[];
      const { data: profiles } = await supabase
        .from("profiles")
        .select("user_id, name, avatar_url")
        .in("user_id", userIds);

      const enriched = commentsData.map(c => ({
        ...c,
        profile: profiles?.find(p => p.user_id === c.user_id)
          ? { name: profiles.find(p => p.user_id === c.user_id)!.name, avatar_url: profiles.find(p => p.user_id === c.user_id)!.avatar_url || "" }
          : { name: "Unknown", avatar_url: "" }
      }));
      setComments(enriched);
    } else {
      setComments([]);
    }
  };

  const handleComment = async () => {
    if (!user) { navigate("/auth"); return; }
    if (!book || !newComment.trim()) return;
    setSubmitting(true);
    const { error } = await (supabase as any).from("comments").insert({
      user_id: user.id,
      book_id: book.id,
      content: newComment.trim()
    });
    if (!error) {
      setNewComment("");
      setCommentsCount(c => c + 1); // DB trigger ta asol count maintain kore
      loadComments();
      toast.success("Comment added!");
    }
    setSubmitting(false);
  };

  const deleteComment = async (commentId: string) => {
    const { error } = await (supabase as any).from("comments").delete().eq("id", commentId);
    if (!error) {
      setCommentsCount(c => Math.max(0, c - 1));
      loadComments();
      toast.success("Comment deleted");
    }
  };

  const toggleLike = async () => {
    if (!user) { navigate("/auth"); return; }
    if (!book) return;
    if (liked) {
      const { error } = await supabase.from("likes").delete().eq("user_id", user.id).eq("book_id", book.id);
      if (error) { toast.error("Could not remove like"); return; }
      setLiked(false);
      setLikesCount(c => Math.max(0, c - 1));
      toast.success("Removed like");
    } else {
      const { error } = await supabase.from("likes").insert({ user_id: user.id, book_id: book.id });
      if (error) { toast.error("Could not like this book"); return; }
      setLiked(true);
      setLikesCount(c => c + 1);
      toast.success("Liked!");
    }
  };

  const toggleFollow = async () => {
    if (!user) { navigate("/auth"); return; }
    if (!book) return;
    if (book.user_id === user.id) return;
    if (following) {
      const { error } = await supabase.from("follows").delete().eq("follower_id", user.id).eq("following_id", book.user_id);
      if (error) { toast.error("Could not unfollow"); return; }
      setFollowing(false);
      toast.success("Unfollowed");
    } else {
      const { error } = await supabase.from("follows").insert({ follower_id: user.id, following_id: book.user_id });
      if (error) { toast.error("Could not follow"); return; }
      setFollowing(true);
      toast.success("Following!");
    }
  };

  const formatCount = (n: number) => (n >= 1000 ? (n / 1000).toFixed(1) + "k" : n.toString());

  const timeAgo = (dateStr: string) => {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  if (loading) {
    return <div className="flex min-h-screen items-center justify-center"><p className="text-muted-foreground">Loading...</p></div>;
  }

  if (!book) {
    return <div className="flex min-h-screen items-center justify-center"><p className="text-muted-foreground">Book not found</p></div>;
  }

  return (
    <div className="min-h-screen pb-20">
      <div className="sticky top-0 z-40 flex items-center gap-3 bg-background/80 px-4 pt-safe pb-3 backdrop-blur-xl">
        <button onClick={() => navigate(-1)} className="rounded-full p-1.5 hover:bg-secondary">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="flex-1" />
        <button className="rounded-full p-1.5 hover:bg-secondary">
          <Share2 className="h-5 w-5" />
        </button>
      </div>

      <div className="mx-auto max-w-lg px-5">
        <div className="mx-auto aspect-[3/4] w-48 overflow-hidden rounded-2xl bg-gradient-to-br from-primary/30 via-primary/10 to-accent/20 shadow-lg">
          {book.cover_url ? (
            <img src={book.cover_url} alt={book.title} className="h-full w-full object-cover" />
          ) : (
            <div className="flex h-full flex-col items-center justify-center p-6">
              <span className="font-display text-lg font-bold text-center leading-tight text-primary/80">{book.title}</span>
              <span className="mt-3 text-xs text-muted-foreground">{book.author_name}</span>
            </div>
          )}
        </div>

        <div className="mt-5 text-center">
          <h1 className="font-display text-2xl font-bold">{book.title}</h1>
          <button onClick={toggleFollow} className="mt-1 flex items-center justify-center gap-2 mx-auto">
            {book.author_avatar ? (
              <img src={book.author_avatar} alt={book.author_name} className="h-6 w-6 rounded-full object-cover" />
            ) : (
              <div className="h-6 w-6 rounded-full bg-secondary flex items-center justify-center">
                <span className="text-[10px] font-bold">{book.author_name?.charAt(0)}</span>
              </div>
            )}
            <span className="text-sm text-muted-foreground">{book.author_name}</span>
            {user && book.user_id !== user.id && (
              following ? <UserCheck className="h-4 w-4 text-primary" /> : <UserPlus className="h-4 w-4 text-muted-foreground" />
            )}
          </button>
        </div>

        <div className="mt-4 flex items-center justify-center gap-6 text-sm text-muted-foreground">
          <span className="flex items-center gap-1"><Heart className={`h-4 w-4 ${liked ? "fill-current text-destructive" : ""}`} /> {formatCount(likesCount)}</span>
          <span className="flex items-center gap-1"><MessageCircle className="h-4 w-4" /> {formatCount(commentsCount)}</span>
          <span className="flex items-center gap-1"><Eye className="h-4 w-4" /> {formatCount(book.reads_count)}</span>
        </div>

        {book.genre && (
          <div className="mt-4 flex items-center justify-center gap-3">
            <span className="rounded-full bg-secondary px-3 py-1 text-xs font-medium text-secondary-foreground">{book.genre}</span>
            <span className="rounded-full bg-accent/10 px-3 py-1 text-xs font-semibold text-accent">Free</span>
          </div>
        )}

        <div className="mt-6 flex gap-3">
          <Button className="flex-1 gap-2" onClick={() => navigate(`/read/${book.id}`)}>
            <BookOpen className="h-4 w-4" /> Start Reading
          </Button>
          <Button variant="outline" size="icon" onClick={toggleLike} className={liked ? "text-destructive border-destructive/30" : ""}>
            <Heart className={`h-4 w-4 ${liked ? "fill-current" : ""}`} />
          </Button>
        </div>

        {book.description && (
          <div className="mt-6">
            <h2 className="font-display text-base font-semibold mb-2">About This Book</h2>
            <p className="text-sm leading-relaxed text-muted-foreground">{book.description}</p>
          </div>
        )}

        {chapters.length > 0 && (
          <div className="mt-6">
            <h2 className="font-display text-base font-semibold mb-3">Chapters ({chapters.length})</h2>
            <div className="flex flex-col gap-1">
              {chapters.map((ch, i) => (
                <button
                  key={ch.id}
                  onClick={() => navigate(`/read/${book.id}?chapter=${i}`)}
                  className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-secondary/60"
                >
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-secondary text-xs font-semibold text-secondary-foreground">{ch.order}</span>
                  <span className="text-sm font-medium">{ch.title}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Comments Section */}
        <div className="mt-8">
          <h2 className="font-display text-base font-semibold mb-4">Comments ({commentsCount})</h2>
          
          {/* Add Comment */}
          <div className="flex gap-2 mb-6">
            <Textarea
              placeholder={user ? "Write a comment..." : "Login to comment"}
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              className="min-h-[60px] resize-none text-sm"
              disabled={!user}
            />
            <Button
              size="icon"
              onClick={handleComment}
              disabled={!newComment.trim() || submitting}
              className="self-end"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>

          {/* Comments List */}
          <div className="flex flex-col gap-4">
            {comments.length > 0 ? (
              comments.map((comment) => (
                <div key={comment.id} className="rounded-xl bg-card p-3 shadow-sm">
                  <div className="flex items-start gap-3">
                    {comment.profile?.avatar_url ? (
                      <img src={comment.profile.avatar_url} alt={comment.profile.name} className="h-8 w-8 rounded-full object-cover" />
                    ) : (
                      <div className="h-8 w-8 rounded-full bg-secondary flex items-center justify-center flex-shrink-0">
                        <span className="text-xs font-bold text-muted-foreground">{comment.profile?.name?.charAt(0)?.toUpperCase() || "?"}</span>
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold">{comment.profile?.name || "Unknown"}</span>
                        <span className="text-[10px] text-muted-foreground">{timeAgo(comment.created_at)}</span>
                      </div>
                      <p className="text-sm text-muted-foreground mt-1">{comment.content}</p>
                    </div>
                    {user && comment.user_id === user.id && (
                      <button onClick={() => deleteComment(comment.id)} className="rounded-full p-1 hover:bg-destructive/10 text-muted-foreground hover:text-destructive">
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <p className="text-center text-sm text-muted-foreground py-6">No comments yet. Be the first!</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookDetail;
