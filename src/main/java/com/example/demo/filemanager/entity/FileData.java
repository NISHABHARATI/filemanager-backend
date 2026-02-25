package com.example.demo.filemanager.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "files")
@Data
@Builder  //instance
@NoArgsConstructor
@AllArgsConstructor

public class FileData {


    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id", unique = true)
    private Long tableId;

    @Column(name = "parent_folder_id")
    private Long parentFolderId = -1L;

    @Column(name = "is_folder", nullable = false)
    private Boolean isFolder;

    @Column(name = "is_file", nullable = false)
    private Boolean isFile;

    @Column(name = "path")
    private String path;


    @Column(columnDefinition = "bytea")
    private byte[] blobData;

    @Column(name = "file_name", nullable = false)
    private String fileName;



    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

