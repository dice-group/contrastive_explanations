package constants;


/**
 * Collection of environment-related constants used across the application.
 *
 * <p>This class centralizes names of environment variables, common string values (like output
 * formats) and identifiers (like the reasoner name and fully-qualified main class).</p>
 */
public class EnvConstants {

    public static final String DYLD_LIBRARY_PATH = "DYLD_LIBRARY_PATH";

    public static final String GVBINDIR = "GVBINDIR";

    public static final String GVPLUGIN_PATH = "GVPLUGIN_PATH";

    public static final String PATH = "PATH";

    public static final String HERMIT_REASONER = "HERMIT";

    public static final String JSON_OUTPUT_FORMAT = "json";

    public static final String REASONER_MAIN_CLASS = "anonymized.contrastive.experiments.FactFoilFileWriter";
}
