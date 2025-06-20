package app;

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

    // Model seçimi için combobox
    private static JComboBox<String> modelComboBox;

    // Chat geçmişi için
    private static List<String[]> chatHistory = new ArrayList<>(); // ["user"/"assistant", mesaj]

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

    // Sunucudan modelleri çek
    private static List<String> fetchModels(String serverUrl) {
        List<String> models = new ArrayList<>();
        try {
            URL url = new URL(serverUrl.replace("/api/generate", "/api/tags"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int status = conn.getResponseCode();
            if (status == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    String json = sb.toString();
                    // Beklenen json: {"models":[{"name":"model1"}, ...]}
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    if (obj.has("models")) {
                        for (var el : obj.getAsJsonArray("models")) {
                            JsonObject m = el.getAsJsonObject();
                            if (m.has("name")) models.add(m.get("name").getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            models.add("gemma3:4b"); // fallback default
        }
        if (models.isEmpty()) models.add("gemma3:4b");
        return models;
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

        // urlPanel'i dikey hizalamak için BoxLayout kullan
        JPanel urlPanel = new JPanel();
        urlPanel.setLayout(new BoxLayout(urlPanel, BoxLayout.Y_AXIS));
        JPanel urlInputPanel = new JPanel(new BorderLayout(5, 5));
        urlInputPanel.add(new JLabel("Sunucu URL'si:"), BorderLayout.WEST);
        urlInputPanel.add(serverUrlField, BorderLayout.CENTER);
        urlInputPanel.add(saveUrlButton, BorderLayout.EAST);
        urlPanel.add(urlInputPanel);

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

        urlPanel.add(serverUrlComboBox);
        urlPanel.add(removeUrlButton);

        // Model combobox'u oluştur
        List<String> models = fetchModels(serverUrl);
        modelComboBox = new JComboBox<>(models.toArray(new String[0]));
        modelComboBox.setSelectedIndex(0);
        JPanel modelPanel = new JPanel(new BorderLayout(5, 5));
        modelPanel.add(new JLabel("Model:"), BorderLayout.WEST);
        modelPanel.add(modelComboBox, BorderLayout.CENTER);

        urlPanel.add(modelPanel);

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

        JTextPane responsePane = new JTextPane();
        responsePane.setEditable(false);
        responsePane.setContentType("text/plain");
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

        JButton newChatButton = new JButton("Yeni Sohbet");
        newChatButton.setPreferredSize(new Dimension(150, 30));
        newChatButton.addActionListener(e -> {
            chatHistory.clear();
            updateChatPane(responsePane);
        });
        buttonPanel.add(newChatButton);

        // Modelleri güncellemek için buton
        JButton refreshModelsButton = new JButton("Modelleri Güncelle");
        refreshModelsButton.setPreferredSize(new Dimension(150, 30));
        refreshModelsButton.addActionListener(e -> {
            String selectedUrl = (String) serverUrlComboBox.getSelectedItem();
            List<String> newModels = fetchModels(selectedUrl);
            modelComboBox.removeAllItems();
            for (String m : newModels) modelComboBox.addItem(m);
            modelComboBox.setSelectedIndex(0);
        });
        buttonPanel.add(refreshModelsButton);

        // Düğme tıklama olayı
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionArea.getText().trim().replaceAll("\\r?\\n", " ");
                String selectedModel = (String) modelComboBox.getSelectedItem();
                if (!question.isEmpty()) {
                    chatHistory.add(new String[]{"user", question});
                    updateChatPane(responsePane, true); // Yanıt bekleniyor mesajı ile
                    questionArea.setText(""); // Soru alanını temizle
                    System.out.println("\n****************Başlandı****************\n");
                    evalDurationLabel.setText("Evaluation Duration: Çalışılıyor");
                    new Thread(() -> sendQuestionToServerWithHistory(question, selectedModel, debugArea, evalDurationLabel, responsePane)).start();
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

    // Soru gönderirken model parametresi de alınacak
    private static String sendQuestionToServer(String question, String model, JTextArea debugArea, JLabel evalDurationLabel, JTextPane responsePane) {
        try {
            String defaultServerUrl = (String) serverUrlComboBox.getSelectedItem();
            URL url = new URL(defaultServerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);
            // JSON formatında soru ve model gönder
            String jsonInput = String.format("{\"prompt\": \"%s\", \"model\": \"%s\", \"stream\": true}", question, model);
            String jsonInputCurl = String.format("{\\\"prompt\\\": \\\"%s\\\", \\\"stream\\\": true, \\\"model\\\": \\\"%s\\\"}", question, model);
            String curlCommand = "curl -X POST " + defaultServerUrl + " -H \"Content-Type: application/json\" -d \"" + jsonInputCurl + "\"";
            System.out.println(curlCommand);
            debugArea.setText(curlCommand);
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
                           // responsePane.append(temp);
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

    // Chat geçmişini ekrana yazan fonksiyon (renkli başlıklar için)
    private static void updateChatPane(JTextPane responsePane) {
        updateChatPane(responsePane, false);
    }
    private static void updateChatPane(JTextPane responsePane, boolean waiting) {
        try {
            javax.swing.text.StyledDocument doc = responsePane.getStyledDocument();
            doc.remove(0, doc.getLength());
            javax.swing.text.Style userStyle = responsePane.addStyle("user", null);
            javax.swing.text.StyleConstants.setForeground(userStyle, Color.BLUE);
            javax.swing.text.Style assistantStyle = responsePane.addStyle("assistant", null);
            javax.swing.text.StyleConstants.setForeground(assistantStyle, new Color(0, 128, 0));
            javax.swing.text.Style normalStyle = responsePane.addStyle("normal", null);
            javax.swing.text.StyleConstants.setForeground(normalStyle, Color.BLACK);
            for (String[] msg : chatHistory) {
                if (msg[0].equals("user")) {
                    doc.insertString(doc.getLength(), "Kullanıcı: ", userStyle);
                    doc.insertString(doc.getLength(), msg[1] + "\n", normalStyle);
                } else {
                    doc.insertString(doc.getLength(), "Asistan: ", assistantStyle);
                    doc.insertString(doc.getLength(), msg[1] + "\n", normalStyle);
                }
            }
            if (waiting) {
                doc.insertString(doc.getLength(), "Yanıt bekleniyor...\n", normalStyle);
            }
            responsePane.setCaretPosition(doc.getLength());
        } catch (Exception ex) {
            responsePane.setText("Hata: " + ex.getMessage());
        }
    }

    // Chat geçmişiyle birlikte sunucuya gönder
    private static String sendQuestionToServerWithHistory(String question, String model, JTextArea debugArea, JLabel evalDurationLabel, JTextPane responsePane) {
        try {
            String defaultServerUrl = (String) serverUrlComboBox.getSelectedItem();
            URL url = new URL(defaultServerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);
            // Geçmişi prompt olarak birleştir
            StringBuilder prompt = new StringBuilder();
            for (String[] msg : chatHistory) {
                if (msg[0].equals("user")) prompt.append("Kullanıcı: ").append(msg[1]).append("\n");
                else prompt.append("Asistan: ").append(msg[1]).append("\n");
            }
            // Sadece yeni soruyu ekle, tekrar etme
            // prompt.append("Kullanıcı: ").append(question).append("\nAsistan: ");
            // Satır sonu karakterlerini escape et
            String promptEscaped = prompt.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            String jsonInput = String.format("{\"prompt\": \"%s\", \"model\": \"%s\", \"stream\": true}", promptEscaped, model);
            String jsonInputCurl = String.format("{\\\"prompt\\\": \\\"%s\\\", \\\"stream\\\": true, \\\"model\\\": \\\"%s\\\"}", promptEscaped, model);
            String curlCommand = "curl -X POST " + defaultServerUrl + " -H \"Content-Type: application/json\" -d \"" + jsonInputCurl + "\"";
            System.out.println(curlCommand);
            debugArea.setText(curlCommand);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInput.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }
            int status = connection.getResponseCode();
            if (status == 200) {
                StringBuilder assistantMsg = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        JsonObject jsonLine = JsonParser.parseString(responseLine).getAsJsonObject();
                        if (jsonLine.has("response")) {
                            String temp = jsonLine.get("response").getAsString();
                            assistantMsg.append(temp);
                            updateChatPane(responsePane); // Anlık güncelleme
                        }
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
                    }
                }
                chatHistory.add(new String[]{"assistant", assistantMsg.toString()});
                updateChatPane(responsePane);
                return "Tamamlandı";
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