package com.trako.services;

import com.trako.entities.Split;
import com.trako.entities.User;
import com.trako.helpers.CommonHelper;
import com.trako.models.request.SplitSaveRequest;
import com.trako.models.responses.SplitResponse;
import com.trako.models.responses.SplitUserResponse;
import com.trako.repositories.SplitRepository;
import com.trako.repositories.UsersRepository;
import com.trako.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Component
public class SplitService {

    @Autowired
    SplitRepository splitRepository;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    CommonHelper commonHelper;

    public List<SplitUserResponse> findAllSplits() {
        User source = commonHelper.loggedInUser();
        List<String> dueUserIds = splitRepository.findAllDueUserId(source.getId());
        List<SplitUserResponse> users = new ArrayList<>();
        for (String dueUserId : dueUserIds) {
            User dueUser = usersRepository.findOne(dueUserId);
            if (dueUser == null) {
                continue;
            }
            SplitUserResponse splitUserResponse = CommonUtil.mapModel(dueUser, SplitUserResponse.class);
            List<SplitResponse> splits = findAllSplitsByUser(dueUserId);
            splitUserResponse.setSplits(splits);
            users.add(splitUserResponse);
        }
        return users;
    }

    public List<SplitResponse> findAllSplitsByUser(String dueUserId) {
        User source = commonHelper.loggedInUser();
        List<Split> splits = splitRepository.findByDueUserIdAndSourceUserId(dueUserId, source.getId());
        return CommonUtil.mapModels(splits, SplitResponse.class);
    }

    @Transactional
    public void save(List<SplitSaveRequest> splitSaveRequestList) throws Exception {
        Boolean b = true;
        for (SplitSaveRequest splitSaveRequest : splitSaveRequestList) {
            Split split = this.save(splitSaveRequest);
            if (split == null)
                throw new Exception("Split cannot be saved");
        }
    }

    @Transactional
    public void settleSplit(String splitId, Double amount) {
        splitRepository.settleSplit(splitId, amount);
    }

    public Split save(SplitSaveRequest splitSaveRequest) {
        Split split = CommonUtil.mapModel(splitSaveRequest, Split.class);
        User dueUser = usersRepository.findOne(split.getDueUserId());
        User sourceUser = commonHelper.loggedInUser();
        split.setSourceUser(sourceUser);
        split.setSourceUserId(sourceUser.getId());
        if (dueUser == null || sourceUser == null)
            return null;
        split = splitRepository.save(split);
        return split;
    }


}
