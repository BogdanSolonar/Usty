// Три розділи на головній. Тепер кожен має власну сторінку (features/dates, wishlist, dreams),
// а тут — лише те, що потрібно для панелей і шапок: назва, фото, емодзі.
export interface CategoryInfo {
  slug: 'dates' | 'wishlist' | 'dreams'; // частина адреси: /c/dates
  title: string;
  subtitle: string;
  image: string;    // фонове фото (лежить у frontend/public/images)
  focus: string;    // яку частину фото тримати в кадрі при обрізанні (CSS background-position)
  emoji: string;    // показується на плитці без фото
}

export const CATEGORIES: CategoryInfo[] = [
  {
    slug: 'dates',
    title: 'Побачення',
    subtitle: 'Ідеї, куди піти разом',
    image: '/images/dates.jpg',
    focus: '50% 35%',
    emoji: '🌹',
  },
  {
    slug: 'wishlist',
    title: 'Вішліст',
    subtitle: 'Подарунки та бажання',
    image: '/images/wishlist.jpg',
    focus: '50% 50%',
    emoji: '🎁',
  },
  {
    slug: 'dreams',
    title: 'Мрії',
    subtitle: 'Великі плани на майбутнє',
    image: '/images/dreams.jpg',
    focus: '72% 40%',
    emoji: '✨',
  },
];

export function category(slug: CategoryInfo['slug']): CategoryInfo {
  return CATEGORIES.find((c) => c.slug === slug)!;
}
