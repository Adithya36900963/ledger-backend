package snovasys.ledger.security.common.whathsapp;

import snovasys.ledger.users.models.WhatsAppRequest;

public interface WhathsappService {

    void send(WhatsAppRequest request);
}
