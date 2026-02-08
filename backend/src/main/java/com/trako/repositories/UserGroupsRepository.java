package com.trako.repositories;

import com.trako.entities.UserChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UserGroupsRepository extends JpaRepository<UserChatGroup, String>, JpaSpecificationExecutor<UserChatGroup> {

    List<UserChatGroup> findByUserId(String userId);

    List<UserChatGroup> findByGroupId(String groupId);
}