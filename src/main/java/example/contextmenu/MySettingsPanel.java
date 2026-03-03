// package example.contextmenu;

// import burp.api.montoya.MontoyaApi;
// import burp.api.montoya.persistence.Preferences;
// import burp.api.montoya.ui.settings.SettingsPanel;
// import burp.api.montoya.http.message.HttpHeader;

// import javax.swing.*;
// import java.awt.*;
// import java.util.Arrays;
// import java.util.List;
// import java.util.stream.Collectors;
// import java.util.regex.Pattern;


// public class MySettingsPanel implements SettingsPanel 
// {
//     public final static List<String> DEFAULT_knownUselessHeaders = List.of(
//         "Accept", 
//         "Accept-Language",
//         "Accept-Encoding",
//         "X-Pwnfox-Color", 
//         "Priority", 
//         "Te", 
//         "User-Agent", 
//         "X-Requested-With", 
//         "Vary", 
//         "Access-Control-Allow-Headers", 
//         "Access-Control-Allow-Methods", 
//         "X-Xss-Protection", 
//         "X-Content-Type-Options", 
//         "Access-Control-Expose-Headers", 
//         "Alt-Svc", 
//         "Via", 
//         "Server", 
//         "Sec-Ch-Ua", 
//         "Sec-Ch-Ua-Mobile", 
//         "Sec-Ch-Ua-Platform", 
//         "Upgrade-Insecure-Requests", 
//         "Sec-Fetch-Site", 
//         "Sec-Fetch-Mode", 
//         "Sec-Fetch-Dest", 
//         "Origin", 
//         "Referer"
//     );
    
//     public final static List<String> DEFAULT_knownUselessCookies = List.of(
//         "^_[\\w_]*$",
//         "^optimizely\\w+$",
//         "^AMP_[\\w_]+$",
//         "^ajs_\\w+_id$",
//         "^GOOG\\w+$"
//     );

//     private final MontoyaApi api;
//     private final Preferences preferences;

//     private final JTextArea cookiesTextArea;
//     private final JTextArea headersTextArea;

//     public MySettingsPanel(MontoyaApi api) {
//         this.api = api;
//         this.preferences = api.persistence().preferences();

//         JPanel panel = new JPanel(new BorderLayout(0, 10));

//         cookiesTextArea = new JTextArea(8, 40);
//         cookiesTextArea.setLineWrap(true);
//         cookiesTextArea.setWrapStyleWord(true);
//         panel.add(new JLabel("Cookies to remove (one regex per line):"), BorderLayout.NORTH);
//         panel.add(new JScrollPane(cookiesTextArea), BorderLayout.CENTER);

//         headersTextArea = new JTextArea(4, 40);
//         headersTextArea.setLineWrap(true);
//         headersTextArea.setWrapStyleWord(true);

//         JPanel headersPanel = new JPanel(new BorderLayout(0, 10));
//         headersPanel.add(new JLabel("Headers to remove (one header name per line):"), BorderLayout.NORTH);
//         headersPanel.add(new JScrollPane(headersTextArea), BorderLayout.CENTER);

//         JPanel combinedPanel = new JPanel();
//         combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.Y_AXIS));
//         combinedPanel.add(panel);
//         combinedPanel.add(Box.createVerticalStrut(15));
//         combinedPanel.add(headersPanel);

//         // Restore Defaults Button
//         JButton restoreDefaultsButton = new JButton("Restore Defaults");
//         restoreDefaultsButton.addActionListener(e -> {
//             setTextAreasFromDefaults();
//             saveSettings();
//         });

//         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//         buttonPanel.add(restoreDefaultsButton);

//         JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
//         mainPanel.add(combinedPanel, BorderLayout.CENTER);
//         mainPanel.add(buttonPanel, BorderLayout.SOUTH);

//         loadSettings();
//         saveSettings();

//         this.panel = mainPanel;
//     }

//     private JPanel panel;

//     @Override
//     public JPanel uiComponent() {
//         return panel;
//     }

//     public void saveSettings() {
//         List<String> cookies = Arrays.stream(cookiesTextArea.getText().split("\\R"))
//                                      .map(String::trim)
//                                      .filter(s -> !s.isEmpty())
//                                      .collect(Collectors.toList());

//         List<String> headers = Arrays.stream(headersTextArea.getText().split("\\R"))
//                                      .map(String::trim)
//                                      .filter(s -> !s.isEmpty())
//                                      .collect(Collectors.toList());

//         preferences.setString("uselessCookies", String.join(",", cookies));
//         preferences.setString("uselessHeaders", String.join(",", headers));
//     }

//     public void loadSettings() {
//         String cookiesPref = preferences.getString("uselessCookies");
//         if (cookiesPref == null || cookiesPref.isEmpty()) {
//             setTextAreaContent(cookiesTextArea, DEFAULT_knownUselessCookies);
//         } else {
//             setTextAreaContent(cookiesTextArea, Arrays.asList(cookiesPref.split(",")));
//         }

//         String headersPref = preferences.getString("uselessHeaders");
//         if (headersPref == null || headersPref.isEmpty()) {
//             setTextAreaContent(headersTextArea, DEFAULT_knownUselessHeaders);
//         } else {
//             setTextAreaContent(headersTextArea, Arrays.asList(headersPref.split(",")));
//         }
//     }

//     private void setTextAreaContent(JTextArea textArea, List<String> lines) {
//         String content = String.join("\n", lines);
//         textArea.setText(content);
//     }

//     private void setTextAreasFromDefaults() {
//         setTextAreaContent(cookiesTextArea, DEFAULT_knownUselessCookies);
//         setTextAreaContent(headersTextArea, DEFAULT_knownUselessHeaders);
//     }
// }
