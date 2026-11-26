package com.planning.backend.dto.user;

import com.planning.backend.entity.PartnerRole;
import com.planning.backend.entity.User;

// Користувач у відповідях API (без хешу пароля, Telegram і запрошень).
// admin — лише підказка фронтенду показати пункт "Керування парами"; доступ перевіряє бекенд.
public record UserResponse(Long id, String name, String nameGenitive, PartnerRole role, String color,
        String avatarUrl, boolean admin) {

    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getId(), user.getName(), user.getNameGenitive(), user.getRole(),
                user.getColor(), user.getAvatarUrl(), user.isAdmin());
    }
}
