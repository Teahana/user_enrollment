package group7.enrollmentSystem.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class GradeService {

    private Map<String, Map<String, Object>> gradeData;

    @PostConstruct
    public void loadGradeThresholds() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        gradeData = objectMapper.readValue(
                new ClassPathResource("configs/gradesThreshold.json").getFile(),
                new TypeReference<Map<String, Map<String, Object>>>() {}
        );
        System.out.println("Grade thresholds loaded: " + gradeData);
    }

    public String getGrade(int mark) {
        return gradeData.entrySet().stream()
                .sorted((e1, e2) -> {
                    Integer v1 = (Integer) e1.getValue().get("mark");
                    Integer v2 = (Integer) e2.getValue().get("mark");
                    return v2.compareTo(v1);
                })
                .filter(entry -> mark >= (Integer) entry.getValue().get("mark"))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("F");
    }

    public double getGradePoint(String grade) {
        Map<String, Object> data = gradeData.get(grade);
        if (data == null) return 0.0;
        Object gpaObj = data.get("gpa");
        return gpaObj instanceof Number ? ((Number) gpaObj).doubleValue() : 0.0;
    }

    public int getLowestPassingMark() {
        return (int) gradeData.get("D").get("mark");
    }

    public void setGradeThresholds(Map<String, Map<String, Object>> newData) throws IOException {
        if (newData == null || newData.isEmpty()) {
            throw new IllegalArgumentException("Grade thresholds cannot be null or empty");
        }
        this.gradeData = newData;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(
                new ClassPathResource("configs/gradesThreshold.json").getFile(),
                this.gradeData
        );
    }
}
