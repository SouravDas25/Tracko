package com.expense.manager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.expense.manager.entities.UserGroups;

public interface UserGroupsRepository extends JpaRepository<UserGroups, Integer>, JpaSpecificationExecutor<UserGroups> {

}