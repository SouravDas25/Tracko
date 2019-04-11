package com.expense.manager.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class MainController {
	
	@RequestMapping(value="/")
	@ResponseBody
	public String demo() {
		return "Hello World";
	}

}
