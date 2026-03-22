package com.trako.dtos;

import java.util.Date;

public record NamedDateAmountRow(String entityName, Date date, Double amount) {}
