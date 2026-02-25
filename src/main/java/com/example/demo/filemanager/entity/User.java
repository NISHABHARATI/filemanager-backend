//package com.example.demo.filemanager.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.util.Objects;
//
//@Data
//@Builder  //instance
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Table(name = "users",schema="TRAINING_NISHA")
//
//public class User{
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long UserId;
//    private String firstName;
//    private String lastName;
//    private String contact;
//    private String email;
//    private String password;
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof User user)) return false;
//        return Objects.equals(UserId, user.UserId);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(UserId);
//    }
//
//
//}

package com.example.demo.filemanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Data
@Builder  // instance builder pattern
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", unique = true, nullable = false)
    private Long userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "contact", nullable = false)
    private String contact;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}

