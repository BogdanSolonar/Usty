import { User } from './user.model';

// Мрія (dto/dream/DreamResponse)
export interface Dream {
  id: number;
  title: string;
  description: string | null;
  imageUrl: string | null;
  createdBy: User;
  createdAt: string;
  achieved: boolean;        // мрія здійснилась
  achievedAt: string | null;
}
