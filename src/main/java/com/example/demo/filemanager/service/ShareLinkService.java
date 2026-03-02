package com.example.demo.filemanager.service;

import com.example.demo.filemanager.dto.CreateShareLinkRequest;
import com.example.demo.filemanager.dto.PublicShareLinkResponse;
import com.example.demo.filemanager.dto.ShareLinkResponse;
import com.example.demo.filemanager.entity.FileData;
import com.example.demo.filemanager.entity.SharedLink;
import com.example.demo.filemanager.repository.FileRepository;
import com.example.demo.filemanager.repository.SharedLinkRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ShareLinkService {

    @Autowired
    private SharedLinkRepository sharedLinkRepository;

    @Autowired
    private FileRepository fileRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public ShareLinkResponse createShareLink(Long ownerUserId, CreateShareLinkRequest request, String baseUrl) {
        if (request.getItemId() == null) {
            throw new ShareLinkException("itemId is required", HttpStatus.BAD_REQUEST);
        }
        if (request.getMaxDownloads() != null && request.getMaxDownloads() <= 0) {
            throw new ShareLinkException("maxDownloads must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ShareLinkException("expiresAt must be in the future", HttpStatus.BAD_REQUEST);
        }

        FileData item = fileRepository.findById(request.getItemId())
                .orElseThrow(() -> new ShareLinkException("File or folder not found", HttpStatus.NOT_FOUND));

        if (!ownerUserId.equals(item.getUserId())) {
            throw new ShareLinkException("You can only share your own file/folder", HttpStatus.FORBIDDEN);
        }

        SharedLink sharedLink = new SharedLink();
        sharedLink.setToken(UUID.randomUUID().toString().replace("-", ""));
        sharedLink.setOwnerUserId(ownerUserId);
        sharedLink.setItemId(item.getTableId());
        sharedLink.setExpiresAt(request.getExpiresAt());
        sharedLink.setMaxDownloads(request.getMaxDownloads());
        sharedLink.setDownloadCount(0);
        sharedLink.setRevoked(false);
        sharedLink.setCreatedAt(LocalDateTime.now());

        String password = request.getPassword();
        if (password != null && !password.isBlank()) {
            sharedLink.setPasswordHash(passwordEncoder.encode(password));
        }

        SharedLink saved = sharedLinkRepository.save(sharedLink);
        return toOwnerResponse(saved, item, baseUrl);
    }

    public List<ShareLinkResponse> listOwnerLinks(Long ownerUserId, String baseUrl) {
        List<SharedLink> links = sharedLinkRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        List<ShareLinkResponse> response = new ArrayList<>();
        for (SharedLink link : links) {
            FileData item = fileRepository.findById(link.getItemId()).orElse(null);
            response.add(toOwnerResponse(link, item, baseUrl));
        }
        return response;
    }

    @Transactional
    public ShareLinkResponse revokeLink(Long ownerUserId, Long linkId, String baseUrl) {
        SharedLink link = sharedLinkRepository.findByIdAndOwnerUserId(linkId, ownerUserId)
                .orElseThrow(() -> new ShareLinkException("Shared link not found", HttpStatus.NOT_FOUND));

        link.setRevoked(true);
        link.setRevokedAt(LocalDateTime.now());
        SharedLink saved = sharedLinkRepository.save(link);
        FileData item = fileRepository.findById(saved.getItemId()).orElse(null);
        return toOwnerResponse(saved, item, baseUrl);
    }

    public PublicShareLinkResponse getPublicLinkDetails(String token, String password) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new ShareLinkException("Shared link not found", HttpStatus.NOT_FOUND));

        validateActive(link);
        validatePassword(link, password);

        FileData item = fileRepository.findById(link.getItemId())
                .orElseThrow(() -> new ShareLinkException("Shared item not found", HttpStatus.NOT_FOUND));

        PublicShareLinkResponse response = new PublicShareLinkResponse();
        response.setToken(link.getToken());
        response.setItemId(item.getTableId());
        response.setItemName(item.getFileName());
        response.setFolder(Boolean.TRUE.equals(item.getIsFolder()));
        response.setPasswordProtected(link.getPasswordHash() != null);
        response.setMaxDownloads(link.getMaxDownloads());
        response.setDownloadCount(link.getDownloadCount());
        response.setRemainingDownloads(remainingDownloads(link));
        response.setExpiresAt(link.getExpiresAt());
        response.setStatus(resolveStatus(link));
        return response;
    }

    @Transactional
    public ShareDownloadPayload downloadByToken(String token, String password) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new ShareLinkException("Shared link not found", HttpStatus.NOT_FOUND));

        validateActive(link);
        validatePassword(link, password);
        validateDownloadLimit(link);

        FileData item = fileRepository.findById(link.getItemId())
                .orElseThrow(() -> new ShareLinkException("Shared item not found", HttpStatus.NOT_FOUND));

        ShareDownloadPayload payload;
        if (Boolean.TRUE.equals(item.getIsFolder())) {
            payload = buildFolderZipPayload(item);
        } else {
            if (item.getBlobData() == null) {
                throw new ShareLinkException("Shared file content not found", HttpStatus.NOT_FOUND);
            }
            payload = new ShareDownloadPayload(
                    item.getFileName(),
                    "application/octet-stream",
                    item.getBlobData()
            );
        }

        link.setDownloadCount(link.getDownloadCount() + 1);
        sharedLinkRepository.save(link);
        return payload;
    }

    private ShareLinkResponse toOwnerResponse(SharedLink link, FileData item, String baseUrl) {
        ShareLinkResponse response = new ShareLinkResponse();
        response.setId(link.getId());
        response.setToken(link.getToken());
        response.setShareUrl(baseUrl + "/api/public/share/" + link.getToken());
        response.setOwnerUserId(link.getOwnerUserId());
        response.setItemId(link.getItemId());
        response.setItemName(item != null ? item.getFileName() : "Deleted item");
        response.setFolder(item != null && Boolean.TRUE.equals(item.getIsFolder()));
        response.setPasswordProtected(link.getPasswordHash() != null);
        response.setExpiresAt(link.getExpiresAt());
        response.setMaxDownloads(link.getMaxDownloads());
        response.setDownloadCount(link.getDownloadCount());
        response.setStatus(resolveStatus(link));
        response.setRevoked(link.getRevoked());
        response.setCreatedAt(link.getCreatedAt());
        return response;
    }

    private void validateActive(SharedLink link) {
        if (Boolean.TRUE.equals(link.getRevoked())) {
            throw new ShareLinkException("This shared link has been revoked", HttpStatus.FORBIDDEN);
        }
        if (link.getExpiresAt() != null && LocalDateTime.now().isAfter(link.getExpiresAt())) {
            throw new ShareLinkException("This shared link has expired", HttpStatus.GONE);
        }
    }

    private void validatePassword(SharedLink link, String password) {
        if (link.getPasswordHash() == null) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new ShareLinkException("Password is required", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(password, link.getPasswordHash())) {
            throw new ShareLinkException("Invalid password", HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateDownloadLimit(SharedLink link) {
        if (link.getMaxDownloads() == null) {
            return;
        }
        if (link.getDownloadCount() >= link.getMaxDownloads()) {
            throw new ShareLinkException("Download limit reached", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private String resolveStatus(SharedLink link) {
        if (Boolean.TRUE.equals(link.getRevoked())) {
            return "REVOKED";
        }
        if (link.getExpiresAt() != null && LocalDateTime.now().isAfter(link.getExpiresAt())) {
            return "EXPIRED";
        }
        if (link.getMaxDownloads() != null && link.getDownloadCount() >= link.getMaxDownloads()) {
            return "LIMIT_REACHED";
        }
        return "ACTIVE";
    }

    private Integer remainingDownloads(SharedLink link) {
        if (link.getMaxDownloads() == null) {
            return null;
        }
        int remaining = link.getMaxDownloads() - link.getDownloadCount();
        return Math.max(remaining, 0);
    }

    private ShareDownloadPayload buildFolderZipPayload(FileData rootFolder) {
        String zipName = rootFolder.getFileName() + ".zip";
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            String basePath = rootFolder.getFileName() + "/";
            addFolderContentsToZip(rootFolder.getUserId(), rootFolder.getTableId(), basePath, zipOutputStream);
            zipOutputStream.finish();
            return new ShareDownloadPayload(zipName, "application/zip", byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new ShareLinkException("Failed to create folder zip", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void addFolderContentsToZip(Long userId, Long folderId, String currentPath, ZipOutputStream zos) throws IOException {
        List<FileData> children = fileRepository.findByUserIdAndParentFolderId(userId, folderId);
        for (FileData child : children) {
            if (Boolean.TRUE.equals(child.getIsFolder())) {
                String folderPath = currentPath + child.getFileName() + "/";
                zos.putNextEntry(new ZipEntry(folderPath));
                zos.closeEntry();
                addFolderContentsToZip(userId, child.getTableId(), folderPath, zos);
            } else if (Boolean.TRUE.equals(child.getIsFile()) && child.getBlobData() != null) {
                String filePath = currentPath + child.getFileName();
                zos.putNextEntry(new ZipEntry(filePath));
                zos.write(child.getBlobData());
                zos.closeEntry();
            }
        }
    }

    public static class ShareDownloadPayload {
        private final String fileName;
        private final String contentType;
        private final byte[] content;

        public ShareDownloadPayload(String fileName, String contentType, byte[] content) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content;
        }
    }

    public static class ShareLinkException extends RuntimeException {
        private final HttpStatus status;

        public ShareLinkException(String message, HttpStatus status) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
