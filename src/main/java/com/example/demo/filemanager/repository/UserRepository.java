package com.example.demo.filemanager.repository;

import com.example.demo.filemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
@Query("SELECT u.email FROM User u WHERE u.userId != :userId")
List<String> findAllEmailsExcludingCurrentUser(@Param("userId") Long userId);

}
