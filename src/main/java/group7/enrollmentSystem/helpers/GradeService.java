package group7.enrollmentSystem.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Map;

@Service
public class GradeService {

    private Map<String, Integer> gradeThresholds;

    @PostConstruct
    public void loadGradeThresholds() throws IOException {
        // Load the JSON file from the resources directory
        ObjectMapper objectMapper = new ObjectMapper();
        gradeThresholds = objectMapper.readValue(
                new ClassPathResource("configs/gradesThreshold.json").getFile(),
                Map.class
        );
        System.out.println("Grade thresholds loaded: " + gradeThresholds);
    }

    public String getGrade(int mark) {
        // Sort the entries by their values (thresholds) in descending order
        return gradeThresholds.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Sort by value descending
                .filter(entry -> mark >= entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("F");
    }

    public void setGradeThresholds(Map<String, Integer> newThresholds) throws IOException {
        if (newThresholds == null || newThresholds.isEmpty()) {
            throw new IllegalArgumentException("Grade thresholds cannot be null or empty");
        }
        this.gradeThresholds = newThresholds;

        // Save the updated thresholds back to the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(
                new ClassPathResource("configs/gradesThreshold.json").getFile(),
                this.gradeThresholds
        );
    }
    public int getLowestPassingMark(){
        return gradeThresholds.get("D");
    }
}
