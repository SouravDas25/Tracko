package com.trako.controllers;

import com.trako.models.request.SplitSaveRequest;
import com.trako.models.request.SplitSettleRequest;
import com.trako.models.responses.SplitResponse;
import com.trako.models.responses.SplitUserResponse;
import com.trako.services.SplitService;
import com.trako.util.Response;
import org.hibernate.validator.constraints.SafeHtml;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/split")
public class SplitController {

    @Autowired
    SplitService splitService;

    @GetMapping({"", "/"})
    private ResponseEntity<?> index() {
        List<SplitUserResponse> index = splitService.findAllSplits();
        return Response.ok(index);
    }

    @GetMapping("/{userId}")
    private ResponseEntity<?> show(@PathVariable String userId) {
        List<SplitResponse> show = splitService.findAllSplitsByUser(userId);
        return Response.ok(show);
    }

    @PatchMapping("/settle/{splitId}")
    private ResponseEntity<?> settle(@PathVariable String splitId, @SafeHtml @Valid @RequestBody SplitSettleRequest settleRequest) {
        splitService.settleSplit(splitId, settleRequest.getAmount());
        return Response.ok("SUCCESS");
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> save(@Valid @SafeHtml @RequestBody List<SplitSaveRequest> splitSaveRequestList) {
        try {
            splitService.save(splitSaveRequestList);
        } catch (Exception e) {
            return Response.badRequest("User id not found.");
        }
        return Response.ok("SUCCESS");
    }

}
