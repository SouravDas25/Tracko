package com.trako.repositories;

import com.trako.entities.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GroupsRepository extends JpaRepository<ChatGroup, String>, JpaSpecificationExecutor<ChatGroup> {


}