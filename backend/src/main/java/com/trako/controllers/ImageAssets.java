package com.trako.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

//@Controller
public class ImageAssets {

    private static final Logger log = LoggerFactory.getLogger(ImageAssets.class);

    @RequestMapping(value="/getImage")
    ResponseEntity<Void> getImageUrl(@RequestParam(name = "domain") String domain) {
//        try{
//            return logoService.getImage(domain);
//        }
//        catch (HttpClientErrorException throwable) {
//        }
//        catch (Throwable throwable){
//            log.error("error : ", throwable);
//        }
//        return logoService.getAlternateImage(domain);
        return ResponseEntity.ok().build();
    }

}
