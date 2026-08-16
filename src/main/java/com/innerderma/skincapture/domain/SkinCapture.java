package com.innerderma.skincapture.domain;

import com.innerderma.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "skin_captures")
public class SkinCapture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "captured_date", nullable = false)
    private LocalDate capturedDate;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(name = "image_path", nullable = false, unique = true, length = 500)
    private String imagePath;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_status", nullable = false, length = 30)
    private SkinCaptureQualityStatus qualityStatus;

    protected SkinCapture() {
    }

    public SkinCapture(
            User user,
            LocalDate capturedDate,
            LocalDateTime capturedAt,
            String imagePath,
            String originalFilename,
            String contentType,
            long fileSize,
            SkinCaptureQualityStatus qualityStatus
    ) {
        this.user = user;
        this.capturedDate = capturedDate;
        this.capturedAt = capturedAt;
        this.imagePath = imagePath;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.qualityStatus = qualityStatus;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getCapturedDate() { return capturedDate; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public String getImagePath() { return imagePath; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public SkinCaptureQualityStatus getQualityStatus() { return qualityStatus; }
}
