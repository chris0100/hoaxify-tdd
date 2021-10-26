package com.hoaxify.user;

import com.hoaxify.UserRepository;
import com.hoaxify.error.NotFoundException;
import com.hoaxify.user.vm.UserUpdateVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    public User save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Page<User> getUsers(User loggedInUser, Pageable pageable) {
        if (loggedInUser != null) {
            return userRepository.findByUsernameNot(loggedInUser.getUsername(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    public User getByUsername(String username) {
        User inDB = userRepository.findByUsername(username);
        if (inDB == null){
            throw new NotFoundException(username + " not found");
        }
        return inDB;
    }

    public User update(long id, UserUpdateVM userUpdate) {
        User inDB = userRepository.getById(id);
        String savedImageName = inDB.getUsername() + UUID.randomUUID().toString().replace("-", "");

        inDB.setDisplayName(userUpdate.getDisplayName());
        inDB.setImage(savedImageName);
        return userRepository.save(inDB);
    }
}
