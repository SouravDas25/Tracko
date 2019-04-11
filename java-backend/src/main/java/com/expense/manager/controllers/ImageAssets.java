package com.expense.manager.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ImageAssets {

    @RequestMapping(value="/getImage")
    @ResponseBody
    String getImageUrl(@RequestParam(name = "domain") String domain) {
        return "kasmfikamsodmaosdmoiamsdoimasmdpamsdpasmdo";
    }

}
