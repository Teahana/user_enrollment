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
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                // On Windows - run Node directly (bypass .sh entirely)
                pb = new ProcessBuilder("node", "node-mermaid-svg-service/server.js");
                pb.directory(new File(System.getProperty("user.dir")));
            } else {
                // On Linux
                pb = new ProcessBuilder("/home/teahana/enrollment-system/enrollmentSystem/user_enrollment/node-mermaid-svg-service/start-node.sh");

            }

            pb.redirectErrorStream(true);
            nodeProcess = pb.start();

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

