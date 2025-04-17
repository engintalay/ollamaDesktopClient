import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject; // Add this import for JSON parsing

public class ollama {
    private static final String SERVER_URL = "http://localhost:11434/api/generate";
    //private static final String SERVER_URL = "http://192.168.1.250:11434/api/generate";

    public static void main(String[] args) {
        // Frame oluştur
        JFrame frame = new JFrame("Soru-Cevap Uygulaması");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 1024); // Set frame size to 800x600

        // Panel ve bileşenler
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10)); // Add padding between components

        JTextArea questionArea = new JTextArea(20, 40);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);

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
                String question = questionArea.getText().trim();
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

        // Bileşenleri ekle
        panel.add(questionScroll, BorderLayout.NORTH);
        panel.add(responseScroll, BorderLayout.CENTER); // Move responseScroll to the center
        panel.add(buttonPanel, BorderLayout.SOUTH); // Place button panel at the bottom

        frame.add(panel);
        frame.setVisible(true);
    }

    private static String sendQuestionToServer(String question, JTextArea debugArea, JLabel evalDurationLabel, JTextArea responsePane) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);

            // JSON formatında soru ve model gönder
            String jsonInput = "{\"prompt\": \"" + question + "\", \"model\": \"gemma3:4b\", \"stream\": true}";
            String jsonInputCurl = "{\\\"prompt\\\": \\\"" + question + "\\\", \\\"stream\\\": true, \\\"model\\\": \\\"gemma3:4b\\\"}";
            String curlCommand = "curl -X POST " + SERVER_URL + " -H \"Content-Type: application/json\" -d \"" + jsonInputCurl + "\"";
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

                            JSONObject jsonLine = new JSONObject(responseLine);
                            if (jsonLine.has("total_duration")) {
                                // Handle total_duration as an integer and format it to seconds
                                int durationNanoseconds = jsonLine.getInt("total_duration");
                                double durationSeconds = durationNanoseconds / 1_000_000_000.0; // Convert to seconds
                                String formattedDuration = String.format("%.2f seconds", durationSeconds); // Format to 2 decimal places
                                evalDurationLabel.setText("Evaluation Duration: " + formattedDuration); // Update the label with the formatted duration
                                for (String key : jsonLine.keySet()) {
                                    System.out.println(key + ": " + jsonLine.get(key));
                                }
                            } else if (jsonLine.has("error")) {
                                String error = jsonLine.getString("error"); // Extract the "error" value
                                evalDurationLabel.setText("Error: " + error); // Update the label with the error message    
                            }
                            if (jsonLine.has("response")) {
                                String temp = jsonLine.getString("response");
                                //formattedResponse.append(temp); // Add line breaks for plain text
                                responsePane.append(temp ); // Append each line to the responsePane
                            }
                       
                        responsePane.setCaretPosition(responsePane.getDocument().getLength()); // Auto-scroll to the bottom
                    }
                    //evalDurationLabel.setText("Evaluation Duration: Tamamlandı");
                    System.out.println("\n****************Bitti****************\n"); // Print "Başlandı" to console

                    return "Tamamlandı";
                }
            } else if (status == 404) {
                return "Sunucu bulunamadı. Lütfen sunucu adresini kontrol edin: " + SERVER_URL;
            } else {
                return "Sunucudan geçerli bir cevap alınamadı. Kod: " + status;
            }
        } catch (Exception ex) {
            return "Hata: " + ex.getMessage();
        }
    }
}