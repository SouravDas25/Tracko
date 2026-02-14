package com.trako.dtos;

import com.trako.entities.Contact;
import com.trako.entities.Split;

public class SplitDetailDTO {
    private Split split;
    private Contact contact;

    public SplitDetailDTO(Split split, Contact contact) {
        this.split = split;
        this.contact = contact;
    }

    public Split getSplit() {
        return split;
    }

    public void setSplit(Split split) {
        this.split = split;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }
}
