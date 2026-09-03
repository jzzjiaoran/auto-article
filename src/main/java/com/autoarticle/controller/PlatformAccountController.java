package com.autoarticle.controller;

import com.autoarticle.service.PlatformAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/platform-accounts")
@RequiredArgsConstructor
public class PlatformAccountController {

    private final PlatformAccountService platformAccountService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", platformAccountService.getAllAccounts());
        return "accounts/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("account", Map.of());
        model.addAttribute("credentials", Map.of());
        return "accounts/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("account", platformAccountService.getAccountById(id));
        return "accounts/form";
    }

    @PostMapping
    public String create(
            @RequestParam String name,
            @RequestParam String platform,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String appSecret,
            @RequestParam(required = false) String cookie,
            @RequestParam(required = false) String apiToken,
            RedirectAttributes redirectAttributes) {
        try {
            Map<String, String> credentials = buildCredentials(platform, appId, appSecret, cookie, apiToken);
            platformAccountService.createAccount(name, platform, credentials);
            redirectAttributes.addFlashAttribute("successMessage", "账号创建成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "创建失败: " + e.getMessage());
        }
        return "redirect:/platform-accounts";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String platform,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String appSecret,
            @RequestParam(required = false) String cookie,
            @RequestParam(required = false) String apiToken,
            @RequestParam(required = false) String _method,
            RedirectAttributes redirectAttributes) {
        if ("delete".equals(_method)) {
            return delete(id, redirectAttributes);
        }
        try {
            Map<String, String> credentials = buildCredentials(platform, appId, appSecret, cookie, apiToken);
            platformAccountService.updateAccount(id, name, platform, credentials, true);
            redirectAttributes.addFlashAttribute("successMessage", "账号更新成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新失败: " + e.getMessage());
        }
        return "redirect:/platform-accounts";
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            platformAccountService.verifyAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "账号验证成功");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "验证失败: " + e.getMessage());
        }
        return "redirect:/platform-accounts";
    }

    private String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            platformAccountService.deleteAccount(id);
            redirectAttributes.addFlashAttribute("successMessage", "账号已删除");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败: " + e.getMessage());
        }
        return "redirect:/platform-accounts";
    }

    private Map<String, String> buildCredentials(String platform, String appId, String appSecret,
                                                  String cookie, String apiToken) {
        return switch (platform) {
            case "gzh" -> Map.of("appId", appId != null ? appId : "", "appSecret", appSecret != null ? appSecret : "");
            case "xiaohongshu" -> Map.of("cookie", cookie != null ? cookie : "");
            case "zhihu", "toutiao" -> Map.of("apiToken", apiToken != null ? apiToken : "");
            default -> Map.of();
        };
    }
}
