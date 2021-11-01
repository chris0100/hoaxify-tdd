package com.hoaxify.hoax;

import com.hoaxify.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoaxRepository extends JpaRepository<Hoax, Long> {

    Page<Hoax> findByUser(User user, Pageable pageable);

}
