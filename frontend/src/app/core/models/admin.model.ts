import { PartnerRole } from './user.model';

// Адмінка (dto/admin/* на бекенді)

// Користувач пари очима адміна
export interface Member {
  id: number;
  name: string;
  nameGenitive: string;
  role: PartnerRole;
  color: string;
  avatarUrl: string | null;
  admin: boolean;
  telegramLinked: boolean;          // натиснув /start у боті
  inviteLink: string | null;        // t.me/<бот>?start=<код>; null — уже використано або бот не налаштований
  pendingPassword: string | null;   // ще не доставлений пароль; null — доставлено / людина вже входила
  createdAt: string | null;
  everLoggedIn: boolean;            // входив хоча б раз
  lastLoginAt: string | null;       // останній вхід
  lastActivityAt: string | null;    // остання дія: вхід або будь-яка зміна даних
  created: MemberCounts;            // скільки чого створив саме цей користувач
}

export interface MemberCounts {
  dates: number;
  wishes: number;
  dreams: number;
  photos: number;
}

// Короткі лічильники по всій системі (блок над списком пар)
export interface AdminOverview {
  couples: number;
  activeCouples: number;   // обоє вже входили
  users: number;
  loggedInUsers: number;
  telegramUsers: number;
  dates: number;
  wishes: number;
  dreams: number;
  photos: number;
}

export interface Couple {
  id: number;
  createdAt: string;
  members: Member[];
  deletable: boolean;      // пару адміна видалити не можна
  botConfigured: boolean;  // false — посилань немає, паролі треба передати вручну
}

// Форма одного з пари; порожні поля бекенд заповнить сам (колір за роллю, пароль згенерує)
export interface PartnerForm {
  name: string;
  nameGenitive: string;
  color: string;
  password: string;
}

export interface CreateCoupleRequest {
  he: PartnerForm;
  she: PartnerForm;
}
