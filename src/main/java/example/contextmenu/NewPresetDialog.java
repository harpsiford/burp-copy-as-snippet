package example.contextmenu;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog for creating a new preset, pre-populated with Default preset values.
 */
public class NewPresetDialog {

    private final PresetStore presetStore;

    public NewPresetDialog(PresetStore presetStore) {
        this.presetStore = presetStore;
    }

    public void show(Component parent) {
        Preset defaults = Preset.createDefault();

        JTextField nameField = new JTextField();
        JComboBox<String> scopeCombo = new JComboBox<>(new String[]{"User", "Project"});

        JTextArea headerRegexesArea = new JTextArea(6, 40);
        headerRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        headerRegexesArea.setText(String.join("\n", defaults.getHeaderRegexes()));

        JTextArea cookieRegexesArea = new JTextArea(4, 40);
        cookieRegexesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        cookieRegexesArea.setText(String.join("\n", defaults.getCookieRegexes()));

        JTextArea templateArea = new JTextArea(5, 40);
        templateArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        templateArea.setText(defaults.getTemplate());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JPanel nameRow = new JPanel(new BorderLayout(5, 0));
        nameRow.add(new JLabel("Name:"), BorderLayout.WEST);
        nameRow.add(nameField, BorderLayout.CENTER);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel scopeRow = new JPanel(new BorderLayout(5, 0));
        scopeRow.add(new JLabel("Scope:"), BorderLayout.WEST);
        scopeRow.add(scopeCombo, BorderLayout.CENTER);
        scopeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel placeholderHint = new JLabel("Template placeholders: {{request}}, {{response}}");
        placeholderHint.setFont(placeholderHint.getFont().deriveFont(Font.ITALIC, 11f));

        form.add(nameRow);
        form.add(Box.createVerticalStrut(5));
        form.add(scopeRow);
        form.add(Box.createVerticalStrut(10));
        form.add(labeledScroll("Header regexes (one per line):", headerRegexesArea));
        form.add(Box.createVerticalStrut(10));
        form.add(labeledScroll("Cookie regexes (one per line):", cookieRegexesArea));
        form.add(Box.createVerticalStrut(10));
        form.add(placeholderHint);
        form.add(labeledScroll("Template:", templateArea));

        form.setPreferredSize(new Dimension(500, 500));

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    parent, form, "Create new preset",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Name cannot be empty.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            String scope = (String) scopeCombo.getSelectedItem();
            List<String> headers = parseLines(headerRegexesArea.getText());
            List<String> cookies = parseLines(cookieRegexesArea.getText());
            String template = templateArea.getText();

            Preset preset = new Preset(name, headers, cookies, template);

            if ("Project".equals(scope)) {
                List<Preset> list = new ArrayList<>(presetStore.getProjectPresets());
                list.removeIf(p -> p.getName().equals(name));
                list.add(preset);
                presetStore.setProjectPresets(list);
            } else {
                List<Preset> list = new ArrayList<>(presetStore.getUserPresets());
                list.removeIf(p -> p.getName().equals(name));
                list.add(preset);
                presetStore.setUserPresets(list);
            }
            return;
        }
    }

    private static List<String> parseLines(String text) {
        List<String> result = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private static JPanel labeledScroll(String label, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }
}
