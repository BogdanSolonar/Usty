package com.planning.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.planning.backend.entity.Couple;

// Доступ до таблиці couples
public interface CoupleRepository extends JpaRepository<Couple, Long> {

    List<Couple> findAllByOrderByIdAsc();
}
