package com.hoaxify.hoax;

import com.hoaxify.user.User;
import com.hoaxify.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class HoaxService {

    @Autowired
    HoaxRepository hoaxRepository;

    @Autowired
    UserService userService;


    public Hoax save(User user, Hoax hoax){
        hoax.setTimestamp(new Date());
        hoax.setUser(user);
        return hoaxRepository.save(hoax);
    }

    public Page<Hoax> getAllHoaxes(Pageable pageable) {
        return hoaxRepository.findAll(pageable);
    }

    public Page<Hoax> getHoaxesOfUser(String username, Pageable pageable) {
        User inDB = userService.getByUsername(username);

    }
}
