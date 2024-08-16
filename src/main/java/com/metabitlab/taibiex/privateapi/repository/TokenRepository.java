package com.metabitlab.taibiex.privateapi.repository;

import com.metabitlab.taibiex.privateapi.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
}
