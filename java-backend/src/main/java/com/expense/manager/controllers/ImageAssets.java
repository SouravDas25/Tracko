package com.expense.manager.controllers;

import com.expense.manager.services.LogoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

@Controller
public class ImageAssets {

    private static final Logger log = LoggerFactory.getLogger(ImageAssets.class);

    @Autowired
    LogoService logoService;

    @RequestMapping(value="/getImage")
    ResponseEntity<byte[]> getImageUrl(@RequestParam(name = "domain") String domain) {
        try{
            return logoService.getImage(domain);
        }
        catch (HttpClientErrorException throwable) {
        }
        catch (Throwable throwable){
            log.error("error : ", throwable);
        }
        return logoService.getAlternateImage(domain);
    }

}
