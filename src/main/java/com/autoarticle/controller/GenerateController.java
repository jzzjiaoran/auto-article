package com.autoarticle.controller;

import com.autoarticle.dto.GenerationRequest;
import com.autoarticle.dto.TaskStatus;
import com.autoarticle.service.GenerationService;
import com.autoarticle.service.HotTopicService;
import com.autoarticle.util.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class GenerateController {

    private final GenerationService generationService;
    private final HotTopicService hotTopicService;

    @GetMapping("/generate")
    public String generateForm(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String style,
            @RequestParam(required = false) String length,
            Model model) {
        model.addAttribute("topics", hotTopicService.getTopics(null, null, null, 0, 100).getContent());
        model.addAttribute("selectedTopicId", topicId);
        model.addAttribute("style", style != null ? style : "popular");
        model.addAttribute("length", length != null ? length : "medium");
        return "generate/index";
    }

    @PostMapping("/articles")
    @ResponseBody
    public Result<String> createAndGenerate(@RequestBody GenerationRequest request) {
        try {
            String taskId = generationService.startGeneration(request);
            return Result.ok(taskId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/articles/{id}/generate")
    @ResponseBody
    public Result<String> regenerate(@PathVariable Long id) {
        try {
            String taskId = generationService.regenerateArticle(id);
            return Result.ok(taskId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/tasks/{taskId}")
    @ResponseBody
    public Result<TaskStatus> getTaskStatus(@PathVariable String taskId) {
        try {
            TaskStatus status = generationService.getTaskStatus(taskId);
            return Result.ok(status);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
