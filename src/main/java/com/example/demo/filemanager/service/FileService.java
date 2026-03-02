package com.example.demo.filemanager.service;
import com.example.demo.filemanager.dto.FileDataResponseDTO;
import com.example.demo.filemanager.entity.FileData;
import com.example.demo.filemanager.repository.FileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileService {

    @Autowired
     FileRepository fileRepository;


    public List<FileDataResponseDTO> getFilesAndFolders(Long userId, Long parentFolderId) {
        List<FileData> fileDataList = fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
        return fileDataList.stream().map(fileData -> FileDataResponseDTO.builder()
                .userId(fileData.getUserId())
                .parentFolderId(fileData.getParentFolderId())
                .tableId(fileData.getTableId())
                .fileName(fileData.getFileName())
                .isFolder(fileData.getIsFolder())
                .isFile(fileData.getIsFile())
                .path(fileData.getPath())
                .build()).collect(Collectors.toList());
    }

    public FileData getFile(Long userId, String fileName) {
        return fileRepository.findByUserIdAndFileName(userId, fileName);
    }


    public String uploadFile(Long userId, MultipartFile file, Long parentFolderId) throws IOException {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }

        String newFileName = buildUniqueFileName(userId, parentFolderId, originalFileName);

        FileData fileData = new FileData();
        fileData.setUserId(userId);
        fileData.setParentFolderId(parentFolderId);
        fileData.setFileName(newFileName);
        fileData.setIsFolder(false);
        fileData.setIsFile(true);
        fileData.setBlobData(file.getBytes());
        fileData.setCreatedAt(LocalDateTime.now());

        fileData.setPath(buildPath(parentFolderId, newFileName));

        fileRepository.save(fileData);
        fileRepository.flush();

        return fileData.getFileName();
    }

    @Transactional
    public int uploadFolderZip(Long userId, MultipartFile folderZip, Long parentFolderId) throws IOException {
        if (folderZip == null || folderZip.isEmpty()) {
            throw new IllegalArgumentException("ZIP file cannot be empty.");
        }

        Map<String, Long> folderCache = new HashMap<>();
        folderCache.put("", parentFolderId);
        int importedFiles = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(folderZip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String normalizedPath = normalizeZipPath(entry.getName());
                if (normalizedPath.isEmpty() || normalizedPath.startsWith("__MACOSX/")) {
                    zipInputStream.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    ensureFolderPath(userId, parentFolderId, trimTrailingSlash(normalizedPath), folderCache);
                    zipInputStream.closeEntry();
                    continue;
                }

                String parentPath = getParentPath(normalizedPath);
                Long fileParentId = ensureFolderPath(userId, parentFolderId, parentPath, folderCache);
                String fileName = getFileName(normalizedPath);
                if (fileName.isEmpty()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                FileData fileData = new FileData();
                fileData.setUserId(userId);
                fileData.setParentFolderId(fileParentId);
                fileData.setFileName(buildUniqueFileName(userId, fileParentId, fileName));
                fileData.setIsFolder(false);
                fileData.setIsFile(true);
                fileData.setBlobData(readAllBytes(zipInputStream));
                fileData.setCreatedAt(LocalDateTime.now());
                fileData.setPath(buildPath(fileParentId, fileData.getFileName()));
                fileRepository.save(fileData);

                importedFiles++;
                zipInputStream.closeEntry();
            }
        }

        fileRepository.flush();
        return importedFiles;
    }

    private Long ensureFolderPath(Long userId, Long rootParentId, String folderPath, Map<String, Long> folderCache) {
        if (folderPath == null || folderPath.isBlank()) {
            return rootParentId;
        }

        String[] segments = folderPath.split("/");
        StringBuilder currentPathBuilder = new StringBuilder();
        Long currentParentId = rootParentId;

        for (String rawSegment : segments) {
            if (rawSegment == null || rawSegment.isBlank()) {
                continue;
            }

            String segment = rawSegment.trim();
            if (currentPathBuilder.length() > 0) {
                currentPathBuilder.append("/");
            }
            currentPathBuilder.append(segment);
            String currentPath = currentPathBuilder.toString();

            Long cachedFolderId = folderCache.get(currentPath);
            if (cachedFolderId != null) {
                currentParentId = cachedFolderId;
                continue;
            }

            FileData existing = fileRepository.findByUserIdAndParentFolderIdAndFileName(userId, currentParentId, segment);
            if (existing != null && Boolean.TRUE.equals(existing.getIsFolder())) {
                currentParentId = existing.getTableId();
                folderCache.put(currentPath, currentParentId);
                continue;
            }

            FileData folder = new FileData();
            folder.setUserId(userId);
            folder.setParentFolderId(currentParentId);
            folder.setFileName(buildUniqueFolderName(userId, currentParentId, segment));
            folder.setIsFolder(true);
            folder.setIsFile(false);
            folder.setCreatedAt(LocalDateTime.now());
            folder.setPath(buildPath(currentParentId, folder.getFileName()));

            FileData savedFolder = fileRepository.save(folder);
            currentParentId = savedFolder.getTableId();
            folderCache.put(currentPath, currentParentId);
        }

        return currentParentId;
    }

    private String buildUniqueFileName(Long userId, Long parentFolderId, String originalFileName) {
        int lastDot = originalFileName.lastIndexOf('.');
        String baseName = lastDot > 0 ? originalFileName.substring(0, lastDot) : originalFileName;
        String extension = lastDot > 0 ? originalFileName.substring(lastDot) : "";

        List<FileData> existingFiles = fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
        Set<String> existingNames = existingFiles.stream().map(FileData::getFileName).collect(Collectors.toSet());

        String candidate = originalFileName;
        int version = 1;
        while (existingNames.contains(candidate)) {
            candidate = baseName + "_v" + version + extension;
            version++;
        }
        return candidate;
    }

    private String buildUniqueFolderName(Long userId, Long parentFolderId, String folderName) {
        List<FileData> existingFiles = fileRepository.findByUserIdAndParentFolderId(userId, parentFolderId);
        Set<String> existingNames = existingFiles.stream().map(FileData::getFileName).collect(Collectors.toSet());

        String candidate = folderName;
        int version = 1;
        while (existingNames.contains(candidate)) {
            candidate = folderName + "_v" + version;
            version++;
        }
        return candidate;
    }

    private String buildPath(Long parentFolderId, String name) {
        if (parentFolderId == -1) {
            return "/" + name;
        }

        FileData parentFolder = fileRepository.findById(parentFolderId).orElse(null);
        if (parentFolder == null || parentFolder.getPath() == null || parentFolder.getPath().isBlank()) {
            return "/" + name;
        }
        return parentFolder.getPath() + "/" + name;
    }

    private String normalizeZipPath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
        String normalized = rawPath.replace("\\", "/").trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String trimTrailingSlash(String path) {
        String result = path;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String getParentPath(String path) {
        int idx = path.lastIndexOf('/');
        return idx < 0 ? "" : path.substring(0, idx);
    }

    private String getFileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx < 0 ? path : path.substring(idx + 1);
    }

    private byte[] readAllBytes(ZipInputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public FileData save(FileData fileData) {
        fileRepository.save(fileData);
        return fileData;
    }

    public String renameFileOrFolder(Long userId, String oldFileName, String newFileName) {

        FileData fileData = fileRepository.findByUserIdAndFileName(userId, oldFileName);
        if (fileData == null) {
            return "File or folder not found";
        }
        if (oldFileName.equals(newFileName)) return "File Name already exists";
        fileData.setFileName(newFileName);
        fileRepository.save(fileData);
        return newFileName;
    }
    public List<FileData> searchFilesByName(Long userId, String fileName) {
        return fileRepository.findByFileNameContaining(userId, fileName);
    }

}
