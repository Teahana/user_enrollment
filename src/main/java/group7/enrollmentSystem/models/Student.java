package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.OnHoldTypes;
import jakarta.persistence.*;
        import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@PrimaryKeyJoinColumn(name = "id")
@AttributeOverrides({
        @AttributeOverride(name = "email", column = @Column(name = "email", nullable = false, unique = true)),
        @AttributeOverride(name = "firstName", column = @Column(name = "first_name")),
        @AttributeOverride(name = "lastName", column = @Column(name = "last_name"))
})
@NoArgsConstructor
public class Student extends User {
    @Column(nullable = false, unique = true)
    private String studentId;
    private String phoneNumber;
    private String address;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OnHoldStatus> onHoldStatusList = new ArrayList<>();

    public Student(String studentId, String firstName, String lastName, String address, String phoneNumber) {
        this.studentId = studentId;
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.getRoles().add("ROLE_STUDENT");
    }

    /*@Override
    public boolean isEnabled() {
        return onHoldStatusList.stream().noneMatch(OnHoldStatus::isOnHold);
    }*/

    public Optional<OnHoldStatus> getActiveHold() {
        return onHoldStatusList.stream()
                .filter(h -> {
                    return h.isOnHold();
                })
                .findFirst();
    }
}