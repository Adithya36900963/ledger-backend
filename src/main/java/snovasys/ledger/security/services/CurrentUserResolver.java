package snovasys.ledger.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import snovasys.ledger.users.entity.ACCOUNT_HOLDER;
import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.repository.ACCOUNT_HOLDERRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserResolver {
    private final ACCOUNT_HOLDERRepo ACCOUNT_HOLDERRepo;

    // =========================================================
    // CURRENT USER
    // =========================================================

    public User getCurrentUser(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new UsernameNotFoundException(
                    "User is not authenticated."
            );
        }

        

        User user =(User)authentication.getPrincipal();

        if (user == null) {
            throw new UsernameNotFoundException(
                    "Authenticated user no longer exists."
            );
        }

        return user;
    }

    // =========================================================
    // CURRENT ACCOUNT_HOLDER
    // =========================================================

    public ACCOUNT_HOLDER getCurrentACCOUNT_HOLDER(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        return ACCOUNT_HOLDERRepo.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Authenticated user is not a ACCOUNT_HOLDER."
                        )
                );
    }

    // =========================================================
    // CURRENT ADMIN
    // =========================================================

    public User getCurrentAdmin(
            Authentication authentication) {

        User user = getCurrentUser(authentication);

        if (user.getRole() == null ||
                !"ADMIN".equals(user.getRole().name())) {

            throw new IllegalStateException(
                    "Authenticated user is not an admin."
            );
        }

        return user;
    }
}