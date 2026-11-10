package snovasys.ledger.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import snovasys.ledger.users.entity.Admin;

public interface AdminRepo extends JpaRepository<Admin,Long>{
    boolean existsByUser_Id(Long userId);
}
