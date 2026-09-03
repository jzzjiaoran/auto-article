package com.autoarticle.controller;

import com.autoarticle.dto.PublishRequest;
import com.autoarticle.service.ArticleService;
import com.autoarticle.service.PlatformAccountService;
import com.autoarticle.service.PublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/publish-records")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;
    private final ArticleService articleService;
    private final PlatformAccountService platformAccountService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        model.addAttribute("publishableArticles", articleService.getArticles(null, null, "updatedAt", 0, 100).getContent());
        model.addAttribute("accounts", platformAccountService.getAllAccounts());
        var records = publishService.getRecords(page, size);
        model.addAttribute("records", records.getContent());
        model.addAttribute("total", records.getTotalElements());
        model.addAttribute("totalPages", records.getTotalPages());
        model.addAttribute("currentPage", page);
        return "publish/index";
    }

    @PostMapping
    public String publish(@ModelAttribute PublishRequest request, RedirectAttributes redirectAttributes) {
        try {
            publishService.publish(request);
            redirectAttributes.addFlashAttribute("successMessage", "发布任务已提交");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "发布失败: " + e.getMessage());
        }
        return "redirect:/publish-records";
    }

    @PostMapping("/{id}/retry")
    public String retry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            publishService.retryPublish(id);
            redirectAttributes.addFlashAttribute("successMessage", "重试已提交");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "重试失败: " + e.getMessage());
        }
        return "redirect:/publish-records";
    }
}
