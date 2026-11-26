import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { UserService } from '../../../core/services/user.service';
import { UserAvatar } from '../../../shared/user-avatar/user-avatar';

const MAX_FILE_BYTES = 10 * 1024 * 1024;

// Кружечок з аватаркою та ім'ям у правому верхньому куті головної.
// Клік — меню: змінити фото аватарки або вийти; в адміна — ще "Створити нову пару" і "Керування парами".
@Component({
  selector: 'app-profile-menu',
  imports: [UserAvatar, RouterLink],
  templateUrl: './profile-menu.html',
  styleUrl: './profile-menu.css',
})
export class ProfileMenu {
  private auth = inject(AuthService);
  private userService = inject(UserService);
  private host = inject(ElementRef<HTMLElement>);

  user = this.auth.user;
  isAdmin = this.auth.isAdmin;
  open = signal(false);
  uploading = signal(false);
  error = signal('');

  // Клік будь-де поза меню — закриваємо
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (!this.host.nativeElement.contains(event.target as Node)) this.open.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.open.set(false);
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (file.size > MAX_FILE_BYTES) {
      this.error.set('Файл завеликий (максимум 10 МБ).');
      return;
    }
    this.error.set('');
    this.uploading.set(true);
    this.userService.uploadAvatar(file).subscribe({
      next: () => this.uploading.set(false),
      error: () => {
        this.uploading.set(false);
        this.error.set('Не вдалося завантажити фото.');
      },
    });
  }

  logout() {
    this.auth.logout();
  }
}
