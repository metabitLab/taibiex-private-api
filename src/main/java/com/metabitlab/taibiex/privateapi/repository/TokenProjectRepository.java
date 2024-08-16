package com.metabitlab.taibiex.privateapi.repository;

import com.metabitlab.taibiex.privateapi.entity.TokenProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenProjectRepository extends JpaRepository<TokenProjectEntity, Long>, JpaSpecificationExecutor {

    TokenProjectEntity findByAddress(String address);

}
