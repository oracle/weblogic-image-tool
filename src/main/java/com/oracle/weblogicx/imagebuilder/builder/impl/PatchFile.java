package com.oracle.weblogicx.imagebuilder.builder.impl;

import com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver;
import com.oracle.weblogicx.imagebuilder.builder.api.model.AbstractFile;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.builder.util.ARUUtil;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PatchFile extends AbstractFile {

    private String userId;
    private String password;
    private String patchId;
    private WLSInstallerType type;
    private String version;

//    public PatchFile(String key) {
//        super(key);
//    }

//    private PatchFile(String key, String userId, String password) {
//        this(key);
//        this.userId = userId;
//        this.password = password;
//    }

    public PatchFile(WLSInstallerType type, String version, String patchId, String userId, String password) {
        super(null);
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        this.type = type;
        this.version = version;
        this.patchId = patchId;
        this.userId = userId;
        this.password = password;
    }

    @Override
    public String resolve(MetaDataResolver metaDataResolver) throws Exception {
        if (patchId == null || patchId.isEmpty()) {
            //this implies get latest psu
            this.key = ARUUtil.getLatestPSUFor(type.toString(), version, userId, password);
        } else {
            List<String> patches = ARUUtil.getPatchesFor(type.toString(), version, Collections.singletonList(patchId),
                    userId, password);
            if (patches != null && !patches.isEmpty()) {
                this.key = patches.get(0);
            }
        }

        if (this.key == null || this.key.isEmpty()) {
            if (this.patchId == null || this.patchId.isEmpty()) {
                throw new Exception(String.format("Failed to find latest psu for product type %s, version %s",
                        this.type, this.version));
            } else {
                throw new Exception(String.format("Failed to find patch %s for product type %s, version %s",
                        this.patchId, this.type, this.version));
            }
        } else {
            String filePath = metaDataResolver.getValueFromCache(this.key);
            if (filePath == null || !Files.isRegularFile(Paths.get(filePath))) {
                throw new Exception(String.format(
                        "File doesn't exist at location %s for product type %s, version %s, patch %s", filePath,
                        this.type, this.version, this.patchId));
            }
            return filePath;
        }
    }
}
