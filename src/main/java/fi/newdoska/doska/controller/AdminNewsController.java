package fi.newdoska.doska.controller;

import fi.newdoska.doska.entity.SiteNews;
import fi.newdoska.doska.repository.SiteNewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminNewsController {

    private final SiteNewsRepository newsRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("newsList", newsRepository.findAllByOrderByCreatedAtDesc());
        return "admin/news";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("newsItem", new SiteNews());
        return "admin/news-form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute SiteNews newsItem, RedirectAttributes ra) {
        newsItem.setCreatedAt(LocalDateTime.now());
        if (newsItem.getPublished() == null) {
            newsItem.setPublished(true);
        }
        newsRepository.save(newsItem);
        ra.addFlashAttribute("success", "Новость создана");
        return "redirect:/admin/news";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("newsItem", newsRepository.findById(id).orElseThrow());
        return "admin/news-form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute SiteNews newsItem, RedirectAttributes ra) {
        SiteNews existing = newsRepository.findById(id).orElseThrow();
        existing.setTitle(newsItem.getTitle());
        existing.setSummary(newsItem.getSummary());
        existing.setContent(newsItem.getContent());
        existing.setPublished(newsItem.getPublished() != null && newsItem.getPublished());
        existing.setUpdatedAt(LocalDateTime.now());
        newsRepository.save(existing);
        ra.addFlashAttribute("success", "Новость обновлена");
        return "redirect:/admin/news";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        newsRepository.deleteById(id);
        ra.addFlashAttribute("success", "Новость удалена");
        return "redirect:/admin/news";
    }
}
