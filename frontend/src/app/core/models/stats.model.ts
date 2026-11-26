import { User } from './user.model';

// Статистика пари (dto/stats/* на бекенді).
// Бали: ідея +10, ініційоване побачення +20, фото +5…+15 залежно від якості.
export interface PlayerStats {
  user: User;
  ideas: number;
  initiated: number;
  photos: number;
  avgPhotoScore: number;     // середня оцінка якості фото 1..100
  ideaPoints: number;
  initiativePoints: number;
  photoPoints: number;
  totalPoints: number;
}

export interface StatsScore {
  userId: number;
  points: number;
  label: string;             // "3 ідеї", "12 фото, якість 74"
}

export interface StatsCategory {
  key: 'ideas' | 'initiative' | 'photos';
  title: string;
  scores: StatsScore[];
  leaderUserId: number | null;   // null = порівну
}

// Тепла підказка — лише своя: бекенд не віддає підказку партнера
export interface Motivation {
  userId: number;
  level: 'START' | 'TIE' | 'LEAD' | 'SMALL' | 'MEDIUM' | 'BIG';
  headline: string;
  action: string | null;     // конкретний крок з цифрами
}

export interface Stats {
  players: PlayerStats[];
  categories: StatsCategory[];
  leaderUserId: number | null;   // хто попереду за сумою; null = порівну
  gap: number;
  myMotivation: Motivation | null;   // підказка для того, хто увійшов
}
