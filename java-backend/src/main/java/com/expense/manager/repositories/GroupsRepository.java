package com.expense.manager.repositories;

import com.expense.manager.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GroupsRepository extends JpaRepository<Group, Integer>, JpaSpecificationExecutor<Group> {

}