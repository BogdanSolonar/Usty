# Розгортання Usty на сервері (VPS)

На сервері працюють 3 контейнери (`docker-compose.prod.yml`):

| Контейнер | Що робить | Де дані |
|---|---|---|
| `web` (Caddy) | віддає сайт, проксує `/api` і `/uploads` на бекенд, **сам отримує HTTPS-сертифікат** | сертифікати — том `caddy-data` |
| `backend` (Spring Boot) | API, Telegram-бот `@UstyBM_bot` | фото — папка `data/uploads` на сервері |
| `db` (PostgreSQL 17) | база | том `db-data` |

Назовні відкриті лише порти 80 і 443. База й бекенд з інтернету недоступні.
Фронтенд ходить на `/api` тієї ж адреси, тож CORS не потрібен.

Секрети — лише у файлі `.env` на сервері (шаблон — `.env.example`). У git вони не потрапляють.

---

## 0. Спершу — новий токен бота

Токен `@UstyBM_bot` світився в чаті. Перед запуском на сервері:
`@BotFather` → `/revoke` → `@UstyBM_bot` → скопіюйте **новий** токен (він піде в `.env` на сервері).

---

## 1. GitHub (з вашого комп'ютера)

1. Створіть на github.com **приватний** репозиторій, напр. `usty` (без README, без .gitignore).
2. У корені проєкту (`Planning\`):

   ```powershell
   git init -b main
   git add .
   git status            # перевірте: НЕМАЄ secrets.properties, .env, node_modules, dist, build, uploads
   git commit -m "Usty: перша версія"
   git remote add origin https://github.com/<ваш-логін>/usty.git
   git push -u origin main
   ```

`.gitignore` у корені вже відсікає секрети (`secrets.properties`, `.env`, ключі), збірки
(`node_modules`, `dist`, `build`, `.gradle`, `.angular`), фото (`backend/uploads`, `data/`) і бекапи (`*.dump`, `backups/`).

---

## 2. Який сервер брати

| | Мінімум | Комфортно |
|---|---|---|
| CPU | 2 vCPU | 2 vCPU |
| RAM | 2 ГБ + 2 ГБ swap | 4 ГБ |
| Диск | 25 ГБ SSD | 40 ГБ SSD (фото ростуть) |
| ОС | **Ubuntu 24.04 LTS** | |
| Мережа | публічна IPv4-адреса | |

Під час роботи бекенду треба ~0,8 ГБ, базі й Caddy — ~0,2 ГБ. Найбільше пам'яті з'їдає **збірка**
(Gradle + Angular), тому на 2 ГБ обов'язково увімкніть swap (крок 3.2).

**Домен** (напр. `usty.example.com`) дуже бажаний: з ним сайт працює по **HTTPS**, і паролі не летять відкритим текстом.
Без домену сайт працюватиме за `http://<IP>` — годиться лише для першої перевірки.

---

## 3. Підготовка сервера (один раз)

Підключіться: `ssh root@<IP сервера>`

### 3.1 Docker і фаєрвол

```bash
apt update && apt upgrade -y
curl -fsSL https://get.docker.com | sh
ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw --force enable
```

### 3.2 Swap (якщо RAM ≤ 2 ГБ)

```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

### 3.3 Домен

У панелі реєстратора домену додайте **A-запис**: `usty.example.com` → `<IP сервера>`.
Перевірка: `ping usty.example.com` показує IP сервера.

### 3.4 Код

```bash
cd ~ && git clone https://github.com/<ваш-логін>/usty.git && cd usty
```

Репозиторій приватний — GitHub попросить логін і **Personal access token**
(github.com → Settings → Developer settings → Personal access tokens → Fine-grained, доступ лише до `usty`, право *Contents: Read*).

### 3.5 Секрети

```bash
cp .env.example .env
openssl rand -base64 48      # -> JWT_SECRET
openssl rand -base64 24      # -> POSTGRES_PASSWORD
nano .env
chmod 600 .env
```

Заповніть:

- `SITE_ADDRESS` — ваш домен (`usty.example.com`), або `:80`, поки домену немає;
- `POSTGRES_PASSWORD`, `JWT_SECRET` — згенеровані вище;
- `TELEGRAM_BOT_TOKEN` — новий токен з кроку 0; `TELEGRAM_BOT_USERNAME=UstyBM_bot`;
- `PIXABAY_API_KEY` — з вашого локального `backend/secrets.properties`;
- `USERS_BODYA_PASSWORD`, `USERS_MARICHKA_PASSWORD` — **лише якщо стартуєте з порожньою базою** (див. крок 4).

Якщо `POSTGRES_PASSWORD` або `JWT_SECRET` порожні, Docker відмовиться запускатися й підкаже, що саме заповнити.

---

## 4. Запуск і дані

```bash
docker compose -f docker-compose.prod.yml up -d --build     # перша збірка — 5–10 хв
docker compose -f docker-compose.prod.yml logs -f backend   # чекаємо "Started BackendApplication", Ctrl+C
```

Далі — **один** із варіантів.

### Варіант А: почати з чистої бази

При першому старті на порожній базі бекенд сам створює першу пару:
**Бодя (адмін)** і **Марічка** з паролями `USERS_BODYA_PASSWORD` / `USERS_MARICHKA_PASSWORD` з `.env`
(у базу йдуть лише BCrypt-хеші). У лозі буде `Created the first couple: Bodya (admin) and Marichka`.
Якщо паролі не задані або однакові — бекенд не запуститься й напише чому.
Після першого запуску ці два рядки в `.env` можна стерти: при наявних користувачах вони не використовуються.

### Варіант Б: перенести все з локального комп'ютера (рекомендовано)

Переноситься все: пари, ваші паролі (як хеші), побачення, бажання, мрії, фото, статистика.
Пароль адміна лишається вашим теперішнім.

1. **На комп'ютері** (локальна база має бути запущена — `docker compose up -d`):
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\export-local.ps1
   scp -r usty-export root@<IP сервера>:~/usty/
   ```
   З'явиться папка `usty-export\` з `db.dump` (база) і `uploads.tar.gz` (усі фото).
2. **На сервері:**
   ```bash
   cd ~/usty && bash scripts/restore.sh usty-export    # спитає підтвердження: введіть yes
   rm -rf usty-export
   ```
   Скрипт замінює базу на сервері вашою, розпаковує фото в `data/uploads` і перезапускає бекенд.
   Схему під поточну версію бекенд при старті доводить сам.
3. Видаліть `usty-export\` і на комп'ютері — там ваші дані.

Цей сценарій перевірено: після відновлення кількість пар, користувачів, побачень, бажань, мрій і фото збіглася
з локальною, усі 34 фото відкривалися за своїми адресами, ваш пароль адміна підійшов.

### Важливо після переїзду: бот лише в одному місці

Опитувати `@UstyBM_bot` може лише **одна** програма. Інакше Telegram відповідає `409 Conflict`,
а повідомлення боту губляться. Тому в **локальному** `backend/secrets.properties` додайте:

```properties
telegram.polling-enabled=false
```

Ще краще — для локальної розробки зробити в `@BotFather` окремого тестового бота й вписати локально його токен.

---

## 5. Перевірка

1. Відкрийте `https://usty.example.com`. Замок у браузері = HTTPS працює
   (сертифікат Caddy отримує сам за ~1 хв після першого запиту).
2. Увійдіть паролем Боді → аватарка → «Керування парами»: видно статистику й пари.
3. Додайте фото до будь-чого: воно має відкриватися й після перезапуску
   (`docker compose -f docker-compose.prod.yml restart`).
4. **Бот.** У лозі:
   `docker compose -f docker-compose.prod.yml logs backend | grep -i telegram` → `Telegram bot polling started`.
   В адмінці біля себе натисніть «🔗 Посилання для Telegram» → відкрийте → **Start**.
   Бот відповість «Telegram прив'язано ✅», в адмінці з'явиться «✅ Telegram прив'язано».
   Так само надішліть посилання Марічці.
5. Заплануйте побачення («Реалізувати») → обом прив'язаним прийде сповіщення від `@UstyBM_bot`.

---

## 6. Як працювати далі

**Оновлення.** Змінили код локально → `git push` → на сервері:

```bash
cd ~/usty && git pull && docker compose -f docker-compose.prod.yml up -d --build
```

База, фото й сертифікати при цьому лишаються.

**Бекап** (база + фото → `backups/<дата>/`, зберігаються останні 14):

```bash
bash scripts/backup.sh
crontab -e     # щоденно о 4:00:
0 4 * * * cd /root/usty && bash scripts/backup.sh >> backups/backup.log 2>&1
```

Час від часу забирайте бекап на свій комп'ютер:
`scp -r root@<IP>:~/usty/backups/<дата> .`
Відновлення з бекапу: `bash scripts/restore.sh backups/<дата>`.

**Корисне:**

```bash
docker compose -f docker-compose.prod.yml ps                 # що запущено
docker compose -f docker-compose.prod.yml logs -f backend    # логи бекенду
docker compose -f docker-compose.prod.yml exec db psql -U planning planning   # консоль бази
```

**Не робіть** `docker compose ... down -v`: прапорець `-v` видаляє базу.
