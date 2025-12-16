package io.github.jihyundev.spring_batch_etl.controller.batch;

import io.github.jihyundev.spring_batch_etl.service.BatchRetryService;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch")
public class BatchController {
    private final BatchRetryService batchRetryService;

    @GetMapping("/retry-requests/new")
    public ResponseEntity<Map> retryRequests(@RequestParam Long jobExecutionId){
        boolean retry = batchRetryService.retrySyncJob(jobExecutionId);
        Map<String, Object> result = new HashMap<>();
        result.put("retryResult", retry);
        return ResponseEntity.ok(result);
    }

}
