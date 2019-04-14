package com.expense.manager.services;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
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
import sun.net.www.MessageHeader;
import sun.net.www.http.ChunkedInputStream;
import sun.net.www.http.HttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Scanner;

@Service
public class LogoService {

    private static final Logger log = LoggerFactory.getLogger(LogoService.class);

    private final RestTemplate restTemplate;

    public LogoService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @SuppressWarnings({ "restriction", "resource" })
	public ResponseEntity<byte[]> getImage(String domain) throws IOException {
        String url = "https://logo.clearbit.com/"+domain;
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        log.info("url {} ",url);

        ResponseEntity<byte[]> entity1 = this.restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

        ByteArrayInputStream bais = new ByteArrayInputStream(entity1.getBody());
        ChunkedInputStream cis = new ChunkedInputStream(bais, new HttpClient() {}, null);
        String result1 = CharStreams.toString(new InputStreamReader(bais));
//        log.info("chunked stream {} ",result1);
        String result = CharStreams.toString(new InputStreamReader(cis));
        return ResponseEntity.ok()
                .contentLength(result.length())
                .contentType(entity1.getHeaders().getContentType())
                .body(result.getBytes());
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
