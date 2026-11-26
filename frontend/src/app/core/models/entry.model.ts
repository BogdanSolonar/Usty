// Спільне для побачень, бажань і мрій: назва, опис, фото.
// Це і те, що надсилаємо при створенні/редагуванні (dto/EntryRequest на бекенді),
// і початкові значення форми редагування. Автора не передаємо — бекенд бере його з токена.
export interface EntryRequest {
  title: string;
  description: string | null;
  imageUrl: string | null;
}
