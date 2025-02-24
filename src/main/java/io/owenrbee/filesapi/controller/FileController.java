package io.owenrbee.filesapi.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import io.owenrbee.filesapi.model.FileVO;
import io.owenrbee.filesapi.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${files.root.folder}")
    private String pwd;

    private FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "Get present working directory", tags = "current dir")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = @ExampleObject("/User/username/data")))
    @GetMapping(value = "/pwd", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getPwd() {

        return pwd;
    }

    @Operation(summary = "Get list of files and directories from working directory", tags = "dir ops")
    @GetMapping("/list")
    public List<FileVO> listFiles(
            // optional query param - filter for folder only
            @RequestParam(required = false) Boolean folderOnly,
            // optional query param - filter for binary files only
            @RequestParam(required = false) Boolean binaryOnly) {

        return fileService.getFiles(pwd).stream()
                // process optional filtering
                .filter(x -> folderOnly == null || x.isFolder() == folderOnly)
                .filter(x -> binaryOnly == null || x.isBinary() == binaryOnly)
                .collect(Collectors.toList());

    }

    @Operation(summary = "Get list of files and directories from a sub-directory", tags = "dir ops", description = "`Note:` Unnecessary '__/**__' may appear at the end of the path when testing with Swagger UI. Test this resource URL directly using your browser")
    @GetMapping("/list/{directory}/**")
    public List<FileVO> listFiles(@PathVariable String directory,
            // optional query param - filter for folder only
            @RequestParam(required = false) Boolean folderOnly,
            // optional query param - filter for binary files only
            @RequestParam(required = false) Boolean binaryOnly,
            HttpServletRequest request) {

        // some trick to get sub-folder/s path string
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

        // swagger-ui band-aid solution
        if (subfolder.endsWith("/**")) {
            subfolder = subfolder.replace("/**", "");
        }

        return fileService.getFiles(pwd + "/" + subfolder.replaceAll("%20", " "))
                .stream()
                // process optional filtering
                .filter(x -> folderOnly == null || x.isFolder() == folderOnly)
                .filter(x -> binaryOnly == null || x.isBinary() == binaryOnly)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Read text file content", tags = "file ops", description = "`Note:` Unnecessary '__/**__' may appear at the end of the path when testing with Swagger UI. Test this resource URL directly using your browser.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = @ExampleObject("The quick brown fox jumps over the lazy dog."))),
            @ApiResponse(responseCode = "204", description = "File is binary", content = @Content(examples = @ExampleObject(""))),
            @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(""))) })
    @GetMapping(value = "/cat/{filename}/**", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getTextFileContent(@PathVariable String filename, HttpServletRequest request,
            HttpServletResponse response) {

        // some trick to get sub-folders and file path string
        final String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                .toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String filepath;
        if (null != arguments && !arguments.isEmpty()) {
            filepath = filename + "/" + arguments;
        } else {
            filepath = filename;
        }

        // swagger-ui band-aid solution
        if (filepath.endsWith("/**")) {
            filepath = filepath.replace("/**", "");
        }

        String fullpath = pwd + "/" + filepath.replaceAll("%20", " ");
        String content = fileService.readTextFile(fullpath);

        if (content == null) {

            if (fileService.isBinary(fullpath)) {
                response.setStatus(HttpStatus.NO_CONTENT.value());
                return "";
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }

        return content;
    }

    @Operation(summary = "Update text file content", tags = "file ops", description = "`Note:` Unnecessary '__/**__' may appear at the end of the path when testing with Swagger UI. Test this resource URL directly using Postman or any other preferred REST client app.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(examples = @ExampleObject("The quick brown fox jumps over the lazy dog."))),
            @ApiResponse(responseCode = "304", description = "Not Modified: File is binary", content = @Content(examples = @ExampleObject(""))),
            @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(""))) })
    @PutMapping(value = "/cat/{filename}/**", produces = MediaType.TEXT_PLAIN_VALUE, consumes = MediaType.TEXT_PLAIN_VALUE)
    public String updateTextFileContent(@PathVariable String filename, @RequestBody String body,
            HttpServletRequest request) {

        final String path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                .toString();

        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);

        String filepath;
        if (null != arguments && !arguments.isEmpty()) {
            filepath = filename + "/" + arguments;
        } else {
            filepath = filename;
        }

        String fullpath = pwd + "/" + filepath.replaceAll("%20", " ");

        fileService.saveTextfile(fullpath, body);

        String content = fileService.readTextFile(fullpath);

        if (content == null) {
            if (fileService.isBinary(fullpath)) {
                throw new ResponseStatusException(HttpStatus.NOT_MODIFIED);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }

        return content;
    }

    @Operation(summary = "Clear all cache", tags = "misc")
    @PutMapping("/clear")
    public void clearCache() {

        fileService.clearCache();

    }

}