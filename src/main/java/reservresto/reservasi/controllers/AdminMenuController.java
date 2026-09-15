package reservresto.reservasi.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reservresto.reservasi.models.Menu;
import reservresto.reservasi.service.MenuService;

import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuService menuService;

    @GetMapping
    public String listMenus(Model model) {
        model.addAttribute("menus", menuService.getAllMenus());
        return "admin/menu-list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("menu", new Menu());
        return "admin/menu-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Menu menu = menuService.getMenuById(id);
        if (menu == null) {
            return "redirect:/admin/menus";
        }
        model.addAttribute("menu", menu);
        return "admin/menu-form";
    }

    @PostMapping("/save")
    public String saveMenu(@Valid @ModelAttribute("menu") Menu menu,
                           BindingResult bindingResult,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            return "admin/menu-form";
        }

        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String rootDir = System.getProperty("user.dir");
                Path uploadPath = Paths.get(rootDir, "uploads", "menus");

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String originalFileName = imageFile.getOriginalFilename();
                String cleanFileName = originalFileName.replaceAll("[^a-zA-Z0-9.]", "_");
                String filename = "menu_" + cleanFileName;

                Path target = uploadPath.resolve(filename);
                imageFile.transferTo(target.toFile());
                menu.setImagePath(filename);
            } else if (menu.getId() != null) {
                Menu existing = menuService.getMenuById(menu.getId());
                if (existing != null) {
                    menu.setImagePath(existing.getImagePath());
                }
            }

            menuService.saveMenu(menu);
            ra.addFlashAttribute("message", "Menu berhasil disimpan!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Gagal menyimpan menu: " + e.getMessage());
            return "redirect:/admin/menus/add";
        }

        return "redirect:/admin/menus";
    }

    @GetMapping("/delete/{id}")
    public String deleteMenu(@PathVariable Long id, RedirectAttributes ra) {
        try {
            menuService.deleteMenu(id);
            ra.addFlashAttribute("message", "Menu berhasil dihapus!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Gagal menghapus menu: " + e.getMessage());
        }
        return "redirect:/admin/menus";
    }
}
