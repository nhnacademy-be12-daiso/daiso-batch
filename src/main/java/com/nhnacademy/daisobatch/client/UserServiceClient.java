package com.nhnacademy.daisobatch.client;

import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(name = "TEAM3-USER")
//@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/birthday")
    List<BirthdayUserDto> getBirthdayUsers(
            @RequestParam("month") int month,
            @RequestParam("page") int page,
            @RequestParam("size") int size
            );
}
