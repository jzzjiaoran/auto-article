package com.autoarticle.controller;

import com.autoarticle.service.HotTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hot-topics")
@RequiredArgsConstructor
public class HotTopicController {

    private final HotTopicService hotTopicService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var topics = hotTopicService.getTopics(keyword, source, status, page, size);
        model.addAttribute("topics", topics.getContent());
        model.addAttribute("total", topics.getTotalElements());
        model.addAttribute("totalPages", topics.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("source", source);
        model.addAttribute("status", status);
        return "hot-topics/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("topic", hotTopicService.getTopicById(id));
        return "hot-topics/detail";
    }

    @PostMapping("/{id}/refresh")
    public String refresh(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            hotTopicService.refreshTopic(id);
            redirectAttributes.addFlashAttribute("successMessage", "热点已刷新");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "刷新失败: " + e.getMessage());
        }
        return "redirect:/hot-topics";
    }
}
