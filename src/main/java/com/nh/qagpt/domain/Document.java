package com.nh.qagpt.domain;

import com.nh.qagpt.domain.enums.ArtifactType;
import com.nh.qagpt.domain.enums.Stage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** 업로드된 산출물 파일 1건. classifier가 artifactType/stage를 채운다. */
@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    private String fileName;
    private String contentType;   // MIME (xlsx/pptx/hwpx)
    private String storagePath;   // 저장 경로

    @Enumerated(EnumType.STRING)
    private ArtifactType artifactType = ArtifactType.UNKNOWN;

    @Enumerated(EnumType.STRING)
    private Stage stage;

    @CreationTimestamp
    private Instant uploadedAt;
}
