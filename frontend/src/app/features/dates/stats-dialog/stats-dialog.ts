import { Component, OnInit, computed, inject, output, signal } from '@angular/core';
import { Motivation, Stats, StatsCategory } from '../../../core/models/stats.model';
import { User } from '../../../core/models/user.model';
import { StatsService } from '../../../core/services/stats.service';
import { Modal } from '../../../shared/modal/modal';
import { UserAvatar } from '../../../shared/user-avatar/user-avatar';

// Статистика побачень: сумарні бали під аватарками, медалька тому, хто попереду,
// бали за кожну активність і тепла підказка — лише своя (бекенд не надсилає чужу).
@Component({
  selector: 'app-stats-dialog',
  imports: [Modal, UserAvatar],
  templateUrl: './stats-dialog.html',
  styleUrl: './stats-dialog.css',
})
export class StatsDialog implements OnInit {
  private statsService = inject(StatsService);

  closed = output<void>();

  stats = signal<Stats | null>(null);
  error = signal(false);

  readonly icons: Record<StatsCategory['key'], string> = { ideas: '💡', initiative: '🚀', photos: '📸' };
  readonly rules: Record<StatsCategory['key'], string> = {
    ideas: '+10 за кожну ідею',
    initiative: '+20 за кожне заплановане побачення',
    photos: '+5…+15 за фото, залежно від якості',
  };

  players = computed(() => this.stats()?.players ?? []);

  outdatedBackend = signal(false); // бекенд віддав старий формат (не перезапущений після оновлення)

  ngOnInit() {
    this.statsService.get().subscribe({
      next: (stats) => {
        const valid = !!stats && 'myMotivation' in stats && stats.players.every((p) => typeof p.totalPoints === 'number');
        if (valid) this.stats.set(stats);
        else this.outdatedBackend.set(true);
      },
      error: () => this.error.set(true),
    });
  }

  user(id: number): User | undefined {
    return this.players().find((p) => p.user.id === id)?.user;
  }

  // Підказка показується лише під власною аватаркою
  motivation(userId: number): Motivation | null {
    const mine = this.stats()?.myMotivation;
    return mine && mine.userId === userId ? mine : null;
  }

  // "1 бал", "3 бали", "25 балів"
  points(n: number): string {
    const mod10 = n % 10, mod100 = n % 100;
    const word = mod100 >= 11 && mod100 <= 14 ? 'балів' : mod10 === 1 ? 'бал' : mod10 >= 2 && mod10 <= 4 ? 'бали' : 'балів';
    return `${n} ${word}`;
  }
}
