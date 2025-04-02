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
            ProcessBuilder pb = new ProcessBuilder("./start-node.sh");
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
}

