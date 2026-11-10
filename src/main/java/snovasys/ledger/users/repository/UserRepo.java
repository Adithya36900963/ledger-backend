package snovasys.ledger.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.Role;


public interface UserRepo extends  JpaRepository<User, Long>{
    @Query("""
            select u from User u where u.email=:data OR u.number=:data
            """)
    public User findByEmailOrNumber(@Param("data") String data);

    boolean existsByRole(Role role);

}
