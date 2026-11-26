// Частина тексту: звичайний текст або посилання
export interface TextPart {
  text: string;
  isLink: boolean;
}

// "Дивись https://site.com тут" -> [текст "Дивись ", посилання "https://site.com", текст " тут"]
// Так посилання стають клікабельними без innerHTML — будь-який текст показується як текст.
export function splitLinks(text: string): TextPart[] {
  return text
    .split(/(https?:\/\/[^\s]+)/g)
    .filter((part) => part !== '')
    .map((part) => ({ text: part, isLink: /^https?:\/\//.test(part) }));
}
