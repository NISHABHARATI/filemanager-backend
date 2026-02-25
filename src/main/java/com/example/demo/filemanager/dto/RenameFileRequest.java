package com.example.demo.filemanager.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RenameFileRequest {

    private String oldName;
    private String newName;

}
