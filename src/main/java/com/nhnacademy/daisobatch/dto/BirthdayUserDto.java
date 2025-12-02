package com.nhnacademy.daisobatch.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BirthdayUserDto {
    private Long userCreatedId;
    private String username; // 로그용
    private LocalDate birth; // 설계용
}
