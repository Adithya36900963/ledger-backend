package snovasys.ledger.security.common.email;


public interface EmailService {

    void sendEmail(String to, String subject, String text);

}