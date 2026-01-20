package utils;

import process.ReasonerProcessRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Common filesystem utilities used across the application.
 */
public class CommonUtil {

    private static final String APP_DIR = ".protege/contrastive-explanations";

    private static final String USER_DIR = "user.home";

    /**
     * Return the application base directory path, creating it if necessary.
     *
     * @return path to the application base directory
     * @throws IOException if directory creation fails
     */
    public static Path baseDir() throws IOException {
        Path dir = Paths.get(System.getProperty(USER_DIR), APP_DIR);
        // Ensure the directory (and any parent directories) exist
        Files.createDirectories(dir);return dir;
    }

    /**
     * Create (if needed) and return a subdirectory under the application base directory.
     *
     * @param folder name of the subdirectory to create (relative to the base dir)
     * @return resolved path to the created subdirectory
     * @throws IOException if directory creation fails
     */
    public static Path createDirectory(String folder) throws IOException {
        Path dir = baseDir().resolve(folder);
        // Ensure the requested folder exists
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Copy a classpath resource to a temporary file and return the temp file path.
     *
     * @param resourcePath classpath resource path (relative to classloader root)
     * @return path to the created temporary file containing the resource content
     * @throws IOException if reading/copying the resource fails
     * @throws FileNotFoundException if the resource cannot be located on the classpath
     */
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

    public static Boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
