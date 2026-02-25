package com.example.demo.filemanager.controller;

import com.example.demo.filemanager.entity.User;
import com.example.demo.filemanager.service.UserService;
import com.example.demo.filemanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;


    @GetMapping("/details")
    public ResponseEntity<User> getUserDetails(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("Session is null, user not logged in.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        User user = userService.getUserById(userId);

        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
    @GetMapping("/emails/exclude-current")
    public ResponseEntity<List<String>> getEmailsExcludingCurrentUser(@RequestHeader("userId") Long userId) {
        try {
            List<String> emails = userService.getEmailsExcludingCurrentUser(userId);
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

