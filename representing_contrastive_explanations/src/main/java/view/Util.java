package view;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Collectors;

public class Util {

    public static boolean isOSX() {
        String property = System.getProperty("os.name");
        if (property == null) {
            return false;
        } else {
            String osName = property.toLowerCase();
            return osName.contains("os x") || osName.contains("macos");
        }
    }

    public static Path extractGraphvizBundle() throws Exception {
        Path gvRoot = JsonCreator.PluginFiles.binsDir()
                .resolve("graphviz")
                .resolve("macos-aarch64");

        if (Files.exists(gvRoot.resolve("bin/dot"))) {
            return gvRoot;
        }

        extractResourceDirectory(
                "graphviz-bundle/macos-aarch64",
                gvRoot
        );

        // Ensure dot is executable
        Path dot = gvRoot.resolve("bin/dot");
        try {
            Files.setPosixFilePermissions(
                    dot,
                    PosixFilePermissions.fromString("rwxr-xr-x")
            );
        } catch (Exception ignored) {}

        return gvRoot;
    }

    public static void extractResourceDirectory(String resourceRoot, Path targetRoot) throws Exception {
        List<String> files = new java.util.ArrayList<>();
        try (InputStream list = Util.class.getClassLoader()
                .getResourceAsStream("graphvizLib.txt")) {

            assert list != null;
            files = new BufferedReader(new InputStreamReader(list))
                    .lines()
                    .filter(s -> !s.isBlank() && !s.startsWith("#"))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new IOException("Failed to read resource file list", e);
        }

        for (String inputPath : files) {
            Path outputPath = targetRoot.resolve(inputPath);
            outputPath = Path.of(outputPath.toString().replace("graphviz-bundle/macos-aarch64/", ""));
            Files.createDirectories(outputPath.getParent());

            try (InputStream in = Util.class.getClassLoader().getResourceAsStream(inputPath)) {
                if (in == null) {
                    throw new FileNotFoundException("Resource not found: " + inputPath);
                }
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }


    private static String dotResourcePath() {
        return isOSX()
                ? "/bin/macos/dot"
                : "/bin/windows/dot.exe";
    }

}
