package com.trako.repositories;

import com.trako.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {
    User findByPhoneNo(String phoneNumber);
    User findByEmail(String email);
    User findByName(String name);
}