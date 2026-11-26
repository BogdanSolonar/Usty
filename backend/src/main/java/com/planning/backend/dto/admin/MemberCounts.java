package com.planning.backend.dto.admin;

// Скільки чого створив один користувач (прості COUNT по user_id)
public record MemberCounts(long dates, long wishes, long dreams, long photos) {
}
