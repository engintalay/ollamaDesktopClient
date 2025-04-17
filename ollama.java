import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ollama {
    private static final String SERVER_URL = "http://localhost:11434/api/generate";
    private static final String CONFIG_FILE = "config.txt";

    // Declare serverUrlComboBox as a class-level variable
    private static JComboBox<String> serverUrlComboBox;

    private static List<String> loadServerUrls() {
        List<String> urls = new ArrayList<>();
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (Scanner scanner = new Scanner(configFile)) {
                while (scanner.hasNextLine()) {
                    String url = scanner.nextLine().trim();
                    if (!url.isEmpty()) {
                        urls.add(url);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (urls.isEmpty()) {
            urls.add(SERVER_URL); // Add default URL if none exist
        }
        return urls;
    }

    private static void saveServerUrls(List<String> urls) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            for (String url : urls) {
                writer.write(url + System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverUrl = loadServerUrls().get(0);

        // Frame oluştur
        JFrame frame = new JFrame("Soru-Cevap Uygulaması");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1280, 900); // Set frame size to 800x600

        // Panel ve bileşenler
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10)); // Add padding between components

        JTextArea questionArea = new JTextArea(20, 40);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);

        JTextField serverUrlField = new JTextField(serverUrl, 40);
        JButton saveUrlButton = new JButton("URL'yi Kaydet");
        saveUrlButton.addActionListener(e -> {
            String newUrl = serverUrlField.getText().trim();
            if (!newUrl.isEmpty()) {
                List<String> urls = loadServerUrls();
                if (!urls.contains(newUrl)) { // Ensure uniqueness
                    urls.add(newUrl);
                    saveServerUrls(urls);
                    serverUrlComboBox.addItem(newUrl); // Add to combo box
                    JOptionPane.showMessageDialog(frame, "Sunucu URL'si kaydedildi!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "Bu URL zaten mevcut!", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(frame, "URL boş olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel urlPanel = new JPanel(new BorderLayout(5, 5));
        urlPanel.add(new JLabel("Sunucu URL'si:"), BorderLayout.WEST);
        urlPanel.add(serverUrlField, BorderLayout.CENTER);
        urlPanel.add(saveUrlButton, BorderLayout.EAST);

        // Add a combo box for selecting server URLs
        List<String> serverUrls = loadServerUrls();
        serverUrlComboBox = new JComboBox<>(serverUrls.toArray(new String[0]));
        serverUrlComboBox.setSelectedItem(serverUrl);
        serverUrlComboBox.addActionListener(e -> {
            String selectedUrl = (String) serverUrlComboBox.getSelectedItem();
            if (selectedUrl != null) {
                serverUrlField.setText(selectedUrl);
            }
        });

        JButton removeUrlButton = new JButton("URL'yi Sil");
        removeUrlButton.addActionListener(e -> {
            String selectedUrl = (String) serverUrlComboBox.getSelectedItem();
            if (selectedUrl != null) {
                serverUrls.remove(selectedUrl);
                saveServerUrls(serverUrls);
                serverUrlComboBox.removeItem(selectedUrl);
                JOptionPane.showMessageDialog(frame, "Sunucu URL'si silindi!", "Bilgi", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Silinecek bir URL seçin!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });

        urlPanel.add(serverUrlComboBox, BorderLayout.NORTH);
        urlPanel.add(removeUrlButton, BorderLayout.SOUTH);

        JButton sendButton = new JButton("Soruyu Gönder"); // Move this declaration above key binding setup
        sendButton.setPreferredSize(new Dimension(150, 30)); // Set button size explicitly

        // Add key binding for Ctrl + Enter to send the question
        questionArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl ENTER"), "sendQuestion");
        questionArea.getActionMap().put("sendQuestion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendButton.doClick(); // Simulate button click
            }
        });

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5)); // Adjust layout to fit three buttons
        buttonPanel.add(sendButton); // Add the send button

        JTextArea responsePane = new JTextArea(); // Change JTextPane to JTextArea for plain text
        responsePane.setEditable(false);
        responsePane.setLineWrap(true); // Enable line wrapping
        responsePane.setWrapStyleWord(true); // Wrap at word boundaries
        JScrollPane responseScroll = new JScrollPane(responsePane);

        JTextArea debugArea = new JTextArea(5, 40);
        debugArea.setEditable(false);
        debugArea.setLineWrap(true);
        debugArea.setWrapStyleWord(true);
        JScrollPane debugScroll = new JScrollPane(debugArea);

        JScrollPane questionScroll = new JScrollPane(questionArea);

        JLabel evalDurationLabel = new JLabel("Evaluation Duration: N/A");
        buttonPanel.add(evalDurationLabel); // Add the duration label

        JButton exitButton = new JButton("Çıkış"); // Create an Exit button
        exitButton.setPreferredSize(new Dimension(150, 30)); // Set button size explicitly
        exitButton.addActionListener(e -> System.exit(0)); // Add action listener to exit the application
        buttonPanel.add(exitButton); // Add the exit button

        // Düğme tıklama olayı
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionArea.getText().trim().replaceAll("\\r?\\n", " "); // Remove all line breaks
                if (!question.isEmpty()) {
                    System.out.println("\n****************Başlandı****************\n"); // Print "Başlandı" to console
                    evalDurationLabel.setText("Evaluation Duration: Çalışılıyor"); // Set label to "Çalışılıyor"
                    responsePane.setText(""); // Clear the responsePane before starting
                    new Thread(() -> sendQuestionToServer(question, debugArea, evalDurationLabel, responsePane)).start(); // Run in a separate thread
                } else {
                    JOptionPane.showMessageDialog(frame, "Lütfen bir soru girin!", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Panel düzenini güncelle
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, questionScroll, responseScroll);
        splitPane.setResizeWeight(0.5); // Üst ve alt bölmelerin boyutlarını ayarla

        // Bileşenleri ekle
        panel.add(urlPanel, BorderLayout.NORTH); // Add URL panel at the top
        panel.add(splitPane, BorderLayout.CENTER); // Add split pane to the center
        panel.add(buttonPanel, BorderLayout.SOUTH); // Place button panel at the bottom

        frame.add(panel);
        frame.setVisible(true);
    }

    private static String sendQuestionToServer(String question, JTextArea debugArea, JLabel evalDurationLabel, JTextArea responsePane) {
        try {
            String defaultServerUrl = (String) serverUrlComboBox.getSelectedItem();
            URL url = new URL(defaultServerUrl); // Use the selected server URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);

            // JSON formatında soru ve model gönder
            String jsonInput = "{\"prompt\": \"" + question + "\", \"model\": \"gemma3:4b\", \"stream\": true}";
            String jsonInputCurl = "{\\\"prompt\\\": \\\"" + question + "\\\", \\\"stream\\\": true, \\\"model\\\": \\\"gemma3:4b\\\"}";
            String curlCommand = "curl -X POST " + defaultServerUrl + " -H \"Content-Type: application/json\" -d \"" + jsonInputCurl + "\"";
            System.out.println(curlCommand); // Print the curl command for debugging
            debugArea.setText(curlCommand); // Debug alanına curl komutunu yaz

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            // Cevabı oku
            int status = connection.getResponseCode();
            if (status == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        JsonObject jsonLine = JsonParser.parseString(responseLine).getAsJsonObject();
                        if (jsonLine.has("total_duration")) {
                            long durationNanoseconds = jsonLine.get("total_duration").getAsLong(); // Corrected method
                            double durationSeconds = durationNanoseconds / 1_000_000_000.0; // Convert to seconds
                            String formattedDuration = String.format("%.2f seconds", durationSeconds); // Format to 2 decimal places
                            evalDurationLabel.setText("Evaluation Duration: " + formattedDuration); // Update the label with the formatted duration
                            for (String key : jsonLine.keySet()) {
                                if (key.equals("context")) {
                                    continue;
                                }
                                if ( key.equals("prompt_eval_duration") || key.equals("load_duration") || key.equals("total_duration") || key.equals("eval_duration") ) {
                                    long nanoSec = jsonLine.get(key).getAsLong(); // Corrected method
                                    double durSec = nanoSec / 1_000_000_000.0; // Convert to seconds
                                    String durStr = String.format("%.2f seconds", durSec);
                                    System.out.println(key + ": " + durStr); // Print the formatted duration
                                } else {
                                    System.out.println(key + ": " + jsonLine.get(key));
                                }  
                            }
                        } else if (jsonLine.has("error")) {
                            String error = jsonLine.get("error").getAsString();
                            evalDurationLabel.setText("Error: " + error);
                        }
                        if (jsonLine.has("response")) {
                            String temp = jsonLine.get("response").getAsString();
                            responsePane.append(temp);
                        }
                        responsePane.setCaretPosition(responsePane.getDocument().getLength());
                    }
                    System.out.println("\n****************Bitti****************\n"); // Print "Başlandı" to console

                    return "Tamamlandı";
                }
            } else if (status == 404) {
                System.err.println("Sunucu bulunamadı. Lütfen sunucu adresini kontrol edin: " + defaultServerUrl);
                return "Sunucu bulunamadı. Lütfen sunucu adresini kontrol edin: " + defaultServerUrl;
            } else {
                System.err.println("Sunucudan geçerli bir cevap alınamadı. Kod: " + status);
                return "Sunucudan geçerli bir cevap alınamadı. Kod: " + status;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Hata: " + ex.getMessage());
            return "Hata: " + ex.getMessage();
        }
    }
}