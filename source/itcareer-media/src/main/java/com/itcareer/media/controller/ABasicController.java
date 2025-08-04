package com.itcareer.media.controller;


import com.itcareer.media.jwt.ItcareerJwt;
import com.itcareer.media.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

public class ABasicController {
    @Autowired
    UserServiceImpl userService;
    public ItcareerJwt getSessionFromToken(){
        return userService.getAddInfoFromToken();
    }
}
