package io.owenrbee.filesapi.model;

import lombok.Data;

@Data
public class FileVO {

    private String name;

    private boolean binary;

    private boolean folder;

    private String parentDir;

}
