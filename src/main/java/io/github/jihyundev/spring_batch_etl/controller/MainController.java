package io.github.jihyundev.spring_batch_etl.controller;

import io.github.jihyundev.spring_batch_etl.dto.request.BatchExecutionCondition;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionKpi;
import io.github.jihyundev.spring_batch_etl.dto.response.BatchExecutionSummaryDto;
import io.github.jihyundev.spring_batch_etl.service.BatchExecutionService;
import io.github.jihyundev.spring_batch_etl.util.PageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MainController {
    private final BatchExecutionService batchExecutionService;

    @RequestMapping("/")
    public String  index(@ModelAttribute("condition") BatchExecutionCondition condition, Model model) {
        int total = batchExecutionService.countSummaries(condition);
        List<BatchExecutionSummaryDto> summaries = batchExecutionService.findSummaries(condition);

        BatchExecutionKpi kpi = batchExecutionService.getKpi(condition);

        model.addAttribute("summaries", summaries);
        model.addAttribute("page", new PageModel(condition.getPage(), condition.getSize(), total));
        model.addAttribute("kpi", kpi);
        return "main/index";
    }
}
