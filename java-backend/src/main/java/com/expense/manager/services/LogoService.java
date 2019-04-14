package com.expense.manager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LogoService {

    private static final Logger log = LoggerFactory.getLogger(LogoService.class);

    private final RestTemplate restTemplate;

    public LogoService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResponseEntity<byte[]> getImage(String domain) {
        String url = "https://logo.clearbit.com/"+domain;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        log.info("url {} ",url);
        return this.restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
    }


    public ResponseEntity<byte[]> getAlternateImage(String domain) {
        String url = "https://ui-avatars.com/api/";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("name", domain);
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        return this.restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, byte[].class);
    }
}
