package snovasys.ledger.security.common.whathsapp;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import snovasys.ledger.users.models.WhatsAppRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WhathsappServiceImpl implements WhathsappService {

    private final RestClient restClient;

    @Override
    public void send(WhatsAppRequest request) {
        restClient.post()
                .uri("/whatsapp/send")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}