package com.trako.repositories;

import com.trako.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessagesRepository extends JpaRepository<ChatMessage, String>, JpaSpecificationExecutor<ChatMessage> {

    @Query(value = "SELECT cm FROM ChatMessage cm WHERE cm.groupId = ?1 AND cm.id < ?2")
    List<ChatMessage> retrieveMessages(String groupId, String id);


    @Query(value = "SELECT cm FROM ChatMessage cm WHERE cm.groupId = ?1  ORDER BY cm.createdAt ASC")
    List<ChatMessage> retrieveMessagesLatest(String groupId);
}