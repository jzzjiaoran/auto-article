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

    /**
     * 手动触发热点采集入口（对应列表页「立即采集」按钮）。
     */
    @PostMapping("/collect")
    public String collect(RedirectAttributes redirectAttributes) {
        try {
            int added = hotTopicService.collectHotTopics();
            if (added < 0) {
                redirectAttributes.addFlashAttribute("successMessage", "有采集/种子任务正在执行，本次已跳过");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "采集完成，新增 " + added + " 条");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "采集失败: " + e.getMessage());
        }
        return "redirect:/hot-topics";
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
