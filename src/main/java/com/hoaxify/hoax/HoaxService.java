package com.hoaxify.hoax;

import com.hoaxify.user.User;
import com.hoaxify.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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
        return hoaxRepository.findByUser(inDB, pageable);
    }

    public Page<Hoax> getOldHoaxes(long id, String username, Pageable pageable) {
        Specification<Hoax> spec = Specification.where(idLessThan(id));
        if (username != null){
            User inDB = userService.getByUsername(username);
            spec = spec.and(userIs(inDB));
        }
        return hoaxRepository.findAll(spec, pageable);
    }


    public List<Hoax> getNewHoaxes(long id, String username, Pageable pageable) {
        Specification<Hoax> spec = Specification.where(idGreaterThan(id));
        if (username != null){
            User inDB = userService.getByUsername(username);
            spec = spec.and(userIs(inDB));
        }
        return hoaxRepository.findAll(spec, pageable.getSort());
    }

    public long getNewHoaxesCount(long id, String username) {
        Specification<Hoax> spec = Specification.where(idGreaterThan(id));
        if (username != null){
            User inDB = userService.getByUsername(username);
            spec = spec.and(userIs(inDB));
        }
        return hoaxRepository.count(spec);
    }


    //METHODS SPECIFICATIONS***********************************************************
    /********************************************************************************/

    private Specification<Hoax> userIs(User user){
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user"), user);
    }

    private Specification<Hoax> idLessThan(long id){
        return ((root, query, criteriaBuilder) -> criteriaBuilder.lessThan(root.get("id"), id));
    }

    private Specification<Hoax> idGreaterThan(long id){
        return ((root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("id"), id));
    }
}
