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

public class ResourceExtractor {

    public static Path extractGraphvizBundle() throws Exception {
        Path gvRoot = CommonUtil.createDirectory(BIN_DIR)
                .resolve(GRAPHVIZ_DEPENDENCIES)
                .resolve(MACOS_AARCH64_DIR);
        if (Files.exists(gvRoot.resolve(BIN_DIR).resolve(DOT_BINARY))) {
            return gvRoot;
        }
        extractResourceDirectory(gvRoot);
        Path dot = gvRoot.resolve(BIN_DIR).resolve(DOT_BINARY);
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

    private static void extractResourceDirectory(Path targetRoot) throws Exception {
        List<String> files;
        try (InputStream list = ResourceExtractor.class.getClassLoader()
                .getResourceAsStream(GRAPHVIZ_LIB_FILE)) {

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
            outputPath = Path.of(outputPath.toString().replace(GRAPHVIZ_BUNDLE_DIR + "/" + MACOS_AARCH64_DIR + "/", ""));
            Files.createDirectories(outputPath.getParent());

            try (InputStream in = ResourceExtractor.class.getClassLoader().getResourceAsStream(inputPath)) {
                if (in == null) {
                    throw new FileNotFoundException(IO_EXCEPTION_MESSAGE_2 + inputPath);
                }
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

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
