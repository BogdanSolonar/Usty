import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CATEGORIES } from '../../core/models/category.model';

// Вхід без імені — лише пароль. Хто саме увійшов (і з якої пари), визначає бекенд.
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  categories = CATEGORIES; // фото для розмитого фону, як на головній
  password = '';
  showPassword = signal(false);
  loading = signal(false);
  error = signal('');

  submit() {
    if (!this.password || this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.auth.login(this.password).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.loading.set(false);
        this.password = '';
        this.error.set(
          err.status === 401
            ? 'Невірний пароль 💔'
            : err.status === 429
              ? 'Забагато спроб. Спробуйте за 10 хвилин.'
              : 'Сервер недоступний. Перевірте, що бекенд запущено.',
        );
      },
    });
  }
}
