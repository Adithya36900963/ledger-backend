package snovasys.ledger.users.service.Auth;

import java.util.Map;

import snovasys.ledger.users.entity.User;
import snovasys.ledger.users.models.LoginModel;
import snovasys.ledger.users.models.RegisterModel;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface UserAuthService {

    Map<Object, Object> register(
            RegisterModel registerModel
    );

    Map<Object, Object> login(
        LoginModel loginModel,
        HttpServletResponse response,
        String clientIp,String role);

    Map<Object, Object> verifyEmail(
            String token
    );

    Map<Object, Object> generateEmailVerificationLink(
            User user, HttpServletRequest httpServletRequest
    );

    Map<Object, Object> generateMobileVerificationToken(
            User user, HttpServletRequest httpServletRequest
    );

    Map<Object, Object> verifyMobile(
            String token
    );
    String getBaseUrl(HttpServletRequest httpServletRequest);

    Map<Object, Object> logout(HttpServletResponse response);

    Map<Object, Object> loginACCOUNT_HOLDER(LoginModel loginModel, HttpServletResponse response, String clientIp);

    Map<Object, Object> loginAdmin(LoginModel loginModel, HttpServletResponse response, String clientIp);
}