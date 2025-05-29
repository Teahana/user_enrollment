package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.enums.OnHoldTypes;
import group7.enrollmentSystem.models.HoldServiceRestriction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldServiceRestrictionRepo extends JpaRepository<HoldServiceRestriction, Long> {
    HoldServiceRestriction findByHoldType(OnHoldTypes holdType);
    boolean existsByHoldType(OnHoldTypes holdType);
}