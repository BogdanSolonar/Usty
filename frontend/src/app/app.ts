import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

// Кореневий компонент: лише "рамка", в яку роутер підставляє потрібну сторінку
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class App {}
