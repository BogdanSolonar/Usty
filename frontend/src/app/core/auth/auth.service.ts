import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginResponse, User } from '../models/user.model';

const STORAGE_KEY = 'usty.session';

// Перехід зі старої назви ("our life"): переносимо збережений вхід, щоб не входити знову
const OLD_STORAGE_KEY = 'our-life.session';
const oldSession = localStorage.getItem(OLD_STORAGE_KEY);
if (oldSession && !localStorage.getItem(STORAGE_KEY)) localStorage.setItem(STORAGE_KEY, oldSession);
localStorage.removeItem(OLD_STORAGE_KEY);

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

// Вхід і поточний користувач.
// Токен (дійсний 7 днів) зберігається в localStorage, тож після перезавантаження
// сторінки чи браузера входити знову не треба, поки він не сплив.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private session = signal<Session | null>(readSession());

  user = computed(() => this.session()?.user ?? null);
  token = computed(() => this.session()?.token ?? null);
  isLoggedIn = computed(() => this.session() !== null);
  isAdmin = computed(() => this.user()?.admin === true); // лише для показу пунктів меню — доступ перевіряє бекенд

  // Лише пароль: бекенд сам визначає, хто це і з якої пари
  login(password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', { password }).pipe(
      tap((res) => this.save({ token: res.token, expiresAt: res.expiresAt, user: res.user })),
    );
  }

  logout() {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    this.router.navigate(['/login']);
  }

  // Свіжі дані про себе з сервера (аватарка, права адміна могли змінитись з іншого пристрою)
  refresh(): Observable<User> {
    return this.http.get<User>('/api/auth/me').pipe(tap((user) => this.updateUser(user)));
  }

  // Після зміни аватарки оновлюємо збереженого користувача
  updateUser(user: User) {
    const current = this.session();
    if (current) this.save({ ...current, user });
  }

  private save(session: Session) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
  }
}

// Читає збережений вхід; прострочений токен одразу відкидаємо
function readSession(): Session | null {
  try {
    const session = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as Session | null;
    if (session && new Date(session.expiresAt).getTime() > Date.now()) return session;
  } catch {
    // пошкоджені дані — просто вважаємо, що входу немає
  }
  localStorage.removeItem(STORAGE_KEY);
  return null;
}
