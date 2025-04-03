package group7.enrollmentSystem.helpers;

import group7.enrollmentSystem.services.CourseService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class NodeMicroserviceClient {

    private Process nodeProcess;
    private final String generateUrl = "http://localhost:3001/generate-svg";
    private final String pingUrl = "http://localhost:3001/ping";
    private final RestTemplate restTemplate = new RestTemplate();
    private final CourseService courseService;

    public NodeMicroserviceClient(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Starts the Node.js service on application startup.
     */
    @PostConstruct
    public void startNodeServer() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                // On Windows, run Node directly
                pb = new ProcessBuilder("node", "node-mermaid-svg-service/server.js");
                pb.directory(new File(System.getProperty("user.dir")));
            } else {
                // On Linux
                pb = new ProcessBuilder("/home/teahana/enrollment-system/enrollmentSystem/user_enrollment/node-mermaid-svg-service/start-node.sh");
            }

            pb.redirectErrorStream(true);
            nodeProcess = pb.start();

            // Log Node.js output
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

    /**
     * Stops the Node.js service on application shutdown.
     */
    @PreDestroy
    public void stopNodeServer() {
        if (nodeProcess != null && nodeProcess.isAlive()) {
            nodeProcess.destroy();
        }
    }

    /**
     * Generates an SVG by fetching the Mermaid diagram code for the given course
     * and then communicating with the Node.js service.
     *
     * @param courseId the ID of the course
     * @return the SVG as a byte array
     */
    public byte[] generateSvg(Long courseId) {
        String code = courseService.getMermaidDiagramForCourse(courseId);
        if (code == null || code.trim().isEmpty()) {
            return "<svg><text x='10' y='20'>Code is missing</text></svg>"
                    .getBytes(StandardCharsets.UTF_8);
        }
        // Flatten the Mermaid code by replacing newlines
        code = code.replace("\n", "; ").replace("\r", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> jsonMap = Map.of("code", code);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(jsonMap, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(generateUrl, requestEntity, String.class);
            if (response.getBody() == null) {
                return "<svg><text x='10' y='20'>Empty response</text></svg>"
                        .getBytes(StandardCharsets.UTF_8);
            }
            return response.getBody().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "<svg><text x='10' y='20'>Error generating diagram</text></svg>"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Pings the Node.js service to check if it is up.
     *
     * @return the ping response
     */
    public String pingNodeService() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(pingUrl, String.class);
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error pinging node service";
        }
    }
}


