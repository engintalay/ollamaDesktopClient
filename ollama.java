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

        // Add key binding for Ctrl + Enter to send the question
        questionArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl ENTER"), "sendQuestion");
        questionArea.getActionMap().put("sendQuestion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendButton.doClick(); // Simulate button click
            }
        });

        JButton sendButton = new JButton("Soruyu Gönder");
        sendButton.setPreferredSize(new Dimension(150, 30)); // Set button size explicitly
        JPanel buttonPanel = new JPanel(); // Add a separate panel for the button
        buttonPanel.add(sendButton);

        JTextPane responsePane = new JTextPane(); // Use JTextPane for formatted display
        responsePane.setEditable(false);
        responsePane.setContentType("text/html"); // Set content type to HTML for formatting
        JScrollPane responseScroll = new JScrollPane(responsePane);

        JTextArea debugArea = new JTextArea(5, 40);
        debugArea.setEditable(false);
        debugArea.setLineWrap(true);
        debugArea.setWrapStyleWord(true);
        JScrollPane debugScroll = new JScrollPane(debugArea);

        JScrollPane questionScroll = new JScrollPane(questionArea);

        JLabel evalDurationLabel = new JLabel("Evaluation Duration: N/A");

        // Düğme tıklama olayı
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionArea.getText().trim();
                if (!question.isEmpty()) {
                    String response = sendQuestionToServer(question, debugArea, evalDurationLabel);
                    StringBuilder formattedResponse = new StringBuilder("<html><body style='font-family: Arial; font-size: 12px;'>");

                    String[] responseLines = response.split("\n");
                    for (String line : responseLines) {
                        try {
                            JSONObject jsonLine = new JSONObject(line);
                            if (jsonLine.has("total_duration")) {
                                // Handle total_duration as an integer and format it to seconds
                                int durationNanoseconds = jsonLine.getInt("total_duration");
                                double durationSeconds = durationNanoseconds / 1_000_000_000.0; // Convert to seconds
                                String formattedDuration = String.format("%.2f seconds", durationSeconds); // Format to 2 decimal places
                                evalDurationLabel.setText("Evaluation Duration: " + formattedDuration); // Update the label with the formatted duration
                            } else if (jsonLine.has("error")) {
                                String error = jsonLine.getString("error"); // Extract the "error" value
                                evalDurationLabel.setText("Error: " + error); // Update the label with the error message    
                            }
                            if (jsonLine.has("response")) {
                                String temp = jsonLine.getString("response");
                                formattedResponse.append(temp).append("<br>"); // Add line breaks for HTML
                            }
                        } catch (Exception ex) {
                            formattedResponse.append(line.trim()).append("<br>"); // Append as-is if not valid JSON
                        }
                    }

                    formattedResponse.append("</body></html>");
                    responsePane.setText(formattedResponse.toString()); // Set formatted HTML content
                } else {
                    JOptionPane.showMessageDialog(frame, "Lütfen bir soru girin!", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Bileşenleri ekle
        panel.add(questionScroll, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER); // Add button panel to the center
        panel.add(responseScroll, BorderLayout.SOUTH);
        JPanel debugPanel = new JPanel(new BorderLayout());
        debugPanel.add(evalDurationLabel, BorderLayout.SOUTH);
        panel.add(debugPanel, BorderLayout.EAST);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static String sendQuestionToServer(String question, JTextArea debugArea, JLabel evalDurationLabel) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);

            // JSON formatında soru ve model gönder
            String jsonInput = "{\"prompt\": \"" + question + "\", \"model\": \"gemma3:4b\"}";
            String jsonInputCurl = "{\\\"prompt\\\": \\\"" + question + "\\\", \\\"model\\\": \\\"gemma3:4b\\\"}";
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
                    StringBuilder responseBuilder = new StringBuilder();
                    String responseLine;

                    while ((responseLine = br.readLine()) != null) {
                        responseBuilder.append(responseLine);
                    }

                    // Insert \n between JSON objects
                    String rawResponse = responseBuilder.toString();
                    String formattedResponse = rawResponse.replace("}{", "}\n{");

                    // Update evalDurationLabel (if applicable)
                    evalDurationLabel.setText("Evaluation Duration: N/A");
                    //System.out.println("Response: " + formattedResponse); // Print the raw response for debugging   
                    return formattedResponse;
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