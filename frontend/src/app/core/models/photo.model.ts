// Пропозиція фото з Pixabay (dto/PhotoSuggestion на бекенді)
export interface PhotoSuggestion {
  previewUrl: string;  // менша версія для вибору
  imageUrl: string;    // велика версія; при збереженні бекенд скачує її собі
  alt: string;
  photographer: string;
}

// Яке фото користувач обрав у формі. Від цього залежить, що робити при збереженні.
export type PhotoChoice =
  | { kind: 'none' }                       // без фото
  | { kind: 'current'; url: string }       // уже збережене фото (редагування) — лишаємо як є
  | { kind: 'suggestion'; url: string }    // фото з Pixabay — бекенд скопіює його собі
  | { kind: 'file'; file: File };          // файл з пристрою — завантажуємо на бекенд
