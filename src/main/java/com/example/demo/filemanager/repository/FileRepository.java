package com.example.demo.filemanager.repository;
import com.example.demo.filemanager.entity.FileData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

    @Repository
    public interface FileRepository extends JpaRepository<FileData, Long> {
    List<FileData> findByUserIdAndParentFolderId(Long userId, Long parentFolderId);
    FileData findByUserIdAndFileName(Long userId, String fileName);
    List<FileData> findByUserId(Long userId);
    FileData findByUserIdAndParentFolderIdAndFileName(Long userId, Long parentFolderId, String fileName);
    @Query("SELECT f FROM FileData f WHERE f.userId = :userId AND f.fileName LIKE %:searchTerm%")
    List<FileData> findByFileNameContaining(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);
    Optional<FileData> findByFileNameAndUserId(String fileName, Long userId);
    }



