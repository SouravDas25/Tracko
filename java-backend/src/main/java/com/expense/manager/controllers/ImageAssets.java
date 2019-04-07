package com.expense.manager.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ImageAssets {

    @RequestMapping(name="/getImage")
    @ResponseBody
    String getImageUrl(String domain) {
        return "kasmfikamsodmaosdmoiamsdoimasmdpamsdpasmdo";
    }

}
