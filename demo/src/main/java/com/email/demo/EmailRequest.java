package com.email.demo;

import lombok.Data;

@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
}
