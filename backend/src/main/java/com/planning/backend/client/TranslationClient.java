package com.planning.backend.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Переклад українська -> англійська через безкоштовний сервіс MyMemory (без ключа).
// Потрібен, бо Pixabay не розуміє українських запитів.
@Component
public class TranslationClient {

    record Response(Data responseData) {
    }

    record Data(String translatedText) {
    }

    private final RestClient http = RestClient.create();

    // Повертає переклад або сам текст, якщо перекладати нічого (немає кирилиці)
    // чи сервіс недоступний
    public String ukrainianToEnglish(String text) {
        if (!text.matches(".*\\p{IsCyrillic}.*")) {
            return text;
        }
        try {
            Response response = http.get()
                    .uri("https://api.mymemory.translated.net/get?q={q}&langpair=uk|en", text)
                    .retrieve()
                    .body(Response.class);
            if (response != null && response.responseData() != null
                    && response.responseData().translatedText() != null) {
                return response.responseData().translatedText();
            }
        } catch (RestClientException e) {
            // перекладач недоступний — спробуємо шукати як є
        }
        return text;
    }
}
