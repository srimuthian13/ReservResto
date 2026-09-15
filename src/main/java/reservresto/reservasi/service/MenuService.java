package reservresto.reservasi.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reservresto.reservasi.models.Menu;
import reservresto.reservasi.repositories.MenuRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;

    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    public List<Menu> getMenusByCategory(String category) {
        return menuRepository.findByCategory(category);
    }

    public Menu getMenuById(Long id) {
        return menuRepository.findById(id).orElse(null);
    }

    public Menu saveMenu(Menu menu) {
        return menuRepository.save(menu);
    }

    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }
}
