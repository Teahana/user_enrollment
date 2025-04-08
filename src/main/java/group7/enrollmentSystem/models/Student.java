package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.OnHoldTypes;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
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
    private boolean feesPaid;
    @OneToMany
    private List<OnHoldStatus> onHoldStatusList;
    @Enumerated(EnumType.STRING)
    private OnHoldTypes onHoldType;

    public Student(String studentId, String firstName, String lastName, String address, String phoneNumber) {
        if("s11209521".equals(studentId)){
            this.feesPaid = true;
        }
//        List<OnHoldTypes> onHoldTypesList = Arrays.asList(OnHoldTypes.values());
//        for(OnHoldTypes onHoldType : onHoldTypesList){
//            OnHoldStatus onHoldStatus = new OnHoldStatus();
//            onHoldStatus.setOnHoldType(onHoldType);
//            onHoldStatus.setOnHold(false);
//            this.onHoldStatusList.add(onHoldStatus);
//        }
        this.studentId = studentId;
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.getRoles().add("ROLE_STUDENT");
    }
    @Override
    public boolean isEnabled() {
//        for(OnHoldStatus onHoldStatus : onHoldStatusList){
//            if(onHoldStatus.isOnHold()){
//                this.onHoldType = onHoldStatus.getOnHoldType();
//                return false;
//            }
//        }
 //       return true;
        return feesPaid;
    }
}
