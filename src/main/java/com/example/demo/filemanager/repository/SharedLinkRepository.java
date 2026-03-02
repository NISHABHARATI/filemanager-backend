package com.example.demo.filemanager.repository;

import com.example.demo.filemanager.entity.SharedLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedLinkRepository extends JpaRepository<SharedLink, Long> {
    Optional<SharedLink> findByToken(String token);
    List<SharedLink> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
    Optional<SharedLink> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}
