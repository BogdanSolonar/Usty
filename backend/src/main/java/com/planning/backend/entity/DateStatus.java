package com.planning.backend.entity;

// Життєвий цикл побачення: ідея -> заплановано (через "Реалізувати") -> реалізовано (в "Історії")
public enum DateStatus {
    IDEA,
    PLANNED,
    COMPLETED
}
