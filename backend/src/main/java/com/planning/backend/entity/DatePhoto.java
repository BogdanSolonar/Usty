package com.planning.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

// Таблиця date_photos: фото з проведеного побачення + автоматична оцінка якості
@Entity
@Table(name = "date_photos")
public class DatePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "date_id", nullable = false)
    private DateIdea date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(name = "photo_url", nullable = false, length = 2000)
    private String photoUrl;

    // підсумковий бал 1..100
    @Column(name = "quality_score", nullable = false)
    private int qualityScore;

    // деталі оцінки у JSON: {"sharpness":82,"lighting":75,"people":100,...}
    @Column(name = "quality_breakdown", columnDefinition = "text")
    private String qualityBreakdown;

    // короткий вердикт: "Чудове світло, чіткий кадр, видно вас обох"
    @Column(name = "quality_verdict", length = 500)
    private String qualityVerdict;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    void onCreate() {
        uploadedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public DateIdea getDate() {
        return date;
    }

    public void setDate(DateIdea date) {
        this.date = date;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getQualityBreakdown() {
        return qualityBreakdown;
    }

    public void setQualityBreakdown(String qualityBreakdown) {
        this.qualityBreakdown = qualityBreakdown;
    }

    public String getQualityVerdict() {
        return qualityVerdict;
    }

    public void setQualityVerdict(String qualityVerdict) {
        this.qualityVerdict = qualityVerdict;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
