import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import { VaultProvider } from "./contexts/VaultContext";
import { ThemeProvider } from "./contexts/ThemeContext";
import { LanguageProvider } from "./contexts/LanguageContext";
import Index from "./pages/Index";
import Explore from "./pages/Explore";
import BookDetail from "./pages/BookDetail";
import Reader from "./pages/Reader";
import Write from "./pages/Write";
import Editor from "./pages/Editor";
import Profile from "./pages/Profile";
import Auth from "./pages/Auth";
import Settings from "./pages/Settings";
import NotFound from "./pages/NotFound";
import ExternalRedirect from "./components/ExternalRedirect";
import { PRIVACY_URL } from "./components/PrivacyPolicyLink";
import BottomNav from "./components/BottomNav";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <ThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <VaultProvider>
          <TooltipProvider>
            <Toaster />
            <Sonner />
            <BrowserRouter>
              <Routes>
                <Route path="/" element={<Index />} />
                <Route path="/explore" element={<Explore />} />
                <Route path="/book/:id" element={<BookDetail />} />
                <Route path="/read/:id" element={<Reader />} />
                <Route path="/write" element={<Write />} />
                <Route path="/editor/:id" element={<Editor />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/auth" element={<Auth />} />
<Route path="/settings" element={<Settings />} />
                <Route path="/privacy" element={<ExternalRedirect to={PRIVACY_URL} />} />
                <Route path="*" element={<NotFound />} />
              </Routes>
              <BottomNav />
            </BrowserRouter>
          </TooltipProvider>
          </VaultProvider>
        </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  </QueryClientProvider>
);

export default App;
