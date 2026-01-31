package com.trako.controllers;

import com.trako.services.NlpDataService;
import com.trako.util.GZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class SmsExtractionController {

    private static final Logger logger = LoggerFactory.getLogger(SmsExtractionController.class);

    private static final String pythonBackendUrl = "http://souravdas25.pythonanywhere.com";

    @Autowired
    NlpDataService nlpDataService;

    @PostMapping(value = "/api/dialog", produces = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<String> forwardToNlp(RequestEntity<?> requestEntity) {
        HttpEntity<?> entity = new HttpEntity<>(requestEntity.getBody(), requestEntity.getHeaders());
        ResponseEntity<byte[]> response = new RestTemplate()
                .exchange(pythonBackendUrl + "/apis/", HttpMethod.POST, entity, byte[].class);
        byte[] gzipResponse = response.getBody();
        String responseBody = GZipUtil.unzip(gzipResponse);
        try {
            nlpDataService.save(requestEntity.getBody().toString(), responseBody);
        } catch (Exception e) {
            logger.error("Cannot save analytics : ", e);
        }
        logger.info("response received : {}", responseBody);
        return ResponseEntity.ok().body(responseBody);
    }

}
