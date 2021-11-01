package com.hoaxify.user;

import com.hoaxify.shared.CurrentUser;
import com.hoaxify.shared.GenericResponse;
import com.hoaxify.user.vm.UserUpdateVM;
import com.hoaxify.user.vm.UserVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/1.0")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/users")
    public GenericResponse createUser(@Valid @RequestBody User user){
        userService.save(user);
        return GenericResponse.builder().message("User saved").build();
    }

    @GetMapping("/users")
    public Page<UserVM> getUsers(@CurrentUser User loggedInUser, Pageable page){
        return userService.getUsers(loggedInUser, page)
                .map(UserVM::new);
    }

    @GetMapping("/users/{username}")
    public UserVM getUserByName(@PathVariable String username){
        User user = userService.getByUsername(username);
        return new UserVM(user);
    }


    @PutMapping("/users/{id:[0-9]+}")
    @PreAuthorize("#id == principal.id")
    public UserVM updateUser(@PathVariable long id, @Valid @RequestBody(required = false) UserUpdateVM userUpdate){
        User updated = userService.update(id, userUpdate);
        return new UserVM(updated);
    }

}
