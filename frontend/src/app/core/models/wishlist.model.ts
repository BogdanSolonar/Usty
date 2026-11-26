import { User } from './user.model';

// Бажання з вішліста (dto/wishlist/WishlistItemResponse)
export interface WishlistItem {
  id: number;
  title: string;
  description: string | null;
  imageUrl: string | null;
  owner: User;              // чиє це бажання
  createdAt: string;
  fulfilled: boolean;       // подарунок отримано
  fulfilledAt: string | null;
}
