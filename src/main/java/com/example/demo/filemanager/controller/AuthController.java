package com.example.demo.filemanager.controller;
import com.example.demo.filemanager.entity.User;
import com.example.demo.filemanager.model.ConstantValue;
import com.example.demo.filemanager.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    ConstantValue constantValue;
    @Autowired
    UserService userService;

//    @PostMapping("/signup")
//    public ResponseEntity<User> signup(@RequestBody User user) {
//        User registeredUser = userService.registerUser(user);
//        return new ResponseEntity<>(registeredUser, HttpStatus.OK);
//    }
@PostMapping("/signup")
public ResponseEntity<?> signup(@RequestBody User user) {
    try {
        // Attempt to register the user
        User registeredUser = userService.registerUser(user);
        // If successful, return a 201 CREATED status
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    } catch (RuntimeException e) {
        // If user already exists, return 409 Conflict
        return new ResponseEntity<>("Email already exists", HttpStatus.CONFLICT);
    }
}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> user, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        String email = user.get("email").toString();
        String password = user.get("password").toString();
        String loggedInUser = userService.loginUser(email, password);
        Long userId = userService.getUserIdByEmail(email);

        HttpSession session = request.getSession(true);

        if (Boolean.TRUE.equals(constantValue.Validation)) {
            session.setAttribute("userId", userId);

            User userDetails = userService.getUserById(userId);

            response.put("status", "success");
            response.put("token", loggedInUser);
            response.put("user", userDetails);

            System.out.println("Successful login!!");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("status", "error");
            response.put("message", loggedInUser); // Error message
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    }





