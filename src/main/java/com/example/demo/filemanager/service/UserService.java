package com.example.demo.filemanager.service;
import com.example.demo.filemanager.entity.User;
import com.example.demo.filemanager.model.ConstantValue;
import com.example.demo.filemanager.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    ConstantValue constantValue;
    @Autowired
    UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


//    public User registerUser(User user) {
//        if (userRepository.findByEmail(user.getEmail()).isPresent() ||
//                userRepository.findByEmail(user.getEmail()).isPresent()) {
//            throw new RuntimeException("User already exists with this username or email");
//        }
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
//        return userRepository.save(user);
//    }
public User registerUser(User user) {
    if (userRepository.findByEmail(user.getEmail()).isPresent()) {
        throw new RuntimeException("Email already exists");
    }
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
}



    public String loginUser(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (userOpt.isPresent()) {
            constantValue.UserFound = true;
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                constantValue.Validation = true;
                return "Login successfully";
            } else {
                constantValue.Validation = false;
                return "Invalid password";
            }
        } else {
            constantValue.UserFound = false;
            constantValue.Validation = false;
            return "User not found";
        }
    }

    public User getUserById(Long userId) {
    return userRepository.findById(userId).orElse(null);
    }
    public Long getUserIdByEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            long mid = userOptional.get().getUserId();
            return mid;
        } else {
            throw new RuntimeException("User not found with email: " + email);
        }
    }
    public List<String> getEmailsExcludingCurrentUser(Long currentUserId) {
        return userRepository.findAllEmailsExcludingCurrentUser(currentUserId);
    }


}

