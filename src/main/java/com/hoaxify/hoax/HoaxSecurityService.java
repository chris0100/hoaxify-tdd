package com.hoaxify.hoax;

import com.hoaxify.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HoaxSecurityService {

    @Autowired
    HoaxRepository hoaxRepository;

    public boolean isAllowedToDelete(long hoaxId, User loggedInUser){
        Optional<Hoax> optionalHoax =  hoaxRepository.findById(hoaxId);
        if (optionalHoax.isPresent()){
            Hoax inDB = optionalHoax.get();
            return inDB.getUser().getId().equals(loggedInUser.getId());
        }
        return false;
    }
}
