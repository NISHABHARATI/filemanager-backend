package com.example.demo.filemanager.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.filemanager.repository.FileRepository;
import com.example.demo.filemanager.repository.UserRepository;
import com.example.demo.filemanager.entity.FileData;
import com.example.demo.filemanager.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FileSharingService {

    @Autowired
    private FileRepository fileDataRepository;

    @Autowired
    private UserRepository userRepository;

    // Method to share a file with another user
    public void shareFile(String fileName, Long ownerId, String recipientEmail) throws Exception {
        // Find the file by filename and owner ID
        FileData originalFile = fileDataRepository.findByFileNameAndUserId(fileName, ownerId)
                .orElseThrow(() -> new Exception("File not found"));

        // Find the recipient user by email
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new Exception("Recipient user not found"));

                FileData sharedFile = FileData.builder()
                .userId(recipient.getUserId())                  // Set recipient user ID
                .fileName(originalFile.getFileName())       // Copy filename
                .blobData(originalFile.getBlobData())       // Copy file content
                .isFile(originalFile.getIsFile())           // Set as a file
                .isFolder(originalFile.getIsFolder())       // If it's a folder
                .parentFolderId(originalFile.getParentFolderId()) // Set the same parent folder
                .path(originalFile.getPath())               // Copy the file path
                .createdAt(LocalDateTime.now())             // Set the current time for the new entry
                .build();

        // Save the shared file for the recipient
        fileDataRepository.save(sharedFile);
    }
}

