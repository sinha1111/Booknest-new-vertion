import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { supabase } from "@/integrations/supabase/client";
import { uploadAvatar, validateAvatar } from "@/lib/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { BookOpen, Mail, Lock, User, Eye, EyeOff, Camera, Tag, FileText } from "lucide-react";
import { toast } from "sonner";
import { genres } from "@/data/mockData";
import appIcon from "@/assets/app-icon.png";
import PrivacyPolicyLink from "@/components/PrivacyPolicyLink";


const Auth = () => {
  const [isLogin, setIsLogin] = useState(true);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [bio, setBio] = useState("");
  const [category, setCategory] = useState("");
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    if (user) {
      navigate("/", { replace: true });
    }
  }, [user, navigate]);

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isLogin) {
        const { error } = await supabase.auth.signInWithPassword({ email, password });
        if (error) throw error;
        toast.success("Welcome back!");
        navigate("/");
      } else {
        const { data: signUpData, error } = await supabase.auth.signUp({
          email,
          password,
          options: {
            // Ei metadata ensure_profile() porhe profile banay (booknest_ prefix = onno app er shathe mishbe na)
            data: { full_name: name.trim(), booknest_bio: bio.trim(), booknest_category: category },
            emailRedirectTo: window.location.origin,
          },
        });
        if (error) throw error;

        if (signUpData.session && signUpData.user) {
          // Email confirmation off thakle shathe shathe session pai
          await supabase.rpc("ensure_profile");
          if (avatarFile) {
            const avatarUrl = await uploadAvatar(signUpData.user.id, avatarFile);
            if (avatarUrl) {
              await supabase.from("profiles").update({ avatar_url: avatarUrl }).eq("user_id", signUpData.user.id);
            }
          }
          toast.success("Account created successfully!");
          navigate("/");
        } else {
          toast.success("Account created! Check your email to confirm, then sign in.");
          setIsLogin(true);
        }
      }
    } catch (error: any) {
      toast.error(error.message || "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-5 bg-background">
      {/* Logo */}
      <div className="flex items-center gap-2 mb-8">
        <img src={appIcon} alt="BookNest app icon" width={44} height={44} className="h-11 w-11 rounded-xl shadow-sm" />
        <span className="font-display text-2xl font-bold text-foreground">BookNest</span>
      </div>


      {/* Card */}
      <div className="w-full max-w-sm rounded-2xl bg-card p-6 shadow-lg border border-border">
        <h2 className="font-display text-xl font-bold text-center mb-1">
          {isLogin ? "Welcome Back" : "Create Account"}
        </h2>
        <p className="text-sm text-muted-foreground text-center mb-6">
          {isLogin ? "Sign in to continue reading & writing" : "Join BookNest and start your journey"}
        </p>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {!isLogin && (
            <>
              {/* Avatar Upload */}
              <div className="flex justify-center">
                <label className="relative cursor-pointer group">
                  <div className="h-20 w-20 rounded-full bg-secondary flex items-center justify-center overflow-hidden ring-2 ring-primary/20">
                    {avatarPreview ? (
                      <img src={avatarPreview} alt="Avatar" className="h-full w-full object-cover" />
                    ) : (
                      <Camera className="h-6 w-6 text-muted-foreground" />
                    )}
                  </div>
                  <div className="absolute inset-0 rounded-full bg-foreground/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                    <Camera className="h-5 w-5 text-primary-foreground" />
                  </div>
                  <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleAvatarChange} className="hidden" />
                </label>
              </div>
              <p className="text-xs text-muted-foreground text-center -mt-2">Tap to add photo</p>

              {/* Name */}
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Full Name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="pl-10"
                  required
                />
              </div>

              {/* Bio */}
              <div className="relative">
                <FileText className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                <Textarea
                  placeholder="Tell us about yourself..."
                  value={bio}
                  onChange={(e) => setBio(e.target.value)}
                  className="pl-10 min-h-[70px] resize-none"
                  maxLength={200}
                />
              </div>

              {/* Category */}
              <div className="relative">
                <Tag className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="flex h-10 w-full rounded-md border border-input bg-background pl-10 pr-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 text-foreground"
                  required
                >
                  <option value="" disabled>Select your category</option>
                  {genres.map((g) => (
                    <option key={g} value={g}>{g}</option>
                  ))}
                </select>
              </div>
            </>
          )}

          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="pl-10"
              required
            />
          </div>

          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type={showPassword ? "text" : "password"}
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="pl-10 pr-10"
              required
              minLength={6}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>

          <Button type="submit" disabled={loading} className="w-full mt-2">
            {loading ? "Please wait..." : isLogin ? "Sign In" : "Sign Up"}
          </Button>
        </form>

        <p className="text-sm text-center text-muted-foreground mt-6">
          {isLogin ? "Don't have an account?" : "Already have an account?"}{" "}
          <button
            onClick={() => setIsLogin(!isLogin)}
            className="text-primary font-medium hover:underline"
          >
            {isLogin ? "Sign Up" : "Sign In"}
          </button>
        </p>
      </div>

      {/* Privacy Policy */}
      <div className="mt-6 mb-8 text-center">
        <PrivacyPolicyLink />
      </div>
    </div>

  );
};

export default Auth;
