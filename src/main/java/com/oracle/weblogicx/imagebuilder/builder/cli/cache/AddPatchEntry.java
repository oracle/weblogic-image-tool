package com.oracle.weblogicx.imagebuilder.builder.cli.cache;

import com.oracle.weblogicx.imagebuilder.builder.api.model.CommandResponse;
import com.oracle.weblogicx.imagebuilder.builder.api.model.WLSInstallerType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.oracle.weblogicx.imagebuilder.builder.util.ARUConstants.DEFAULT_WLS_VERSION;

@Command(
        name = "addPatch",
        description = "Add cache entry for wls|fmw patch or psu",
        mixinStandardHelpOptions = true,
        sortOptions = false
)
public class AddPatchEntry implements Callable<CommandResponse> {

    @Override
    public CommandResponse call() throws Exception {
        if (patchNumber != null && !patchNumber.isEmpty()
                && location != null && Files.exists(location) && location.toFile().isFile()) {
            //TODO: GET Release number for corresponding patch
            // this also serves as validation that the given patch exists for said version and product
        }
        return null;
    }

    @Option(
            names = { "--patchId" },
            description = "Patch number. Ex: p28186730",
            required = true
    )
    private String patchNumber;

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
            names = { "--cli" },
            description = "If the command is being executed via CLI Mode",
            hidden = true
    )
    private boolean cliMode;
}
