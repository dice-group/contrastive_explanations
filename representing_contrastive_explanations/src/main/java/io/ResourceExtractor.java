package io;

import utils.CommonUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Collectors;

import static constants.ErrorMessageConstants.IO_EXCEPTION_MESSAGE_1;
import static constants.ErrorMessageConstants.IO_EXCEPTION_MESSAGE_2;
import static constants.PathConstants.*;
import static utils.CommonUtil.isMacOS;

/**
 * Utility responsible for extracting bundled resources (Graphviz runtime and reasoner JAR)
 * from the application's classpath into the local filesystem layout expected by the runtime.
 *
 * <p>Extraction targets:
 * - a platform-specific Graphviz bundle under `bin/graphviz-dependencies/...`
 * - the packaged reasoner JAR into `bin/contrastive-explanations-reasoner.jar`</p>
 */
public class ResourceExtractor {
    /**
     * Ensure the Graphviz bundle is present and return the root path to that bundle.
     *
     * @return path to the graphviz bundle root directory
     * @throws Exception when resource extraction or filesystem operations fail
     */
    public static Path extractGraphvizBundle() throws Exception {
        // Build the expected bundle root under the project's bin
        String os = WINDOWS;
        String dotBinary = DOT_BINARY_WINDOWS;
        if (isMacOS()) {
            os = MACOS_AARCH64_DIR;
            dotBinary = DOT_BINARY;
        }
        Path gvRoot = CommonUtil.createDirectory(BIN_DIR)
                .resolve(GRAPHVIZ_DEPENDENCIES)
                .resolve(os);
        // Extract all listed resources into the target root
        extractResourceDirectory(gvRoot, os);
        Path dot = gvRoot.resolve(BIN_DIR).resolve(dotBinary);
        try {
            Files.setPosixFilePermissions(
                    dot,
                    PosixFilePermissions.fromString("rwxr-xr-x")
            );
        } catch (UnsupportedOperationException e) {
            // Ignore if the file system does not support POSIX permissions
        }
        return gvRoot;
    }

    /**
     * Read the resource listing file (`GRAPHVIZ_LIB_FILE`) from the classpath and copy
     * each listed entry into the provided `targetRoot`.
     *
     * @param targetRoot destination root where resources will be written
     * @throws Exception on I/O failures or missing resources
     */
    private static void extractResourceDirectory(Path targetRoot, String OS) throws Exception {
        List<String> files;
        // Read the resource list file from the classpath
        String resourceFile = GRAPHVIZ_LIB_FILE_MAC;
        if(OS.equals(WINDOWS)){
            resourceFile = GRAPHVIZ_LIB_FILE_WINDOWS;
        }
        try (InputStream list = ResourceExtractor.class.getClassLoader()
                .getResourceAsStream(resourceFile)) {
            assert list != null;
            files = new BufferedReader(new InputStreamReader(list))
                    .lines()
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new IOException(IO_EXCEPTION_MESSAGE_1, e);
        }
        for (String inputPath : files) {
            Path outputPath = targetRoot.resolve(inputPath);
            Path prefix = Path.of(GRAPHVIZ_BUNDLE_DIR, OS);
            outputPath = Path.of(outputPath.toString().replace(prefix.toString(), ""));
            // Ensure parent directories exist before copying the file
            Files.createDirectories(outputPath.getParent());
            try (InputStream in = ResourceExtractor.class.getClassLoader().getResourceAsStream(inputPath)) {
                if (in == null) {
                    throw new FileNotFoundException(IO_EXCEPTION_MESSAGE_2 + inputPath);
                }
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    /**
     * Extract the packaged reasoner JAR from the classpath `lib/` resource into `bin/`.
     *
     * <p>Creates `bin` if necessary, then copies `lib/contrastive-explanations-reasoner.jar`
     * from the plugin resources to `bin/contrastive-explanations-reasoner.jar` on disk.</p>
     *
     * @throws IOException when reading or writing the jar resource fails
     */
    public static void extractJar() throws IOException {
        Path binDir = CommonUtil.baseDir().resolve(BIN_DIR);
        Files.createDirectories(binDir);
        Path outJar = binDir.resolve(RESOURCE_JAR);
        try (InputStream in = ResourceExtractor.class.getClassLoader()
                .getResourceAsStream(LIB_DIR + "/" + RESOURCE_JAR)) {
            if (in == null) {
                throw new FileNotFoundException("Resource jar not found inside plugin");
            }
            Files.copy(in, outJar, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}


