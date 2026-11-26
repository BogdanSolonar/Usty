package com.planning.backend.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.planning.backend.dto.user.UserResponse;
import com.planning.backend.entity.User;
import com.planning.backend.repository.UserRepository;
import com.planning.backend.security.CurrentUser;
import com.planning.backend.service.ImageStorageService.Folder;

// Профіль: хто я, обоє з моєї пари (для шапки, вкладок, маркерів), заміна аватарки
@Service
public class UserService {

    private final UserRepository users;
    private final CurrentUser currentUser;
    private final ImageStorageService imageStorage;

    public UserService(UserRepository users, CurrentUser currentUser, ImageStorageService imageStorage) {
        this.users = users;
        this.currentUser = currentUser;
        this.imageStorage = imageStorage;
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(currentUser.get());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> couple() {
        return users.findByCouple_IdOrderByIdAsc(currentUser.coupleId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    // Нове фото аватарки: зберігаємо файл, прив'язуємо до поточного користувача, старе видаляємо
    @Transactional
    public UserResponse updateAvatar(MultipartFile file) throws IOException {
        User me = currentUser.get();
        String oldAvatar = me.getAvatarUrl();
        me.setAvatarUrl(imageStorage.saveUpload(file, Folder.AVATARS));
        imageStorage.deleteIfLocal(oldAvatar);
        return UserResponse.from(me);
    }
}
