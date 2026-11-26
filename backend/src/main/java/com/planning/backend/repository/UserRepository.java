package com.planning.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.planning.backend.entity.User;

// Доступ до таблиці users
public interface UserRepository extends JpaRepository<User, Long> {

    // Усі користувачі (вхід за паролем перебирає їх, адмінка групує в пари)
    List<User> findAllByOrderByIdAsc();

    // Двоє з однієї пари: спершу хлопець, потім дівчина (у порядку створення)
    List<User> findByCouple_IdOrderByIdAsc(Long coupleId);

    // Хто з цієї пари — за id (щоб не можна було підставити користувача з чужої пари)
    Optional<User> findByIdAndCouple_Id(Long id, Long coupleId);

    // Telegram-бот: чий це код запрошення з /start <код>
    Optional<User> findByInviteToken(String inviteToken);

    // Остання активність: один UPDATE після кожної успішної зміни даних (security/ActivityTracker)
    @Modifying
    @Transactional
    @Query("update User u set u.lastActivityAt = :at where u.id = :id")
    void touchActivity(Long id, Instant at);
}
