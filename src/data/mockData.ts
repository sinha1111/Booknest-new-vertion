export interface Book {
  id: string;
  title: string;
  author: string;
  authorAvatar: string;
  coverUrl: string;
  description: string;
  genre: string;
  chapters: Chapter[];
  likes: number;
  comments: number;
  reads: number;
  price: number | null; // null = free
  isPublished: boolean;
  createdAt: string;
}

export interface Chapter {
  id: string;
  title: string;
  content: string;
  order: number;
}

export interface User {
  id: string;
  name: string;
  avatar: string;
  bio: string;
  followers: number;
  following: number;
  booksWritten: number;
}

export const genres = [
  "Fiction", "Romance", "Fantasy", "Sci-Fi", "Mystery",
  "Thriller", "Horror", "Poetry", "Non-Fiction", "Biography",
  "Self-Help", "Adventure"
];

export const currentUser: User = {
  id: "user-1",
  name: "Alex Rivera",
  avatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop",
  bio: "Storyteller | Dream weaver | Coffee enthusiast ☕",
  followers: 1243,
  following: 89,
  booksWritten: 5,
};

const sampleChapterContent = `The morning light crept through the curtains, casting long golden shadows across the wooden floor. She sat at her desk, pen in hand, staring at the blank page before her. The words would come, she told herself. They always did.

Outside, the city was waking up. The distant hum of traffic, the chirping of sparrows on the windowsill, the faint aroma of coffee from the café downstairs — all of it wove together into the familiar symphony of dawn.

She closed her eyes and let her mind wander. Back to the cobblestone streets of her childhood, to the old bookshop on the corner where stories lived and breathed between dusty covers. That was where it all began — her love affair with words.

"Every story starts with a single sentence," her grandmother had told her once, pressing a leather-bound journal into her small hands. "Write that sentence, and the rest will follow."

She opened her eyes, smiled, and began to write.`;

export const mockBooks: Book[] = [
  {
    id: "book-1",
    title: "The Last Lighthouse Keeper",
    author: "Elena Marsh",
    authorAvatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop",
    coverUrl: "",
    description: "A haunting tale of solitude, storms, and the light that guides us home. Set on a remote island off the coast of Maine.",
    genre: "Fiction",
    chapters: [
      { id: "ch-1", title: "The Arrival", content: sampleChapterContent, order: 1 },
      { id: "ch-2", title: "The Storm", content: sampleChapterContent, order: 2 },
      { id: "ch-3", title: "The Light", content: sampleChapterContent, order: 3 },
    ],
    likes: 2847,
    comments: 342,
    reads: 15200,
    price: null,
    isPublished: true,
    createdAt: "2025-12-15",
  },
  {
    id: "book-2",
    title: "Midnight in the Garden of Code",
    author: "James Chen",
    authorAvatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop",
    coverUrl: "",
    description: "When a young programmer discovers a hidden message in an ancient algorithm, she's drawn into a world where code and magic intertwine.",
    genre: "Sci-Fi",
    chapters: [
      { id: "ch-4", title: "The Algorithm", content: sampleChapterContent, order: 1 },
      { id: "ch-5", title: "Debug Mode", content: sampleChapterContent, order: 2 },
    ],
    likes: 1923,
    comments: 198,
    reads: 8900,
    price: 4.99,
    isPublished: true,
    createdAt: "2026-01-05",
  },
  {
    id: "book-3",
    title: "Whispers of the Willow",
    author: "Sarah Kim",
    authorAvatar: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop",
    coverUrl: "",
    description: "A lyrical romance set in rural Japan, where two strangers find love beneath the ancient willow tree that holds centuries of secrets.",
    genre: "Romance",
    chapters: [
      { id: "ch-6", title: "Under the Willow", content: sampleChapterContent, order: 1 },
      { id: "ch-7", title: "Spring Rain", content: sampleChapterContent, order: 2 },
      { id: "ch-8", title: "The Letter", content: sampleChapterContent, order: 3 },
      { id: "ch-9", title: "Forever After", content: sampleChapterContent, order: 4 },
    ],
    likes: 4521,
    comments: 567,
    reads: 23400,
    price: null,
    isPublished: true,
    createdAt: "2025-11-20",
  },
  {
    id: "book-4",
    title: "The Cartographer's Daughter",
    author: "Marcus Webb",
    authorAvatar: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop",
    coverUrl: "",
    description: "An epic adventure across uncharted lands as a young woman follows her father's mysterious maps to discover a hidden civilization.",
    genre: "Adventure",
    chapters: [
      { id: "ch-10", title: "The Map Room", content: sampleChapterContent, order: 1 },
      { id: "ch-11", title: "Beyond the Edge", content: sampleChapterContent, order: 2 },
    ],
    likes: 3102,
    comments: 289,
    reads: 12800,
    price: 6.99,
    isPublished: true,
    createdAt: "2026-01-28",
  },
  {
    id: "book-5",
    title: "Brewing Shadows",
    author: "Priya Nair",
    authorAvatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&h=100&fit=crop",
    coverUrl: "",
    description: "A cozy mystery set in a small-town coffee shop where the barista solves crimes one cup at a time.",
    genre: "Mystery",
    chapters: [
      { id: "ch-12", title: "The First Cup", content: sampleChapterContent, order: 1 },
      { id: "ch-13", title: "Grounds for Suspicion", content: sampleChapterContent, order: 2 },
      { id: "ch-14", title: "The Bitter Truth", content: sampleChapterContent, order: 3 },
    ],
    likes: 1876,
    comments: 234,
    reads: 9100,
    price: 3.99,
    isPublished: true,
    createdAt: "2026-02-01",
  },
  {
    id: "book-6",
    title: "Letters to Myself",
    author: "Alex Rivera",
    authorAvatar: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop",
    coverUrl: "",
    description: "A deeply personal collection of poetry about growth, loss, and finding beauty in the mundane.",
    genre: "Poetry",
    chapters: [
      { id: "ch-15", title: "Dawn", content: sampleChapterContent, order: 1 },
      { id: "ch-16", title: "Noon", content: sampleChapterContent, order: 2 },
      { id: "ch-17", title: "Dusk", content: sampleChapterContent, order: 3 },
    ],
    likes: 987,
    comments: 145,
    reads: 4300,
    price: null,
    isPublished: true,
    createdAt: "2026-01-10",
  },
];
