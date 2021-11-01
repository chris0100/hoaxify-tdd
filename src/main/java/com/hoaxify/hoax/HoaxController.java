package com.hoaxify.hoax;

import com.hoaxify.hoax.vm.HoaxVM;
import com.hoaxify.shared.CurrentUser;
import com.hoaxify.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/1.0")
public class HoaxController {

    @Autowired
    HoaxService hoaxService;


    @PostMapping("/hoaxes")
    public HoaxVM createHoax(@Valid @RequestBody Hoax hoax, @CurrentUser User user){
        return new HoaxVM(hoaxService.save(user, hoax));
    }


    @GetMapping("/hoaxes")
    public Page<HoaxVM> getAllHoaxes(Pageable pageable){
        return hoaxService.getAllHoaxes(pageable)
                .map(HoaxVM::new);
    }


    @GetMapping("/users/{username}/hoaxes")
    public Page<HoaxVM> getHoaxesOfUser(@PathVariable String username, Pageable pageable){
        return hoaxService.getHoaxesOfUser(username, pageable)
                .map(HoaxVM::new);
    }


}
