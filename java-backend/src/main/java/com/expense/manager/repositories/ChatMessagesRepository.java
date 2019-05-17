package com.expense.manager.repositories;

import com.expense.manager.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessagesRepository extends JpaRepository<ChatMessage, Integer>, JpaSpecificationExecutor<ChatMessage> {

    @Query(value = "SELECT * FROM chat_messages cm WHERE cm.group_id = ?1 AND cm.id < ?2  LIMIT 100;", nativeQuery = true)
    List<ChatMessage> retrieveMessages(Integer groupId, Integer id);


    @Query(value = "SELECT * FROM chat_messages cm WHERE cm.group_id = ?1 ORDER BY created_at ASC LIMIT 100;", nativeQuery = true)
    List<ChatMessage> retrieveMessagesLatest(Integer groupId);
}