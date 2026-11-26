# Usty — побачення, вішліст і мрії для пар

- `frontend/` — Angular 22 (http://localhost:4200)
- `backend/` — Spring Boot 4.2 + Spring Security (JWT) + Java 17 + Gradle (http://localhost:8080)
- `docker-compose.yml` — PostgreSQL 17 (localhost:5432, db/user/password: `planning`), за бажанням і бекенд
- `docker-compose.prod.yml`, `deploy/Caddyfile`, `.env.example`, `scripts/` — сервер (VPS): **див. [DEPLOY.md](DEPLOY.md)**

## Перед першим запуском

Усі секрети лежать у `backend/secrets.properties` (файл не потрапляє в git):

```properties
pixabay.api-key=...            # автопідбір фото (без нього все працює, крім пропозицій)
jwt.secret=...                 # ключ підпису токенів входу (base64, 32+ байти)
users.bodya.password=...       # паролі першої пари (Бодя — адмін) — у базу йдуть лише BCrypt-хеші
users.marichka.password=...
telegram.bot-token=...         # бот @UstyBM_bot: видача паролів новим парам і сповіщення про побачення
telegram.bot-username=UstyBM_bot # для посилань https://t.me/UstyBM_bot?start=<код>; зміна бота -> усі прив'язуються заново
telegram.chat-id=...           # запасний чат для пари адміна, поки ніхто з неї не прив'язав бота
```

Перша пара (Бодя-адмін і Марічка) створюється автоматично, коли таблиця `users` порожня.
Усі наступні пари створює адмін у застосунку: аватарка → «Створити нову пару».
База попередньої версії (одна пара без таблиці `couples`) переноситься в модель пар автоматично при першому запуску.

Старі записи з версії 2 (одна таблиця `items`) переносяться в нові таблиці автоматично при першому запуску;
стара таблиця лишається як резервна копія `items_legacy`.

## Пари, адмінка й Telegram-бот

- **Пара** (`couples`) — межа даних: двоє користувачів і все їхнє (`dates`, `date_photos`, `wishlist_items`, `dreams`)
  мають `couple_id`; інші пари цих даних не бачать (бекенд фільтрує кожен запит за парою з токена).
- **Адмінка** (`/admin`) — лише для користувача з `is_admin` (Бодя): створення пар, нові паролі, посилання на бота,
  видалення пари разом з усім контентом. Зверху — короткі лічильники використання; у кожного з пари —
  чи входив, останній вхід (`last_login_at`), остання дія (`last_activity_at`, оновлює `security/ActivityTracker`
  після кожного успішного POST/PUT/DELETE) і скільки чого створив. Бекенд перевіряє права при кожному запиті до `/api/admin/**`.
- **Бот**: при створенні пари кожен отримує посилання `https://t.me/<бот>?start=<код>`. Після «Start» бот прив'язує
  його Telegram і надсилає ЙОГО пароль. Бекенд слухає бота сам (long polling, `service/telegram/TelegramPoller`),
  тож публічна адреса не потрібна. Опитувати бота може лише одна програма: для другого (тестового) запуску
  задайте `TELEGRAM_POLLING_ENABLED=false`.

## Запуск (три окремі термінали, з кореня проєкту)

```powershell
# 1. База даних
docker compose up -d

# 2. Бекенд
cd backend
.\gradlew.bat bootRun

# 3. Фронтенд (перший раз виконайте npm install)
cd frontend
npm start
```

Відкрийте http://localhost:4200 і введіть свій пароль.

### Варіант: бекенд теж у Docker

```powershell
docker compose --profile full up -d --build
```

Фото тоді зберігаються в Docker-томі `uploads-data`, а `secrets.properties` монтується в контейнер.

## Тести бекенду

```powershell
cd backend
.\gradlew.bat test
```

## Де що лежить

**Frontend** (`frontend/src/app/`)
- `core/auth/` — вхід: сервіс сесії (токен у localStorage), interceptor (додає токен до запитів), guard-и сторінок
- `core/models/` — типи даних (користувач, побачення, бажання, мрія, статистика)
- `core/services/` — HTTP-запити до бекенду, по одному сервісу на розділ API
- `shared/` — спільні компоненти: модальне вікно, шапка сторінки, плитка, вікно деталей, форма, вибір фото, календар, аватарка
- `features/login/` — вхід за паролем
- `features/home/` — головна з трьома панелями, шапка пари «я ♥ партнер» і меню профілю (аватарка, вихід, для адміна — пари)
- `features/admin/` — адмінка: список пар, форма нової пари, картка пари з паролями й посиланнями на бота
- `features/dates/` — побачення: сторінка (ідеї + права колонка з календариком і запланованими), картка, «Реалізувати», «Реалізовано» (окреме вікно поверх) + фото, Історія, Статистика
- `features/wishlist/`, `features/dreams/` — вішліст (вкладка кожного з пари) і мрії (фільтр за автором)

**Backend** (`backend/src/main/java/com/planning/backend/`)
- `controller/` — HTTP-ендпоінти (приймають запит, віддають відповідь)
- `service/` — логіка (побачення, фото, вішліст, мрії, статистика, вхід, сповіщення)
- `service/quality/` — автоматична оцінка якості фото (OpenCV + аналіз пікселів)
- `service/stats/` — статистика пари: формула балів (ідея +10, побачення +20, фото +5…+15) і тепла підказка (кожному — лише своя)
- `service/telegram/` — бот: опитування Telegram і видача паролів за кодом із посилання
- `service/AdminService` — пари: створення, нові паролі й посилання, видалення пари з усім вмістом
- `security/` — видача JWT, "хто зараз увійшов" (і з якої пари, чи адмін), захист від перебору паролів,
  генерація паролів і шифрування ще не доставлених паролів (`PasswordVault`)
- `repository/` — доступ до бази даних
- `entity/` — таблиці бази: `couples`, `users`, `dates`, `date_photos`, `wishlist_items`, `dreams`
- `dto/` — форма JSON-запитів і відповідей API
- `client/` — запити до зовнішніх сервісів (Pixabay, перекладач, Telegram)
- `config/` — Spring Security, роздача файлів, створення першої пари й перенесення старих даних (`CoupleSetup`)

**Файли**
- Фото: `backend/uploads/covers/`, `avatars/`, `date-photos/` (у Docker — том `uploads-data`)
- Фонові фото категорій: `frontend/public/images/` (з Pixabay, ліцензія Pixabay Content License)
- Модель пошуку облич: `backend/src/main/resources/opencv/face_detection_yunet_2023mar.onnx` (OpenCV Zoo, MIT)

## Зупинка

- Бекенд і фронтенд: `Ctrl+C` у їхніх терміналах
- База: `docker compose down` (дані зберігаються в томі `db-data`)
- Видалити базу разом із даними: `docker compose down -v`
