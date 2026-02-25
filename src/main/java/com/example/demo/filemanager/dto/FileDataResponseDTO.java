package com.example.demo.filemanager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileDataResponseDTO {
    private Long userId;
    private Long parentFolderId;
    private Long tableId;
    private String fileName;
    private Boolean isFolder;
    private Boolean isFile;
    private String path;

}

