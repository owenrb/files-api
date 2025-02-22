package io.owenrbee.filesapi.service;

import org.apache.tomcat.util.buf.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import io.owenrbee.filesapi.model.FileVO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Cacheable("files")
    public List<FileVO> getFiles(String directory) {

        logger.info("reading files from: " + directory);

        List<FileVO> filesAndDirectories = new ArrayList<>();

        // Create a File object representing the specified directory
        File dir = new File(directory);

        if (dir.exists() && dir.isDirectory()) {

            for (File file : dir.listFiles()) {

                FileVO vo = new FileVO();
                vo.setName(file.getName());
                vo.setParentDir(directory);

                if (file.isDirectory()) {
                    vo.setFolder(true);
                } else {

                    try {
                        boolean isBinary = isBinary(file);
                        vo.setBinary(isBinary);
                    } catch (IOException e) {
                        logger.error("File reading error in " + directory, e);
                        continue;
                    }

                }

                filesAndDirectories.add(vo);

            }

        } else {
            logger.error("Invalid directory path: {}", directory);
            return null;
        }

        return filesAndDirectories;
    }

    @Cacheable("file")
    public String readTextFile(String fullpath) {

        logger.info("Reading text file: " + fullpath);

        File file = new File(fullpath);
        if (!file.exists()) {
            logger.error("File doesn't exist: " + fullpath);
            return null;
        }

        if (file.isDirectory()) {
            logger.error("Path is a directory: " + fullpath);
            return null;
        }

        try {
            if (isBinary(file)) {
                logger.error("File is binary: " + fullpath);
                return null;
            }
        } catch (IOException e) {
            logger.error("Error reading file: " + fullpath, e);
            return null;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(fullpath));
            return StringUtils.join(lines, '\n');
        } catch (IOException e) {
            logger.error("Error reading file: " + fullpath, e);
            return null;
        }

    }

    public boolean isBinary(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                return false; // Empty file considered text
            }
            for (int i = 0; i < bytesRead; i++) {
                int unsignedByte = buffer[i] & 0xFF;
                // Check for NUL byte
                if (unsignedByte == 0x00) {
                    return true;
                }
                // Check for control characters excluding allowed ones
                if (unsignedByte < 0x20 && !isAllowedControlCharacter(unsignedByte)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isAllowedControlCharacter(int unsignedByte) {
        return unsignedByte == 0x09 || // Tab
                unsignedByte == 0x0A || // Newline
                unsignedByte == 0x0B || // Vertical Tab
                unsignedByte == 0x0C || // Form Feed
                unsignedByte == 0x0D; // Carriage Return
    }

}