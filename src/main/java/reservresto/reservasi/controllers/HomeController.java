package reservresto.reservasi.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import reservresto.reservasi.service.RoomTypeService;
import reservresto.reservasi.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final RoomTypeService roomTypeService;
    private final MenuService menuService;

@GetMapping("/")
public String showIndex(Model model) {
    model.addAttribute("areas", roomTypeService.getAll());
    model.addAttribute("menus", menuService.getAllMenus());
    return "index"; // Menampilkan landing page untuk tamu
}

@GetMapping("/home")
public String showHome(jakarta.servlet.http.HttpSession session) {
    // PROTEKSI: Jika user belum login tapi nekat ngetik /home di browser
    if (session.getAttribute("userId") == null) {
        return "redirect:/login"; // Tendang balik ke login
    }
    return "home"; // Menampilkan halaman utama user yang sudah login
}

@GetMapping("/menu")
public String showMenu(Model model, jakarta.servlet.http.HttpSession session) {
    if (session.getAttribute("userId") == null) {
        return "redirect:/login"; // Tamu harus login untuk melihat semua menu
    }
    model.addAttribute("menus", menuService.getAllMenus());
    return "menu"; // Menampilkan menu dari database
}
}
