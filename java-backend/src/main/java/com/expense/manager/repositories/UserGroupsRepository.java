package com.expense.manager.repositories;

import com.expense.manager.entities.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserGroupsRepository extends JpaRepository<UserGroup, Integer>, JpaSpecificationExecutor<UserGroup> {

}