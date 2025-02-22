package io.owenrbee.filesapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FileVO {

    @Schema(example = "filename.ext")
    private String name;

    @Schema(example = "false")
    private boolean binary;

    @Schema(example = "false")
    private boolean folder;

    @Schema(example = "/root/path/subdir")
    private String parentDir;

}
