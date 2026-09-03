package com.autoarticle.controller;

import com.autoarticle.dto.ArticleDto;
import com.autoarticle.service.ArticleService;
import com.autoarticle.service.PublishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final PublishService publishService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<ArticleDto> articles = articleService.getArticles(keyword, status, sort, page, size);
        model.addAttribute("articles", articles.getContent());
        model.addAttribute("total", articles.getTotalElements());
        model.addAttribute("totalPages", articles.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        return "articles/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("article", articleService.getArticleById(id));
        model.addAttribute("publishRecords", publishService.getRecordsByArticle(id));
        return "articles/detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("article", articleService.getArticleById(id));
        return "articles/edit";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {
        try {
            articleService.updateArticle(id, title, content, summary, status);
            redirectAttributes.addFlashAttribute("successMessage", "文章已保存");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "保存失败: " + e.getMessage());
        }
        return "redirect:/articles/" + id;
    }
}
