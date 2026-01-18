package com.ragnarok.ragnarok_customers_training_diary.entity.repository;

import com.ragnarok.ragnarok_customers_training_diary.entity.AdminEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUserEmail(String adminEmail);

}
