import { User } from './user.model';

// "Вішліст Боді", "Мрії Марічки" — форма імені "чий?" (задається при створенні пари)
export function whose(user: User): string {
  return user.nameGenitive || user.name;
}

// "Бодя запланував" / "Марічка запланувала"
export function planned(user: User): string {
  return user.role === 'SHE' ? 'запланувала' : 'запланував';
}
