package com.hoaxify.hoax;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class HoaxService {

    @Autowired
    HoaxRepository hoaxRepository;


    public void save(Hoax hoax){
        hoax.setTimestamp(new Date());
        hoaxRepository.save(hoax);
    }
}
