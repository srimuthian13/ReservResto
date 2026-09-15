package reservresto.reservasi.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import reservresto.reservasi.models.RoomType;
import reservresto.reservasi.models.TableRes;
import reservresto.reservasi.service.RoomTypeService;
import reservresto.reservasi.service.TableService;
import reservresto.reservasi.service.ReservasiService;
import reservresto.reservasi.service.UserService;
import reservresto.reservasi.models.User;
import org.springframework.beans.factory.annotation.Value;
import reservresto.reservasi.service.PaymentService;

@Controller
@RequiredArgsConstructor
public class AdminTableController {

    private final TableService tableService;
    private final ReservasiService reservasiService;
    private final reservresto.reservasi.service.RoomTypeService roomTypeService;
    private final UserService userService;
    private final PaymentService paymentService;

    @Value("${file.upload-dir:uploads/roomtypes}")
    private String uploadDir;

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        // show all active reservations
        java.util.List<reservresto.reservasi.models.Reservasi> list = reservasiService.getAllReservasi();
        model.addAttribute("reservasiList", list);
        return "dashboard";
    }

    // CHECK-IN
    @GetMapping("/admin/reservasi/checkin/{id}")
    public String checkInReservasi(@PathVariable Long id, RedirectAttributes ra) {
        reservasiService.updateStatus(id, "CHECKED_IN");
        ra.addFlashAttribute("message", "Customer berhasil check-in.");
        return "redirect:/admin/dashboard"; 
    }

    // NO-SHOW
    @GetMapping("/admin/reservasi/noshow/{id}")
    public String noShowReservasi(@PathVariable Long id, RedirectAttributes ra) {
        reservasiService.updateStatus(id, "NO_SHOW");
        ra.addFlashAttribute("message", "Status diubah menjadi No-show.");
        return "redirect:/admin/dashboard"; 
    }

    // SELESAI / COMPLETE RESERVASI (kembalikan kapasitas meja)
    @GetMapping("/admin/reservasi/complete/{id}")
    public String completeReservasi(@PathVariable Long id, RedirectAttributes ra) {
        reservasiService.completeReservasi(id);
        ra.addFlashAttribute("message", "Reservasi selesai.");
        return "redirect:/admin/dashboard";
    }

    // VERIFY PAYMENT
    @PostMapping("/admin/reservasi/verify-payment")
    public String verifyPayment(@RequestParam Long paymentId, 
                                @RequestParam boolean isApproved, 
                                @RequestParam(required = false) String adminNote, 
                                RedirectAttributes ra) {
        try {
            paymentService.verifyPayment(paymentId, isApproved, adminNote);
            ra.addFlashAttribute("message", "Pembayaran berhasil diverifikasi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/reservasi";
    }

    // CANCEL RESERVASI
    @GetMapping("/admin/reservasi/cancel/{id}")
    public String cancelReservasi(@PathVariable Long id) {
        reservasiService.adminCancelReservasi(id);
        return "redirect:/admin/dashboard";
    }

    // DELETE RESERVASI (permanently) - only for unpaid reservations
    @GetMapping("/admin/reservasi/delete/{id}")
    public String deleteReservasi(@PathVariable Long id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            reservasiService.deleteReservasi(id);
            ra.addFlashAttribute("message", "Reservasi berhasil dihapus.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Gagal menghapus reservasi: " + e.getMessage());
        }
        return "redirect:/admin/reservasi";
    }

    // POST cancel dari form di admin templates
    @PostMapping("/admin/reservasi/cancel")
    public String cancelReservasiPost(@RequestParam Long id) {
        reservasiService.adminCancelReservasi(id);
        return "redirect:/admin/reservasi";
    }

    // LIST reservasi untuk admin (berlaku di halaman khusus)
    @GetMapping("/admin/reservasi")
    public String listReservasi(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "dir", required = false) String dir,
            Model model) {

        java.util.List<reservresto.reservasi.models.Reservasi> list = reservasiService.getAllReservasi();

        // filter by user id (q) when provided
        if (q != null && !q.isEmpty()) {
            try {
                Long userId = Long.valueOf(q);
                User u = userService.getById(userId);
                list = reservasiService.getReservasiByUser(u);
            } catch (Exception ex) {
                // ignore and keep full list
            }
        }

        // search by user name or email
        if (search != null && !search.trim().isEmpty()) {
            String s = search.trim().toLowerCase();
            list = list.stream()
                    .filter(r -> r.getUser() != null
                            && ((r.getUser().getName() != null && r.getUser().getName().toLowerCase().contains(s)) ||
                                    (r.getUser().getEmail() != null
                                            && r.getUser().getEmail().toLowerCase().contains(s))))
                    .toList();
        }

        // sorting
        String direction = (dir == null || (!dir.equalsIgnoreCase("asc") && !dir.equalsIgnoreCase("desc"))) ? "asc"
                : dir.toLowerCase();
        if (sort != null && !sort.isEmpty()) {
            java.util.Comparator<reservresto.reservasi.models.Reservasi> cmp;
            switch (sort) {
                case "name":
                    cmp = java.util.Comparator.comparing(
                            r -> r.getUser() == null || r.getUser().getName() == null ? "" : r.getUser().getName());
                    break;
                case "id":
                    cmp = java.util.Comparator
                            .comparing(r -> r.getIdReservasi() == null ? Long.valueOf(0L) : r.getIdReservasi());
                    break;
                case "time":
                default:
                    cmp = java.util.Comparator.comparing(
                            r -> r.getReservasiTime() == null ? java.time.LocalDateTime.MIN : r.getReservasiTime());
                    break;
            }
            if ("desc".equals(direction))
                cmp = cmp.reversed();
            list = list.stream().sorted(cmp).toList();
        }

        model.addAttribute("reservasiList", list);
        model.addAttribute("q", q);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("sort", sort == null ? "time" : sort);
        model.addAttribute("dir", direction);

        return "admin/reservasi-list";
    }

    // LIST users (recent users / users with reservations)
    @GetMapping("/admin/users")
    public String listUsers(@RequestParam(value = "q", required = false) String q, Model model) {
        java.util.List<User> all = userService.getAllUsers();
        java.util.List<User> filtered = new java.util.ArrayList<>();
        if (all != null) {
            for (User u : all) {
                if (u != null && u.getReservasis() != null && !u.getReservasis().isEmpty()) {
                    filtered.add(u);
                }
            }
        }

        if (q != null && !q.trim().isEmpty()) {
            String s = q.trim().toLowerCase();
            filtered = filtered.stream()
                    .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(s))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(s)))
                    .toList();
        }

        model.addAttribute("users", filtered);
        model.addAttribute("q", q == null ? "" : q);
        return "admin/recent-users";
    }

    // Admin manual reservation creation removed per user request.

    // DETAIL reservasi untuk admin
    @GetMapping("/admin/reservasi/{id}")
    public String detailReservasi(@PathVariable Long id, Model model) {
        model.addAttribute("reservasi", reservasiService.getReservasiById(id));
        return "admin/reservasi-detail";
    }

    // READ: Menampilkan semua meja
    @GetMapping("/admin/tables")
    public String listTables(@RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "dir", required = false) String dir,
            Model model) {
        java.util.List<reservresto.reservasi.models.TableRes> tables;
        if (q != null && !q.isEmpty()) {
            tables = tableService.searchTables(q);
        } else if (sort != null && !sort.isEmpty()) {
            tables = tableService.getAllSorted(sort, dir == null ? "asc" : dir);
        } else {
            tables = tableService.getAllTables();
        }

        model.addAttribute("tables", tables);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort == null ? "" : sort);
        model.addAttribute("dir", dir == null ? "asc" : dir);
        return "admin/table-list";
    }

    // CREATE: Menampilkan form tambah meja
    @GetMapping("/admin/tables/add")
    public String showAddForm(Model model) {
        model.addAttribute("table", new TableRes());
        model.addAttribute("roomTypes", roomTypeService.getAll());
        return "admin/table-form";
    }

    // UPDATE: Menampilkan form edit meja
    @GetMapping("/admin/tables/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("table", tableService.getTableById(id));
        model.addAttribute("roomTypes", roomTypeService.getAll());
        return "admin/table-form";
    }

    // SAVE (Create/Update): Menyimpan data dari form
    @PostMapping("/admin/tables/save")
    public String saveTable(@Valid @ModelAttribute("table") TableRes table,
            BindingResult bindingResult,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roomTypes", roomTypeService.getAll());
            return "admin/table-form";
        }
        try {
            tableService.saveTable(table);
            ra.addFlashAttribute("message", "Meja berhasil disimpan.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/tables/add";
        }
        return "redirect:/admin/tables";
    }

    // DELETE: Menghapus meja
    @GetMapping("/admin/tables/delete/{id}")
    public String deleteTable(@PathVariable Long id) {
        tableService.deleteTable(id);
        return "redirect:/admin/tables";
    }

    // ROOM TYPE management
    @GetMapping("/admin/roomtypes")
    public String listRoomTypes(Model model) {
        model.addAttribute("roomTypes", roomTypeService.getAll());
        return "admin/roomtype-list";
    }

    @GetMapping("/admin/roomtypes/add")
    public String showAddRoomType(Model model) {
        model.addAttribute("roomType", new reservresto.reservasi.models.RoomType());
        return "admin/roomtype-form";
    }

    @GetMapping("/admin/roomtypes/edit/{id}")
    public String showEditRoomType(@PathVariable Long id, Model model) {
        model.addAttribute("roomType", roomTypeService.getById(id));
        return "admin/roomtype-form";
    }

    @GetMapping("/admin/roomtypes/delete/{id}")
    public String deleteRoomType(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            roomTypeService.delete(id);
            ra.addFlashAttribute("message", "Data Room Type berhasil dihapus!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Gagal menghapus data: " + e.getMessage());
        }
        return "redirect:/admin/roomtypes";
    }

    @PostMapping("/admin/roomtypes/save")
    public String saveRoomType(@ModelAttribute("roomType") RoomType roomType,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile,
            RedirectAttributes ra) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String rootDir = System.getProperty("user.dir");
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(rootDir, "uploads", "roomtypes");

                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }

                String originalFileName = imageFile.getOriginalFilename();

                String cleanFileName = originalFileName.replaceAll("[^a-zA-Z0-9.]", "_");

                // (Opsional) Tambahkan sedikit prefix singkat agar unik tapi tetap terbaca
                String filename = "room_" + cleanFileName;

                java.nio.file.Path target = uploadPath.resolve(filename);

                imageFile.transferTo(target.toFile());
                roomType.setImagePath(filename);
            } // 2. Logika Menjaga Gambar Lama (Jika sedang Edit dan tidak upload gambar baru)
            else if (roomType.getId() != null) {
                RoomType existing = roomTypeService.getById(roomType.getId());
                if (existing != null) {
                    roomType.setImagePath(existing.getImagePath());
                }
            }

            roomTypeService.save(roomType);
            ra.addFlashAttribute("message", "Data Berhasil Disimpan!");
            return "redirect:/admin/roomtypes";

        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Gagal menyimpan data: " + e.getMessage());
            return "redirect:/admin/roomtypes/add";
        }
    }
}