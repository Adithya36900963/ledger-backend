package snovasys.ledger.admin.model;

import java.math.BigDecimal;

public record AdminUserView(Long userId, String name, String email, String number, String role,
        boolean emailVerified, boolean mobileVerified, String accountHolderStatus,
        Long accountId, BigDecimal balance) { }
