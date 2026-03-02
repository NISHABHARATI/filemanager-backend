package com.example.demo.filemanager.controller;

import com.example.demo.filemanager.dto.CreateShareLinkRequest;
import com.example.demo.filemanager.dto.PublicShareLinkResponse;
import com.example.demo.filemanager.dto.ShareLinkPasswordRequest;
import com.example.demo.filemanager.dto.ShareLinkResponse;
import com.example.demo.filemanager.service.ShareLinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "https://filemanager-frontend-navy.vercel.app", allowCredentials = "true")
public class ShareLinkController {

    @Autowired
    private ShareLinkService shareLinkService;

    @PostMapping("/api/share-links")
    public ResponseEntity<?> createShareLink(@RequestHeader("userId") Long userId,
                                             @RequestBody CreateShareLinkRequest request,
                                             HttpServletRequest httpServletRequest) {
        try {
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
                    .replacePath(null)
                    .build()
                    .toUriString();
            ShareLinkResponse response = shareLinkService.createShareLink(userId, request, baseUrl);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ShareLinkService.ShareLinkException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create shared link");
        }
    }

    @GetMapping("/api/share-links")
    public ResponseEntity<?> listShareLinks(@RequestHeader("userId") Long userId,
                                            HttpServletRequest httpServletRequest) {
        try {
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
                    .replacePath(null)
                    .build()
                    .toUriString();
            List<ShareLinkResponse> links = shareLinkService.listOwnerLinks(userId, baseUrl);
            return ResponseEntity.ok(links);
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch shared links");
        }
    }

    @PutMapping("/api/share-links/{linkId}/revoke")
    public ResponseEntity<?> revokeShareLink(@RequestHeader("userId") Long userId,
                                             @PathVariable Long linkId,
                                             HttpServletRequest httpServletRequest) {
        try {
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(httpServletRequest)
                    .replacePath(null)
                    .build()
                    .toUriString();
            ShareLinkResponse response = shareLinkService.revokeLink(userId, linkId, baseUrl);
            return ResponseEntity.ok(response);
        } catch (ShareLinkService.ShareLinkException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to revoke shared link");
        }
    }

    @GetMapping("/api/public/share/{token}")
    public ResponseEntity<?> getSharedLink(@PathVariable String token) {
        try {
            PublicShareLinkResponse response = shareLinkService.getPublicLinkDetails(token, null);
            return ResponseEntity.ok(response);
        } catch (ShareLinkService.ShareLinkException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch shared item");
        }
    }

    @PostMapping("/api/public/share/{token}/verify")
    public ResponseEntity<?> verifySharedLinkPassword(@PathVariable String token,
                                                      @RequestBody ShareLinkPasswordRequest request) {
        try {
            String password = request != null ? request.getPassword() : null;
            PublicShareLinkResponse response = shareLinkService.getPublicLinkDetails(token, password);
            return ResponseEntity.ok(response);
        } catch (ShareLinkService.ShareLinkException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify shared link");
        }
    }

    @PostMapping("/api/public/share/{token}/download")
    public ResponseEntity<?> downloadSharedItem(@PathVariable String token,
                                                @RequestBody(required = false) ShareLinkPasswordRequest request) {
        try {
            String password = request != null ? request.getPassword() : null;
            ShareLinkService.ShareDownloadPayload payload = shareLinkService.downloadByToken(token, password);
            ByteArrayResource resource = new ByteArrayResource(payload.getContent());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(payload.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.getFileName() + "\"")
                    .contentLength(payload.getContent().length)
                    .body(resource);
        } catch (ShareLinkService.ShareLinkException e) {
            return error(e.getStatus(), e.getMessage());
        } catch (Exception e) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download shared item");
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
