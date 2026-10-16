import { Settings, BookOpen, Heart, Users, LogOut, Camera, Pencil, X, Check, UserMinus, MessageCircle, Eye, Trash2 } from "lucide-react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useState, useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext";
import { useNavigate } from "react-router-dom";
import { supabase } from "@/integrations/supabase/client";
import { uploadAvatar, validateAvatar } from "@/lib/avatar";
import { toast } from "sonner";
import { genres } from "@/data/mockData";
import { Badge } from "@/components/ui/badge";
import PrivacyPolicyLink from "@/components/PrivacyPolicyLink";


interface ProfileData {
  name: string;
  bio: string;
  avatar_url: string;
  category: string;
  followers: number;
  following: number;
}

interface BookData {
  id: string;
  title: string;
  description: string;
  genre: string;
  cover_url: string;
  is_published: boolean;
  likes_count: number;
  reads_count: number;
  created_at: string;
}

interface FollowedUser {
  id: string;
  following_id: string;
  follower_id?: string;
  profile: {
    name: string;
    avatar_url: string;
    bio: string;
    category: string;
  } | null;
}

interface CommentData {
  id: string;
  content: string;
  created_at: string;
  book_id: string;
  book_title?: string;
}

interface ReadHistoryData {
  id: string;
  book_id: string;
  created_at: string;
  book_title?: string;
  book_cover?: string;
  book_genre?: string;
}

const tabs = ["My Books", "Liked", "Comments", "Views", "Followers", "Following"];

