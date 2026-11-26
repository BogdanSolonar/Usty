#!/usr/bin/env bash
# Створює .env на сервері з мінімумом ручного введення (зручно з веб-консолі, де не працює вставка):
#   bash scripts/init-env.sh
# Сам генерує пароль бази й ключ JWT, адресу сайту бере з IP сервера (<IP-з-дефісами>.sslip.io -> HTTPS без домену).
# Питає лише токен бота, ключ Pixabay і (для порожньої бази) паролі першої пари.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  echo ".env уже існує — нічого не змінюю. Редагувати: nano .env"
  exit 1
fi

# публічна адреса — та, з якої сервер виходить в інтернет (не внутрішня адреса Docker)
IP=$(ip -4 route get 1.1.1.1 | awk '{for (i = 1; i < NF; i++) if ($i == "src") print $(i + 1)}')
SITE="${IP//./-}.sslip.io"

read -r -p "Токен бота @UstyBM_bot (Enter — поки без бота): " BOT
read -r -p "Ключ Pixabay (Enter — без автопідбору фото): " PIXABAY
read -r -p "Переносити базу з комп'ютера? (y — так, Enter — почати з чистої бази): " MIGRATE

BODYA=""
MARICHKA=""
if [ "$MIGRATE" != "y" ]; then
  read -r -p "Пароль Боді (адмін): " BODYA
  read -r -p "Пароль Марічки (інший!): " MARICHKA
  if [ -z "$BODYA" ] || [ -z "$MARICHKA" ] || [ "$BODYA" = "$MARICHKA" ]; then
    echo "Паролі мають бути непорожні й різні. Запустіть скрипт ще раз."
    exit 1
  fi
fi

umask 077 # .env читає лише root
cat > .env <<EOF
SITE_ADDRESS=$SITE
POSTGRES_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
USERS_BODYA_PASSWORD='$BODYA'
USERS_MARICHKA_PASSWORD='$MARICHKA'
TELEGRAM_BOT_TOKEN=$BOT
TELEGRAM_BOT_USERNAME=UstyBM_bot
TELEGRAM_CHAT_ID=
PIXABAY_API_KEY=$PIXABAY
EOF

echo
echo "Готово: .env створено. Адреса сайту: https://$SITE"
echo "Змінити щось пізніше: nano .env"
