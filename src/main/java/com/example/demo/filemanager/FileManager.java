package com.example.demo.filemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileManager {

    public static void main(String[] args) {
        System.out.println("Entering Main");
        SpringApplication.run(FileManager.class, args);
    }

}
