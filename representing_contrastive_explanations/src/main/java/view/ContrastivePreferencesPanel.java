package view;

import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.protege.editor.owl.ui.preferences.OWLPreferencesPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Preferences panel for the contrastive explanations plugin.
 */
public class ContrastivePreferencesPanel extends OWLPreferencesPanel {
    public static final String PREF_SET = "edu.upb.contrastive";
    private JTextField endpointField, apiKeyField, dotPathField;

    /**
     * Build the UI and populate fields with current preference values.
     * <p>
     * The layout uses a GridBagLayout for simple two-column alignment:
     * label on the left, input field on the right.
     */
    @Override
    public void initialise() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;

        add(new JLabel("Endpoint URL:"), c);
        c.gridx = 1;
        endpointField = new JTextField(32);
        add(endpointField, c);

        c.gridx = 0;
        c.gridy = 1;
        add(new JLabel("API Key (if any):"), c);
        c.gridx = 1;
        // JPasswordField extends JTextField so assigning to JTextField is acceptable;
        // it provides masked input for sensitive values.
        apiKeyField = new JPasswordField(32);
        add(apiKeyField, c);

        c.gridx = 0;
        c.gridy = 2;
        add(new JLabel("Graphviz 'dot' path (optional):"), c);
        c.gridx = 1;
        dotPathField = new JTextField(32);
        add(dotPathField, c);

        // Load current values from the preferences store and set them into the fields.
        Preferences p = PreferencesManager.getInstance().getPreferencesForSet(PREF_SET, "general");
        endpointField.setText(p.getString("endpoint", ""));
        apiKeyField.setText(p.getString("apikey", ""));
        dotPathField.setText(p.getString("dotPath", ""));
    }

    /**
     * Persist current field values into the preferences store.
     */
    @Override
    public void applyChanges() {
        Preferences p = PreferencesManager.getInstance().getPreferencesForSet(PREF_SET, "general");
        p.putString("endpoint", endpointField.getText().trim());
        p.putString("apikey", apiKeyField.getText().trim());
        p.putString("dotPath", dotPathField.getText().trim());
    }

    @Override
    public void dispose() {
    }
}