const Profile = () => {
  const [activeTab, setActiveTab] = useState("My Books");
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  const [profile, setProfile] = useState<ProfileData | null>(null);
  const [myBooks, setMyBooks] = useState<BookData[]>([]);
  const [likedBooks, setLikedBooks] = useState<BookData[]>([]);
  const [followingUsers, setFollowingUsers] = useState<FollowedUser[]>([]);
  const [followerUsers, setFollowerUsers] = useState<FollowedUser[]>([]);
  const [userComments, setUserComments] = useState<CommentData[]>([]);
  const [readHistory, setReadHistory] = useState<ReadHistoryData[]>([]);
  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editBio, setEditBio] = useState("");
  const [editCategory, setEditCategory] = useState("");
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user) {
      navigate("/auth");
      return;
    }
    loadProfile();
    loadMyBooks();
    loadLikedBooks();
    loadFollowing();
    loadFollowers();
    loadUserComments();
    loadReadHistory();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  const [profileError, setProfileError] = useState("");

  const loadProfile = async () => {
    if (!user) return;
    setProfileError("");
    // Profile row na thakle age banai (idempotent), tarpor porhi — race condition ar nei
    const { error: ensureError } = await supabase.rpc("ensure_profile");
    if (ensureError) {
      console.error("ensure_profile failed:", ensureError);
      setProfileError(ensureError.message);
      return;
    }
    const { data, error } = await supabase
      .from("profiles")
      .select("name, bio, avatar_url, category, followers, following")
      .eq("user_id", user.id)
      .single();
    if (error) {
      console.error("profile load failed:", error);
      setProfileError(error.message);
      return;
    }
    if (data) {
      // Get real counts from follows table
      const { count: followersCount } = await supabase
        .from("follows")
        .select("id", { count: "exact", head: true })
        .eq("following_id", user.id);
      const { count: followingCount } = await supabase
        .from("follows")
        .select("id", { count: "exact", head: true })
        .eq("follower_id", user.id);
      setProfile({
        ...(data as ProfileData),
        followers: followersCount || 0,
        following: followingCount || 0,
      });
      setEditName(data.name || "");
      setEditBio(data.bio || "");
      setEditCategory((data as any).category || "");
    }
  };

  const loadMyBooks = async () => {
    if (!user) return;
    const { data } = await supabase
      .from("books")
      .select("id, title, description, genre, cover_url, is_published, likes_count, reads_count, created_at")
      .eq("user_id", user.id)
      .order("created_at", { ascending: false });
    if (data) setMyBooks(data);
  };

  const [deletingBookId, setDeletingBookId] = useState<string | null>(null);
  const [deletingAccount, setDeletingAccount] = useState(false);

  const handleDeleteBook = async (bookId: string) => {
    setDeletingBookId(bookId);
    const { error } = await supabase.from("books").delete().eq("id", bookId);
    setDeletingBookId(null);
    if (error) {
      toast.error("Failed to delete book");
      return;
    }
    setMyBooks((prev) => prev.filter((b) => b.id !== bookId));
    toast.success("Book deleted");
  };

  const handleDeleteAccount = async () => {
    setDeletingAccount(true);
    try {
      const { error } = await supabase.functions.invoke("booknest-delete-account");
      if (error) throw error;
      toast.success("Your account has been deleted");
      await supabase.auth.signOut();
      navigate("/auth", { replace: true });
    } catch (e: any) {
      toast.error(e.message || "Failed to delete account");
    } finally {
      setDeletingAccount(false);
    }
  };



  const loadLikedBooks = async () => {
    if (!user) return;
    const { data: likesData } = await supabase
      .from("likes")
      .select("book_id")
      .eq("user_id", user.id);
    if (likesData && likesData.length > 0) {
      const bookIds = likesData.map((l) => l.book_id);
      const { data: booksData } = await supabase
        .from("books")
        .select("id, title, description, genre, cover_url, is_published, likes_count, reads_count, created_at")
        .in("id", bookIds);
      if (booksData) setLikedBooks(booksData);
    } else {
      setLikedBooks([]);
    }
  };

  const loadFollowing = async () => {
    if (!user) return;
    const { data: followsData } = await supabase
      .from("follows")
      .select("id, following_id")
      .eq("follower_id", user.id);
    if (followsData && followsData.length > 0) {
      const userIds = followsData.map((f) => f.following_id);
      const { data: profilesData } = await supabase
        .from("profiles")
        .select("user_id, name, avatar_url, bio, category")
        .in("user_id", userIds);
      const merged = followsData.map((f) => ({
        ...f,
        profile: profilesData?.find((p) => p.user_id === f.following_id)
          ? {
              name: profilesData.find((p) => p.user_id === f.following_id)!.name,
              avatar_url: profilesData.find((p) => p.user_id === f.following_id)!.avatar_url || "",
              bio: profilesData.find((p) => p.user_id === f.following_id)!.bio || "",
              category: (profilesData.find((p) => p.user_id === f.following_id) as any)?.category || "",
            }
          : null,
      }));
      setFollowingUsers(merged);
    } else {
      setFollowingUsers([]);
    }
  };

  const loadFollowers = async () => {
    if (!user) return;
    const { data: followsData } = await supabase
      .from("follows")
      .select("id, follower_id")
      .eq("following_id", user.id);
    if (followsData && followsData.length > 0) {
      const userIds = followsData.map((f) => f.follower_id);
      const { data: profilesData } = await supabase
        .from("profiles")
        .select("user_id, name, avatar_url, bio, category")
        .in("user_id", userIds);
      const merged = followsData.map((f) => ({
        ...f,
        following_id: f.follower_id,
        profile: profilesData?.find((p) => p.user_id === f.follower_id)
          ? {
              name: profilesData.find((p) => p.user_id === f.follower_id)!.name,
              avatar_url: profilesData.find((p) => p.user_id === f.follower_id)!.avatar_url || "",
              bio: profilesData.find((p) => p.user_id === f.follower_id)!.bio || "",
              category: (profilesData.find((p) => p.user_id === f.follower_id) as any)?.category || "",
            }
          : null,
      }));
      setFollowerUsers(merged);
    } else {
      setFollowerUsers([]);
    }
  };

  const loadUserComments = async () => {
    if (!user) return;
    const { data: commentsData } = await (supabase as any)
      .from("comments")
      .select("id, content, created_at, book_id")
      .eq("user_id", user.id)
      .order("created_at", { ascending: false });
    if (commentsData && commentsData.length > 0) {
      const bookIds = [...new Set(commentsData.map((c: any) => c.book_id))] as string[];
      const { data: booksData } = await supabase.from("books").select("id, title").in("id", bookIds);
      const enriched = commentsData.map((c: any) => ({
        ...c,
        book_title: booksData?.find((b) => b.id === c.book_id)?.title || "Unknown",
      }));
      setUserComments(enriched);
    } else {
      setUserComments([]);
    }
  };

  const loadReadHistory = async () => {
    if (!user) return;
    const { data: historyData } = await (supabase as any)
      .from("read_history")
      .select("id, book_id, created_at")
      .eq("user_id", user.id)
      .order("created_at", { ascending: false });
    if (historyData && historyData.length > 0) {
      const bookIds = [...new Set(historyData.map((h: any) => h.book_id))] as string[];
      const { data: booksData } = await supabase.from("books").select("id, title, cover_url, genre").in("id", bookIds);
      const enriched = historyData.map((h: any) => {
        const book = booksData?.find((b) => b.id === h.book_id);
        return { ...h, book_title: book?.title || "Unknown", book_cover: book?.cover_url || "", book_genre: book?.genre || "" };
      });
      setReadHistory(enriched);
    } else {
      setReadHistory([]);
    }
  };

  const handleDeleteComment = async (commentId: string, bookId: string) => {
    const { error } = await (supabase as any).from("comments").delete().eq("id", commentId);
    if (!error) {
      toast.success("Comment deleted");
      loadUserComments();
    }
  };

  const timeAgo = (dateStr: string) => {
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  };

  const handleUnfollow = async (followId: string) => {
    const { error } = await supabase.from("follows").delete().eq("id", followId);
    if (!error) {
      toast.success("Unfollowed!");
      loadFollowing();
    }
  };

  const handleUnlike = async (bookId: string) => {
    if (!user) return;
    const { error } = await supabase.from("likes").delete().eq("user_id", user.id).eq("book_id", bookId);
    if (!error) {
      toast.success("Removed from liked!");
      loadLikedBooks();
    }
  };

  const startEditing = () => {
    if (profile) {
      setEditName(profile.name);
      setEditBio(profile.bio);
      setEditCategory(profile.category);
      setAvatarPreview("");
      setAvatarFile(null);
    }
    setEditing(true);
  };

  const cancelEditing = () => {
    setEditing(false);
    setAvatarFile(null);
    setAvatarPreview("");
  };

  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const invalid = validateAvatar(file);
      if (invalid) {
        toast.error(invalid);
        return;
      }
      setAvatarFile(file);
      setAvatarPreview(URL.createObjectURL(file));
    }
  };

  const saveProfile = async () => {
    if (!user) return;
    setSaving(true);

    let avatarUrl = profile?.avatar_url || "";

    if (avatarFile) {
      const uploaded = await uploadAvatar(user.id, avatarFile);
      if (uploaded) avatarUrl = uploaded;
      else toast.error("Avatar upload failed (JPG/PNG/WebP, max 2MB)");
    }

    const { error } = await supabase
      .from("profiles")
      .update({
        name: editName,
        bio: editBio,
        category: editCategory,
        avatar_url: avatarUrl,
      } as any)
      .eq("user_id", user.id);

    setSaving(false);
    if (error) {
      toast.error("Failed to save profile");
    } else {
      toast.success("Profile updated!");
      setEditing(false);
      loadProfile();
    }
  };

  const handleSignOut = async () => {
    await signOut();
    navigate("/auth");
  };

  if (!user || !profile) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-3 bg-background px-6 text-center">
        {profileError ? (
          <>
            <p className="text-sm font-medium text-destructive">Could not load your profile</p>
            <p className="max-w-xs break-words text-xs text-muted-foreground">{profileError}</p>
            <button onClick={loadProfile} className="rounded-lg bg-primary px-4 py-2 text-sm text-primary-foreground">
              Retry
            </button>
          </>
        ) : (
          <p className="text-muted-foreground">Loading...</p>
        )}
      </div>
    );
  }

  const displayAvatar = editing && avatarPreview ? avatarPreview : profile.avatar_url;

  return (
    <div className="min-h-screen pb-20 bg-background">
      <header className="mx-auto max-w-lg px-5 pt-safe pb-4">
        <div className="flex items-center justify-between">
          <h1 className="font-display text-xl font-bold text-foreground">Profile</h1>
          <button onClick={() => navigate("/settings")} className="rounded-full p-1.5 hover:bg-secondary">
            <Settings className="h-5 w-5 text-muted-foreground" />
          </button>
        </div>
      </header>

      <div className="mx-auto max-w-lg px-5">
        {/* Profile Card */}
        <div className="flex items-start gap-4 mb-4">
          <div className="relative">
            {displayAvatar ? (
              <img src={displayAvatar} alt={profile.name} className="h-16 w-16 rounded-full object-cover ring-2 ring-primary/20" />
            ) : (
              <div className="h-16 w-16 rounded-full bg-secondary flex items-center justify-center ring-2 ring-primary/20">
                <span className="font-display text-lg font-bold text-muted-foreground">
                  {profile.name?.charAt(0)?.toUpperCase() || "?"}
                </span>
              </div>
            )}
            {editing && (
              <label className="absolute -bottom-1 -right-1 bg-primary rounded-full p-1 cursor-pointer shadow-md">
                <Camera className="h-3.5 w-3.5 text-primary-foreground" />
                <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleAvatarChange} className="hidden" />
              </label>
            )}
          </div>
          <div className="flex-1 min-w-0">
            {editing ? (
              <Input value={editName} onChange={(e) => setEditName(e.target.value)} className="font-display text-lg font-bold mb-1" placeholder="Your name" />
            ) : (
              <h2 className="font-display text-lg font-bold text-foreground">{profile.name}</h2>
            )}
            {!editing && profile.category && <Badge variant="secondary" className="text-xs mt-1">{profile.category}</Badge>}
            {!editing && <p className="text-sm text-muted-foreground mt-1">{profile.bio || "No bio yet"}</p>}
          </div>
        </div>

        {editing && (
          <div className="space-y-3 mb-4">
            <div>
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Bio</label>
              <Textarea value={editBio} onChange={(e) => setEditBio(e.target.value)} placeholder="Tell about yourself..." className="resize-none min-h-[70px]" maxLength={200} />
            </div>
            <div>
              <label className="text-xs font-medium text-muted-foreground mb-1 block">Category</label>
              <select value={editCategory} onChange={(e) => setEditCategory(e.target.value)} className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 text-foreground">
                <option value="">Select category</option>
                {genres.map((g) => <option key={g} value={g}>{g}</option>)}
              </select>
            </div>
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-3 gap-3 mb-6">
          {[
            { icon: BookOpen, label: "Books", value: myBooks.length },
            { icon: Users, label: "Followers", value: profile.followers.toLocaleString() },
            { icon: Heart, label: "Following", value: followingUsers.length },
          ].map(({ icon: Icon, label, value }) => (
            <div key={label} className="flex flex-col items-center rounded-xl bg-card p-3 shadow-sm">
              <Icon className="h-4 w-4 text-muted-foreground mb-1" />
              <span className="font-display text-lg font-bold text-foreground">{value}</span>
              <span className="text-[10px] text-muted-foreground">{label}</span>
            </div>
          ))}
        </div>

        {editing ? (
          <div className="flex gap-2 mb-6">
            <Button onClick={saveProfile} disabled={saving} className="flex-1">
              <Check className="h-4 w-4 mr-1" /> {saving ? "Saving..." : "Save"}
            </Button>
            <Button variant="outline" onClick={cancelEditing} className="flex-1">
              <X className="h-4 w-4 mr-1" /> Cancel
            </Button>
          </div>
        ) : (
          <>
            <Button variant="outline" className="w-full mb-3" onClick={startEditing}>
              <Pencil className="h-4 w-4 mr-2" /> Edit Profile
            </Button>
            <Button variant="ghost" className="w-full mb-3 text-destructive" onClick={handleSignOut}>
              <LogOut className="h-4 w-4 mr-2" /> Sign Out
            </Button>
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="destructive" className="w-full mb-6">
                  <Trash2 className="h-4 w-4 mr-2" /> Delete Account
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete your account?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This permanently deletes your profile, books, comments and all other data. This action cannot be undone.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={handleDeleteAccount}
                    disabled={deletingAccount}
                    className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                  >
                    {deletingAccount ? "Deleting..." : "Delete Forever"}
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>

          </>
        )}

        {/* Tabs */}
        <div className="flex gap-1 mb-4 overflow-x-auto pb-1 scrollbar-hide">
          {tabs.map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`whitespace-nowrap rounded-lg px-3 py-2 text-xs font-medium transition-all ${
                activeTab === tab ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-secondary"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* My Books */}
        {activeTab === "My Books" && (
          <div className="flex flex-col gap-3">
            {myBooks.length > 0 ? (
              myBooks.map((book) => (
                <div key={book.id} className="rounded-xl bg-card p-4 shadow-sm">
                  <div className="flex items-start gap-3">
                    {book.cover_url ? (
                      <img src={book.cover_url} alt={book.title} className="h-16 w-12 rounded-lg object-cover" />
                    ) : (
                      <div className="h-16 w-12 rounded-lg bg-secondary flex items-center justify-center">
                        <BookOpen className="h-5 w-5 text-muted-foreground" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-display font-bold text-foreground truncate">{book.title || "Untitled"}</h3>
                      <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">{book.description || "No description"}</p>
                      <div className="flex items-center gap-3 mt-2">
                        {book.genre && <Badge variant="secondary" className="text-[10px]">{book.genre}</Badge>}
                        {!book.is_published && <Badge variant="outline" className="text-[10px] text-muted-foreground">Draft</Badge>}
                        <span className="text-[10px] text-muted-foreground">❤ {book.likes_count}</span>
                        <span className="text-[10px] text-muted-foreground">👁 {book.reads_count}</span>
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-2 mt-3">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1"
                      onClick={() => navigate(`/editor/${book.id}`)}
                    >
                      <Pencil className="h-3.5 w-3.5 mr-1.5" /> Edit
                    </Button>
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button variant="outline" size="sm" className="flex-1 text-destructive hover:text-destructive">
                          <Trash2 className="h-3.5 w-3.5 mr-1.5" /> Delete
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>Delete this book?</AlertDialogTitle>
                          <AlertDialogDescription>
                            "{book.title || "Untitled"}" and its chapters will be permanently deleted. This cannot be undone.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancel</AlertDialogCancel>
                          <AlertDialogAction
                            onClick={() => handleDeleteBook(book.id)}
                            disabled={deletingBookId === book.id}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                          >
                            {deletingBookId === book.id ? "Deleting..." : "Delete"}
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  </div>
                </div>
              ))

            ) : (
              <div className="py-12 text-center">
                <BookOpen className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">No books written yet</p>
                <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/write")}>Start Writing</Button>
              </div>
            )}
          </div>
        )}

        {/* Liked Books */}
        {activeTab === "Liked" && (
          <div className="flex flex-col gap-3">
            {likedBooks.length > 0 ? (
              likedBooks.map((book) => (
                <div key={book.id} className="rounded-xl bg-card p-4 shadow-sm">
                  <div className="flex items-start gap-3">
                    {book.cover_url ? (
                      <img src={book.cover_url} alt={book.title} className="h-16 w-12 rounded-lg object-cover" />
                    ) : (
                      <div className="h-16 w-12 rounded-lg bg-secondary flex items-center justify-center">
                        <BookOpen className="h-5 w-5 text-muted-foreground" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-display font-bold text-foreground truncate">{book.title}</h3>
                      <p className="text-xs text-muted-foreground mt-0.5 line-clamp-2">{book.description}</p>
                      <div className="flex items-center gap-3 mt-2">
                        {book.genre && <Badge variant="secondary" className="text-[10px]">{book.genre}</Badge>}
                        <span className="text-[10px] text-muted-foreground">❤ {book.likes_count}</span>
                      </div>
                    </div>
                    <button onClick={() => handleUnlike(book.id)} className="text-destructive hover:bg-destructive/10 rounded-full p-1.5">
                      <Heart className="h-4 w-4 fill-current" />
                    </button>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-12 text-center">
                <Heart className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">No liked books yet</p>
                <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/explore")}>Explore Books</Button>
              </div>
            )}
          </div>
        )}

        {/* Comments */}
        {activeTab === "Comments" && (
          <div className="flex flex-col gap-3">
            {userComments.length > 0 ? (
              userComments.map((comment) => (
                <div key={comment.id} className="rounded-xl bg-card p-4 shadow-sm">
                  <div className="flex items-start gap-3">
                    <MessageCircle className="h-4 w-4 text-muted-foreground mt-1 flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <button onClick={() => navigate(`/book/${comment.book_id}`)} className="text-xs font-semibold text-primary hover:underline">
                        {comment.book_title}
                      </button>
                      <p className="text-sm text-foreground mt-1">{comment.content}</p>
                      <span className="text-[10px] text-muted-foreground">{timeAgo(comment.created_at)}</span>
                    </div>
                    <button onClick={() => handleDeleteComment(comment.id, comment.book_id)} className="text-muted-foreground hover:text-destructive rounded-full p-1 hover:bg-destructive/10">
                      <X className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-12 text-center">
                <MessageCircle className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">No comments yet</p>
                <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/explore")}>Explore Books</Button>
              </div>
            )}
          </div>
        )}

        {/* Views / Read History */}
        {activeTab === "Views" && (
          <div className="flex flex-col gap-3">
            {readHistory.length > 0 ? (
              readHistory.map((item) => (
                <div key={item.id} className="rounded-xl bg-card p-4 shadow-sm cursor-pointer" onClick={() => navigate(`/book/${item.book_id}`)}>
                  <div className="flex items-start gap-3">
                    {item.book_cover ? (
                      <img src={item.book_cover} alt={item.book_title} className="h-16 w-12 rounded-lg object-cover" />
                    ) : (
                      <div className="h-16 w-12 rounded-lg bg-secondary flex items-center justify-center">
                        <Eye className="h-5 w-5 text-muted-foreground" />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-display font-bold text-foreground truncate">{item.book_title}</h3>
                      {item.book_genre && <Badge variant="secondary" className="text-[10px] mt-1">{item.book_genre}</Badge>}
                      <p className="text-[10px] text-muted-foreground mt-1">{timeAgo(item.created_at)}</p>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-12 text-center">
                <Eye className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">No reading history yet</p>
                <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/explore")}>Start Reading</Button>
              </div>
            )}
          </div>
        )}

        {/* Followers */}
        {activeTab === "Followers" && (
          <div className="flex flex-col gap-3">
            {followerUsers.length > 0 ? (
              followerUsers.map((follow) => (
                <div key={follow.id} className="rounded-xl bg-card p-4 shadow-sm">
                  <div className="flex items-center gap-3">
                    {follow.profile?.avatar_url ? (
                      <img src={follow.profile.avatar_url} alt={follow.profile.name} className="h-10 w-10 rounded-full object-cover" />
                    ) : (
                      <div className="h-10 w-10 rounded-full bg-secondary flex items-center justify-center">
                        <span className="font-display text-sm font-bold text-muted-foreground">
                          {follow.profile?.name?.charAt(0)?.toUpperCase() || "?"}
                        </span>
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-display font-bold text-sm text-foreground">{follow.profile?.name || "Unknown"}</h3>
                      {follow.profile?.category && <Badge variant="secondary" className="text-[10px] mt-0.5">{follow.profile.category}</Badge>}
                      <p className="text-xs text-muted-foreground mt-0.5 truncate">{follow.profile?.bio || ""}</p>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-12 text-center">
                <Users className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">No followers yet</p>
                <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/explore")}>Share Your Work</Button>
              </div>
            )}
          </div>
        )}

        {activeTab === "Following" && (
          <div className="flex flex-col gap-3">
            {followingUsers.length > 0 ? (
              followingUsers.map((follow) => (
                <div key={follow.id} className="rounded-xl bg-card p-4 shadow-sm">
                  <div className="flex items-center gap-3">
                    {follow.profile?.avatar_url ? (
                      <img src={follow.profile.avatar_url} alt={follow.profile.name} className="h-10 w-10 rounded-full object-cover" />
                    ) : (
                      <div className="h-10 w-10 rounded-full bg-secondary flex items-center justify-center">
                        <span className="font-display text-sm font-bold text-muted-foreground">
                          {follow.profile?.name?.charAt(0)?.toUpperCase() || "?"}
                        </span>
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-display font-bold text-sm text-foreground">{follow.profile?.name || "Unknown"}</h3>
                      {follow.profile?.category && <Badge variant="secondary" className="text-[10px] mt-0.5">{follow.profile.category}</Badge>}
                      <p className="text-xs text-muted-foreground mt-0.5 truncate">{follow.profile?.bio || ""}</p>
                    </div>
                    <Button variant="outline" size="sm" onClick={() => handleUnfollow(follow.id)} className="text-xs">
                      <UserMinus className="h-3.5 w-3.5 mr-1" /> Unfollow
                    </Button>
                  </div>
                </div>
              ))
            ) : (
              <div className="py-12 text-center">
                <Users className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">Not following anyone yet</p>
                <Button variant="outline" size="sm" className="mt-3" onClick={() => navigate("/explore")}>Discover Authors</Button>
              </div>
            )}
          </div>
        )}
        <div className="mt-8 pb-4 text-center">
          <PrivacyPolicyLink />
        </div>
      </div>
    </div>

  );
};

export default Profile;
