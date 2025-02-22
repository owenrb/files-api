package io.owenrbee.filesapi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.owenrbee.filesapi.model.FileVO;
import io.owenrbee.filesapi.service.FileService;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${files.root.folder}")
    private String pwd;

    private FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/pwd")
    public String getPwd() {
        return pwd;
    }

    @GetMapping
    public List<FileVO> getFiles() {
        return fileService.getFiles(pwd);
    }

}