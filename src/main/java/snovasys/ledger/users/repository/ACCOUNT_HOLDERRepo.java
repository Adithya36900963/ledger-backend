package snovasys.ledger.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import snovasys.ledger.users.entity.ACCOUNT_HOLDER;

public interface ACCOUNT_HOLDERRepo  extends JpaRepository<ACCOUNT_HOLDER,Long>{

    Optional<ACCOUNT_HOLDER> findByUser_Id(@Param("userId") Long userId);

    @Query("""
                select cu.id from ACCOUNT_HOLDER cu where cu.user.id = : userId
            """)
    Long getACCOUNT_HOLDERIdByUserId(@Param("userId") Long userId);
}
