package view;

import graphviz.GraphvizService;
import process.ReasonerProcessRunner;
import process.PythonProcessRunner;
import utils.CommonUtil;
import io.ResourceExtractor;
import io.JsonWriter;
import guru.nidi.graphviz.engine.*;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static constants.ErrorMessageConstants.RUNTIME_ERROR_MESSAGE_3;
import static constants.PathConstants.*;
import static io.ResourceExtractor.extractGraphvizBundle;
import static utils.CommonUtil.isMacOS;
import static validation.FactFoilValidation.validateFactFoil;

/**
 * UI view component for generating contrastive explanations.
 *
 * <p>This class builds a simple form for user inputs (fact, foil, query),
 * runs reasoning and post-processing as external processes (reasoner JAR,
 * Python venv and requirements, Graphviz), and displays the generated graph PNG.
 * </p>
 */
public class ContrastiveView extends AbstractOWLViewComponent {

    private JTextArea factArea, foilArea, queryArea;
    private JButton runBtn;
    private JTextArea nlOutput;
    private JLabel graphLabel; // shows PNG from dot
    private JLabel imageLabel;

    /**
     * Initialize the view components and layout.
     *
     * <p>Sets up:
     * - Graphviz engine initialization,
     * - form with inputs and button.
     * </p>
     */
    @Override
    protected void initialiseOWLView() throws Exception {
        // Initialize Graphviz engine by using the bundled Graphviz distribution
        initGraphviz();
        // Central image area (separate from split pane) - initial placeholder
        imageLabel = new JLabel("No image yet", SwingConstants.CENTER);
        JScrollPane imageScroll = new JScrollPane(imageLabel);
        add(imageScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        // Build the input form (Fact / Foil / Query + run button)
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel("Fact:"), c);
        c.gridx = 1;
        factArea = new JTextArea(2, 40);
        form.add(new JScrollPane(factArea), c);

        c.gridx = 0;
        c.gridy = 1;
        form.add(new JLabel("Foil:"), c);
        c.gridx = 1;
        foilArea = new JTextArea(2, 40);
        form.add(new JScrollPane(foilArea), c);

        c.gridx = 0;
        c.gridy = 2;
        form.add(new JLabel("Query:"), c);
        c.gridx = 1;
        queryArea = new JTextArea(2, 40);
        form.add(new JScrollPane(queryArea), c);

        runBtn = new JButton("Generate Explanations");
        c.gridx = 1;
        c.gridy = 3;
        c.anchor = GridBagConstraints.EAST;
        form.add(runBtn, c);

        add(form, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        nlOutput = new JTextArea(8, 80);
        nlOutput.setEditable(false);
        split.setTopComponent(new JScrollPane(nlOutput));
        graphLabel = new JLabel();
        graphLabel.setHorizontalAlignment(SwingConstants.CENTER);
        split.setBottomComponent(new JScrollPane(graphLabel));
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);
        runBtn.addActionListener(e -> runExplanationAsync());
    }

    /**
     * Initialize Graphviz engine using the bundled Graphviz distribution.
     *
     * Extracts the Graphviz bundle.
     */
    private void initGraphviz() throws Exception {
        String dotBinary = DOT_BINARY_WINDOWS;
        if (isMacOS()) {
            dotBinary = DOT_BINARY;
        }
        Path gvRoot = extractGraphvizBundle();
        String dotPath = gvRoot.resolve(BIN_DIR).resolve(dotBinary).toString();
        Graphviz.useEngine(new GraphvizCmdLineEngine(dotPath));
        System.setProperty("java.awt.headless", "true");
    }

    /**
     * Run the full explanation generation pipeline.
     *
     * <p>Pipeline steps (in order):
     * 1. Save the active OWL ontology to a temp file,
     * 2. Validate user inputs (fact/foil/query) against the ontology,
     * 3. Create reasoner input JSON and run the external reasoner JAR,
     * 4. Create Python venv and install requirements, run Python post-processing,
     * 5. Use Graphviz service to obtain DOT and convert it to a PNG,
     * 6. Update the UI with the resulting image or show errors.</p>
     * <p>
     */
    private void runExplanationAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Path pngOut;

            @Override
            protected Void doInBackground() throws Exception {
                // Grab the active ontology from the Protege model manager
                OWLModelManager mm = getOWLModelManager();
                OWLOntology activeOntology = mm.getActiveOntology(); // the in-memory ontology user opened
                // Save to a temp OWL file
                File owlTmp = CommonUtil.createDirectory(INPUT_DIR).resolve(INPUT_OWL_FILE).toFile();
                mm.getOWLOntologyManager().saveOntology(
                        activeOntology, new RDFXMLDocumentFormat(), IRI.create(owlTmp));
                // Collect user inputs
                String fact = factArea.getText().trim();
                String foil = foilArea.getText().trim();
                String query = queryArea.getText().trim();
                // Validate inputs and ensure entities exist in the ontology
                try {
                    validateFactFoil(fact, foil, query, activeOntology);
                } catch (IllegalArgumentException e) {
                    throw new Exception("Validation error: " + e.getMessage());
                }

                String dot = "";
                try {
//                     1) Create reasoner input JSON and extract the embedded reasoner JAR
                    JsonWriter.createInputJsonFile(fact, foil, query);
                    ResourceExtractor.extractJar();

                    // 2) Run the reasoner external JAR using the generated JSON as input
                    String jsonInputFilePath = String.valueOf(CommonUtil.createDirectory(INPUT_DIR).resolve(REASONER_INPUT_JSON));
                    ReasonerProcessRunner processRunner = new ReasonerProcessRunner(jsonInputFilePath);
                    processRunner.runReasoner();

                    // 3) Prepare Python environment
                    PythonProcessRunner pythonProcess = new PythonProcessRunner();
                    Path venvDir = pythonProcess.createVenvAndInstallRequirements();

                    // 4) Use Graphviz service to produce DOT and convert to PNG
                    Path gvRoot = extractGraphvizBundle();
                    GraphvizService graphvizService = new GraphvizService(venvDir, gvRoot);
                    dot = graphvizService.graphvizRunner();
                    pngOut = graphvizService.convertDotToPng(dot);
                    return null;
                } catch (Throwable e) {
                    throw new RuntimeException(RUNTIME_ERROR_MESSAGE_3 + e
                            + Arrays.toString(e.getStackTrace()) + e.getCause() + dot + pngOut);
                }
            }

            @Override
            protected void done() {
                runBtn.setEnabled(true);
                try {
                    get(); // rethrow exceptions
                    if (pngOut != null) {
                        BufferedImage img = ImageIO.read(pngOut.toFile());
                        graphLabel.setIcon(new ImageIcon(img));
                        graphLabel.setText(null);
                        graphLabel.revalidate();
                        graphLabel.repaint();
                    }
                } catch (Exception ex) {
                    nlOutput.setText("Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Convenience method to show an image in the center image label.
     *
     * @param pngPath path to the PNG file to display
     * @throws IOException if reading the image fails
     */
    private void showImage(Path pngPath) throws IOException {
        BufferedImage img = ImageIO.read(pngPath.toFile());
        imageLabel.setIcon(new ImageIcon(img));
        imageLabel.setText(null);
        imageLabel.revalidate();
        imageLabel.repaint();
    }

    @Override
    protected void disposeOWLView() { /* no-op */ }
}
