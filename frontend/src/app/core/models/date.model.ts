import { User } from './user.model';

// Побачення: ідея -> заплановано -> реалізовано (dto/date/DateResponse на бекенді)
export type DateStatus = 'IDEA' | 'PLANNED' | 'COMPLETED';

export interface QualityBreakdown {
  sharpness: number;    // чіткість 0..100
  lighting: number;     // освітлення
  people: number;       // люди в кадрі
  faces: number;        // скільки облич знайдено
  composition: number;  // композиція
  location: number;     // фон і кольори
  resolution: number;   // роздільна здатність
}

export interface DatePhoto {
  id: number;
  photoUrl: string;
  uploadedBy: User;
  uploadedAt: string;
  qualityScore: number;         // 1..100
  qualityVerdict: string;       // "Чудове світло, чіткий кадр, видно вас обох"
  qualityBreakdown: QualityBreakdown | null;
}

export interface DateIdea {
  id: number;
  title: string;
  description: string | null;
  coverImageUrl: string | null;
  status: DateStatus;
  createdAt: string;
  createdBy: User;
  plannedDate: string | null;   // "2026-12-05"
  plannedNote: string | null;
  initiatedBy: User | null;
  completedAt: string | null;   // "2026-12-05"
  photos: DatePhoto[];
}
