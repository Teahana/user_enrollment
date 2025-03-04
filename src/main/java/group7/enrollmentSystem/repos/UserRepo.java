package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u.feesPaid FROM User u WHERE u.email = :email")
    boolean feesPaid(@Param("email") String email);
    @Modifying
    @Query("Update User u SET u.feesPaid = :feesPaid WHERE u.email = :email")
    void updateFeesPaid(@Param("email") String email, @Param("feesPaid") boolean feesPaid);
}
