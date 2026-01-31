package com.trako.services;

import com.trako.entities.NlpData;
import com.trako.entities.User;
import com.trako.exceptions.UserNotLoggedInException;
import com.trako.repositories.NlpDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NlpDataService {

    @Autowired
    NlpDataRepository nlpDataRepository;

    @Autowired
    UserService userService;

    public void save(String request, String response) throws UserNotLoggedInException {
        NlpData nlpData = new NlpData();
        User user = userService.loggedInUser();
        nlpData.setUserId(user.getId());
        nlpData.setResponse(response);
        nlpData.setRequest(request);
        nlpDataRepository.save(nlpData);
    }

}
