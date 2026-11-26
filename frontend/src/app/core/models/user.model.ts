// Користувач — така сама форма, як dto/user/UserResponse на бекенді
export type PartnerRole = 'HE' | 'SHE';   // хлопець / дівчина в парі

export interface User {
  id: number;
  name: string;            // "Бодя"
  nameGenitive: string;    // "чий?" — "Боді" (для "Вішліст Боді")
  role: PartnerRole;
  color: string;           // колір-маркер
  avatarUrl: string | null;
  admin: boolean;          // лише підказка інтерфейсу; доступ до адмінки перевіряє бекенд
}

// Відповідь на POST /api/auth/login
export interface LoginResponse {
  token: string;
  expiresAt: string;       // ISO-дата, коли токен перестане діяти (через 7 днів)
  user: User;
}
