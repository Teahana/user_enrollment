package group7.enrollmentSystem.helpers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class NodeMicroserviceClient {

    private Process nodeProcess;

    private final String serviceUrl = "http://localhost:3001/generate-svg";

    @PostConstruct
    public void startNodeServer() {
        try {
            ProcessBuilder pb = new ProcessBuilder("node", "node-mermaid-svg-service/server.js");
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            nodeProcess = pb.start();

            //log output for debugging
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(nodeProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[NodeService] " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            throw new RuntimeException("Failed to start Node.js microservice", e);
        }
    }

    @PreDestroy
    public void stopNodeServer() {
        if (nodeProcess != null && nodeProcess.isAlive()) {
            nodeProcess.destroy();
        }
    }

    public String generateSvg(String mermaidCode) {
        try {
            URL url = new URL(serviceUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            String jsonInput = String.format("{\"code\": \"%s\"}", mermaidCode.replace("\"", "\\\""));

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
                return response.toString(); // SVG XML string
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "<svg><text x='10' y='20'>Error rendering diagram</text></svg>";
        }
    }
}

