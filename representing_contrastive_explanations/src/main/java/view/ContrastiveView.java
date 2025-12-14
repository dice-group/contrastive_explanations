package view;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ContrastiveView extends AbstractOWLViewComponent {

    private JTextArea factArea, foilArea, queryArea;
    private JButton runBtn;
    private JTextArea nlOutput;
    private JLabel graphLabel; // shows PNG from dot
    private JLabel imageLabel;

    @Override
    protected void initialiseOWLView() {
        initGraphviz();
        imageLabel = new JLabel("No image yet", SwingConstants.CENTER); // <-- initialized here
        JScrollPane imageScroll = new JScrollPane(imageLabel);
        add(imageScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        // Inputs
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

        // Outputs
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

    private void initGraphviz() {
        String dotPath = "/opt/homebrew/bin/dot";
        Graphviz.useEngine(new GraphvizCmdLineEngine(dotPath));
        System.setProperty("java.awt.headless", "true");
    }

    private void runExplanationAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
             Path pngOut;
            @Override protected Void doInBackground() throws Exception {
                //Grab the active ontology from Protégé
                OWLModelManager mm = getOWLModelManager();
                OWLOntology ont = mm.getActiveOntology(); // the in-memory ontology user opened

                // Save to a temp OWL file
                File owlTmp = JsonCreator.PluginFiles.inputsDir().resolve("family.owl").toFile();
                mm.getOWLOntologyManager().saveOntology(
                        ont, new RDFXMLDocumentFormat(), IRI.create(owlTmp));

                //Collect inputs
                String fact = factArea.getText().trim();
                String foil = foilArea.getText().trim();
                String query = queryArea.getText().trim();
                String dot = "";
                try {
                    JsonCreator.createInputJsonFile(fact, foil, query);
                    ProcessRunner.runReasoner();
                    dot = ProcessRunner.runGraphviz();
                    pngOut = GraphvizRender.toPng(dot);
                    return null;
                } catch (Exception e) {
                    throw new Exception("Failed to create dot image file: "
                            + Arrays.toString(e.getStackTrace()) + e.getCause() +  dot +   pngOut);
                } catch (Throwable e) {
                    throw new RuntimeException("Failed to create dot image file: " +e
                            + Arrays.toString(e.getStackTrace()) + e.getCause() +  dot +   pngOut);
                }
            }
            @Override protected void done() {
                runBtn.setEnabled(true);
                try {
                    get(); // rethrow exceptions
                    if (pngOut != null) {
//                        graphLabel.setIcon(new ImageIcon(pngOut.toString()));
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
