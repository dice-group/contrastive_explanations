package utils;

import process.ReasonerProcessRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CommonUtil {

    private static final String APP_DIR = ".protege/contrastive-explanations";

    private static final String USER_DIR = "user.home";

    public static Path baseDir() throws IOException {
        Path dir = Paths.get(System.getProperty(USER_DIR), APP_DIR);
        Files.createDirectories(dir);return dir;
    }

    public static Path createDirectory(String folder) throws IOException {
        Path dir = baseDir().resolve(folder);
        Files.createDirectories(dir);
        return dir;
    }

    public static Path getTempFile(String resourcePath) throws IOException {
        try (InputStream in = ReasonerProcessRunner.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            Path tempFile = Files.createTempFile("contrastive_", "_" + Paths.get(resourcePath).getFileName());
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }
}
