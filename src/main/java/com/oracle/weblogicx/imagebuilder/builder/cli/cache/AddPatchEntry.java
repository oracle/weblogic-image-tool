package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSInstallerType;
import com.oracle.weblogicx.imagebuilder.builder.util.ARUUtil;
import com.oracle.weblogicx.imagebuilder.builder.util.SearchResult;
import com.oracle.weblogicx.imagebuilder.builder.util.XPathUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Document;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.impl.meta.FileMetaDataResolver.META_RESOLVER;
import static com.oracle.weblogicx.imagebuilder.builder.api.meta.MetaDataResolver.CACHE_KEY_SEPARATOR;
import static com.oracle.weblogicx.imagebuilder.builder.util.Constants.DEFAULT_WLS_VERSION;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        //mixinStandardHelpOptions = true,
        sortOptions = false
)
public class AddPatchEntry implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        if (patchId != null && !patchId.isEmpty()
                && userId != null && !userId.isEmpty()
                && password != null && !password.isEmpty()
                && location != null && Files.exists(location) && Files.isRegularFile(location)) {
            String patchNumber = this.patchId.toLowerCase();
            if (patchNumber.startsWith("p") && patchNumber.length() > 1) {
                patchNumber = patchNumber.substring(1);
            }
            SearchResult result = ARUUtil.getPatchDetail(type.toString(), version, patchNumber, userId, password);
            if (result.isSuccess()) {
                Document document = result.getResults();
                String patchDigest = XPathUtil.applyXPathReturnString(document, "string"
                        + "(/results/patch[1]/files/file/digest[@type='SHA-256']/text())");
                String localDigest = DigestUtils.sha256Hex(new FileInputStream(location.toFile()));

                if (localDigest.equalsIgnoreCase(patchDigest)) {
                    String releaseNumber = XPathUtil.applyXPathReturnString(document,
                            "string(/results/patch[1]/release/@id)");
                    String key = patchNumber + CACHE_KEY_SEPARATOR + releaseNumber;
                    META_RESOLVER.addToCache(key, location.toAbsolutePath().toString());
                    return new CommandResponse(0, String.format(
                            "Added Patch entry %s=%s for %s", key, location.toAbsolutePath(), type));
                } else {
                    return new CommandResponse(-1, String.format("Local file sha-256 digest %s != patch digest %s", localDigest, patchDigest));
                }
            }
        } else {
            return new CommandResponse(-1, "Invalid arguments");
        }
        return null;
    }

    @Option(
            names = { "--patchId" },
            description = "Patch number. Ex: p28186730",
            required = true
    )
    private String patchId;

    @Option(
            names = { "--type" },
            description = "Type of patch. Valid values: ${COMPLETION-CANDIDATES}",
            required = true,
            defaultValue = "wls"
    )
    private WLSInstallerType type;

    @Option(
            names = { "--ver" },
            description = "version of mw this patch is for. Ex: 12.2.1.3.0",
            required = true,
            defaultValue = DEFAULT_WLS_VERSION
    )
    private String version;

    @Option(
            names = { "--path" },
            description = "Location on disk. For ex: /path/to/FMW/patch.zip",
            required = true
    )
    private Path location;

    @Option(
            names = { "--user" },
            paramLabel = "<support email>",
            required = true,
            description = "Oracle Support email id"
    )
    private String userId;

    @Option(
            names = { "--password" },
            paramLabel = "<password associated with support user id>",
            required = true,
            description = "Password for support userId"
    )
    private String password;

    @Unmatched
    List<String> unmatcheOptions;
}
