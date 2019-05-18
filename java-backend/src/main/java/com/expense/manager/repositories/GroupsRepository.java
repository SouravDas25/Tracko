package com.expense.manager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.expense.manager.entities.Groups;

public interface GroupsRepository extends JpaRepository<Groups, Integer>, JpaSpecificationExecutor<Groups> {

}