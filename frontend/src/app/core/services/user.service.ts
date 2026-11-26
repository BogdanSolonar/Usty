import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { User } from '../models/user.model';

// Обоє з моєї пари (шапка, вкладки, фільтри, статистика) і зміна аватарки
@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  // обоє з моєї пари; завантажуються з /api/users і далі беруться звідси
  users = signal<User[]>([]);

  load(): Observable<User[]> {
    return this.http.get<User[]>('/api/users').pipe(tap((users) => this.users.set(users)));
  }

  uploadAvatar(file: File): Observable<User> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<User>('/api/users/me/avatar', form).pipe(
      tap((me) => {
        this.auth.updateUser(me);
        this.users.update((list) => list.map((u) => (u.id === me.id ? me : u)));
      }),
    );
  }
}
