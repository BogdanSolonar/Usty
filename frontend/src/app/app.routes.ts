import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';
import { AdminPage } from './features/admin/admin-page/admin-page';
import { Home } from './features/home/home';
import { Login } from './features/login/login';
import { DatesPage } from './features/dates/dates-page/dates-page';
import { DateHistory } from './features/dates/date-history/date-history';
import { WishlistPage } from './features/wishlist/wishlist-page';
import { DreamsPage } from './features/dreams/dreams-page';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [guestGuard] },           // вхід за паролем
  { path: '', component: Home, canActivate: [authGuard] },                  // головна: 3 панелі
  { path: 'c/dates', component: DatesPage, canActivate: [authGuard] },      // побачення
  { path: 'c/dates/history', component: DateHistory, canActivate: [authGuard] }, // реалізовані
  { path: 'c/wishlist', component: WishlistPage, canActivate: [authGuard] },
  { path: 'c/dreams', component: DreamsPage, canActivate: [authGuard] },
  { path: 'admin', component: AdminPage, canActivate: [authGuard, adminGuard] }, // керування парами (лише адмін)
  { path: '**', redirectTo: '' },                                           // будь-яка інша адреса -> на головну
];
