package io.owenrbee.filesapi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import io.owenrbee.filesapi.model.FileVO;
import io.owenrbee.filesapi.service.FileService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
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

    @GetMapping("/list")
    public List<FileVO> listFiles() {
        return fileService.getFiles(pwd);
    }

    @GetMapping("/list/{directory}/**")
    public List<FileVO> listFiles(@PathVariable String directory, HttpServletRequest request) {
        final String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                .toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String subfolder;
        if (null != arguments && !arguments.isEmpty()) {
            subfolder = directory + "/" + arguments;
        } else {
            subfolder = directory;
        }

        return fileService.getFiles(pwd + "/" + subfolder);
    }

}