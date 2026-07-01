package com.trako.dtos;

import com.trako.entities.Contact;
import com.trako.entities.Split;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SplitDetailDTO {
    private Split split;
    private Contact contact;
}
