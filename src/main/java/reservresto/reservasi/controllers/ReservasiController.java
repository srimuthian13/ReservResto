package reservresto.reservasi.controllers;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import reservresto.reservasi.models.Reservasi;
import reservresto.reservasi.service.ReservasiService;
import reservresto.reservasi.service.UserService;

import lombok.RequiredArgsConstructor;
import reservresto.reservasi.constan.Role;
import reservresto.reservasi.models.User;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.List;
import reservresto.reservasi.models.ReservasiDetail;
import reservresto.reservasi.models.TableRes;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class ReservasiController {
    private final ReservasiService reservasiService;
    private final UserService userService;
    private final reservresto.reservasi.service.TableService tableService;
    private final reservresto.reservasi.service.PaymentService paymentService;
    private final reservresto.reservasi.service.RoomTypeService roomTypeService;
    private final reservresto.reservasi.service.MenuService menuService;
    private final reservresto.reservasi.repositories.ReservasiRepository reservasiRepository;

    @GetMapping("/reservasi")
    public String reservasiPage(Model model, jakarta.servlet.http.HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("tables", tableService.getAllTables());
        model.addAttribute("roomTypes", roomTypeService.getAll());
        model.addAttribute("menus", menuService.getAllMenus());
        return "reservasi"; // => templates/reservasi.html
    }

    @PostMapping("/create")
    public ResponseEntity<?> createReservasi(@RequestParam Long userId, @RequestParam Long tableId,
            @RequestParam String reservasiTime,
            @RequestParam(required = false, defaultValue = "30") int durationMinutes,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false, defaultValue = "1") int roomQty) {
        try {
            User user = userService.getById(userId);

            LocalDateTime time;
            try {
                time = LocalDateTime.parse(reservasiTime);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Format waktu tidak valid. Gunakan input datetime-local.");
            }

            // 🔐 VALIDASI ROLE
            if (user.getRole() != Role.USER) {
                return ResponseEntity.status(403)
                        .body("Hanya USER yang boleh membuat reservasi");
            }

                Reservasi reservasi = reservasiService.createReservasi(
                    userId, tableId, time, durationMinutes, 1, "API User", "-", "-");

            if (roomTypeId != null) {
                reservasiService.addDetail(reservasi.getIdReservasi(), roomTypeId, roomQty);
            }

            return ResponseEntity.ok(reservasi);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Terjadi kesalahan server");
        }
    }

    // Endpoint khusus untuk form HTML — akan redirect ke daftar reservasi user
    @PostMapping("/create/form")
    public String createReservasiForm(@RequestParam(required = false) Long userId, @RequestParam Long tableId,
            @RequestParam String reservasiTime,
            @RequestParam(required = false, defaultValue = "30") int durationMinutes,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false, defaultValue = "1") int roomQty,
            @RequestParam int totalGuest,
            @RequestParam String customerName,
            @RequestParam String customerPhone,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) List<Long> menuIds,
            @RequestParam(required = false) List<Integer> menuQtys,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpSession session) {
        try {
            Long uid = userId;
            if (uid == null) {
                Object sid = session.getAttribute("userId");
                if (sid == null) {
                    redirectAttributes.addFlashAttribute("error", "Anda harus login terlebih dahulu.");
                    return "redirect:/login";
                }
                uid = Long.valueOf(sid.toString());
            }

            User user = userService.getById(uid);

            LocalDateTime time = LocalDateTime.parse(reservasiTime);

            Reservasi saved = reservasiService.createReservasi(uid, tableId, time, durationMinutes, totalGuest, customerName, customerPhone, notes);
            
            // Remove roomTypeId logic if it's no longer mandatory or handled, but we keep it just in case:
            if (roomTypeId != null) {
                reservasiService.addDetail(saved.getIdReservasi(), roomTypeId, roomQty);
            }
            
            if (menuIds != null && !menuIds.isEmpty()) {
                for (int i = 0; i < menuIds.size(); i++) {
                    int qty = (menuQtys != null && menuQtys.size() > i) ? menuQtys.get(i) : 1;
                    reservasiService.addMenuDetail(saved.getIdReservasi(), menuIds.get(i), qty);
                }
            }

            redirectAttributes.addFlashAttribute("message", "Reservasi berhasil dibuat. Silakan upload bukti pembayaran.");
            return "redirect:/user/" + uid + "/reservasi/" + saved.getIdReservasi();

        } catch (IllegalArgumentException | DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            Long uid = userId;
            if (uid == null) {
                Object sid = session.getAttribute("userId");
                uid = (sid == null) ? null : Long.valueOf(sid.toString());
            }
            return (uid != null) ? "redirect:/user/" + uid + "/reservasi" : "redirect:/login";
        } catch (Exception e) {
e.printStackTrace(); 
    
    // Kirim pesan error asli ke UI
    redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());            Long uid = userId;
            if (uid == null) {
                Object sid = session.getAttribute("userId");
                uid = (sid == null) ? null : Long.valueOf(sid.toString());
            }
            return (uid != null) ? "redirect:/user/" + uid + "/reservasi" : "redirect:/login";
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllReservasi(@RequestParam Long adminId) {

        User admin = userService.getById(adminId);

        if (admin.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403)
                    .body("Akses ditolak. Khusus ADMIN");
        }

        return ResponseEntity.ok(reservasiService.getAllReservasi());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getReservasiByUser(@PathVariable Long userId) {
        reservresto.reservasi.models.User u = userService.getById(userId);
        return ResponseEntity.ok(reservasiService.getReservasiByUser(u));
    }

    // Halaman web: daftar reservasi milik user
    @GetMapping("/user/{userId}/reservasi")
    public String userReservasiPage(@PathVariable Long userId, Model model) {
        reservresto.reservasi.models.User user = userService.getById(userId);

        model.addAttribute("reservasiList", reservasiService.getReservasiByUser(user));
        model.addAttribute("user", user);
        return "user/profile";
    }

    // DETAIL reservasi untuk user (lihat dari history/daftar)
    @GetMapping("/user/{userId}/reservasi/{id}")
    public String userDetailReservasi(@PathVariable Long userId, @PathVariable Long id, Model model) {
        reservresto.reservasi.models.User user = userService.getById(userId);
        model.addAttribute("user", user);
        Reservasi r = reservasiService.getReservasiById(id);
        int total = 0;
        if (r.getTable() != null && r.getTable().getRoomType() != null && r.getTable().getRoomType().getPrice() != null) {
            total += r.getTable().getRoomType().getPrice();
        }
        if (r.getDetails() != null) {
            for (var d : r.getDetails()) {
                if (d != null) total += d.getSubtotal();
            }
        }
        model.addAttribute("reservasi", r);
        model.addAttribute("total", total);
        return "user/reservasi-detail";
    }

    @PostMapping("/user/reservasi/cancel")
    public String userCancelReservasi(@RequestParam Long id, @RequestParam Long userId, RedirectAttributes ra) {
        try {
            reservasiService.batalkanReservasi(id);
            ra.addFlashAttribute("message", "Reservasi berhasil dibatalkan.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/user/" + userId + "/reservasi";
    }

    @GetMapping("/user/{userId}/reservasi/{id}/hapus")
    public String userCancelReservasiGet(@PathVariable Long id, @PathVariable Long userId, RedirectAttributes ra, HttpSession session) {
        try {
            Object sid = session.getAttribute("userId");
            if (sid == null || !Long.valueOf(sid.toString()).equals(userId)) {
                ra.addFlashAttribute("error", "Akses ditolak");
                return "redirect:/login";
            }

            reservasiService.batalkanReservasi(id);
            ra.addFlashAttribute("message", "Reservasi berhasil dibatalkan.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Gagal membatalkan reservasi: " + e.getMessage());
        }
        return "redirect:/user/" + userId + "/reservasi";
    }

    // SHOW payment form for a reservation
    @GetMapping("/user/{userId}/reservasi/{id}/bayar")
    public String showPaymentForm(@PathVariable Long userId, @PathVariable Long id, Model model) {
        // payment form removed — redirect back to reservation detail where payment button exists
        return "redirect:/user/" + userId + "/reservasi/" + id;
    }

    // Upload payment proof
    @PostMapping("/user/{userId}/reservasi/{id}/upload-payment")
    public String uploadPaymentProof(@PathVariable Long userId, @PathVariable Long id,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam("paymentMethod") String paymentMethod,
                                     RedirectAttributes ra) {
        try {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Silakan pilih file bukti pembayaran.");
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/jpg"))) {
                throw new IllegalArgumentException("Hanya file JPG, JPEG, dan PNG yang diperbolehkan.");
            }
            
            // Check file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Ukuran file maksimal 5 MB.");
            }

            // Save file
            String uploadDir = "./uploads";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".jpg";
            String newFileName = "payment_" + id + "_" + System.currentTimeMillis() + fileExtension;
            Path filePath = uploadPath.resolve(newFileName);
            
            Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Update payment record
            paymentService.savePaymentProof(id, newFileName, paymentMethod);
            
            ra.addFlashAttribute("message", "Terima kasih. Bukti pembayaran berhasil dikirim dan sedang menunggu verifikasi admin.");
            return "redirect:/user/" + userId + "/reservasi/" + id;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/" + userId + "/reservasi/" + id;
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Gagal menyimpan file bukti pembayaran.");
            return "redirect:/user/" + userId + "/reservasi/" + id;
        }
    }
    
    // API for AJAX Table Availability
    @GetMapping("/api/tables/available")
    @ResponseBody
    public ResponseEntity<?> getAvailableTablesAPI(
            @RequestParam String reservasiTime,
            @RequestParam(defaultValue = "60") int durationMinutes,
            @RequestParam int guestCount) {
        try {
            LocalDateTime time = LocalDateTime.parse(reservasiTime);
            List<TableRes> available = tableService.getAvailableTables(time, durationMinutes, guestCount, reservasiRepository);
            return ResponseEntity.ok(available);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Top-up endpoints separated so page can be reused elsewhere
    @GetMapping("/user/{userId}/topup")
    public String showTopupPage(@PathVariable Long userId, @RequestParam(required = false) Long reservasiId, Model model) {
        reservresto.reservasi.models.User user = userService.getById(userId);
        model.addAttribute("user", user);
        if (reservasiId != null) model.addAttribute("reservasiId", reservasiId);
        return "user/topup";
    }

    @PostMapping("/user/{userId}/topup")
    public String doTopup(@PathVariable Long userId, @RequestParam int amount, @RequestParam(required = false) Long reservasiId, RedirectAttributes ra) {
        try {
            userService.topUp(userId, amount);
            ra.addFlashAttribute("message", "Top-up berhasil.");

            // If this top-up was triggered to pay a reservation, attempt payment now
            if (reservasiId != null) {
                // Manual payment doesn't use balance automatically, so we just redirect
                return "redirect:/user/" + userId + "/reservasi/" + reservasiId;
            }

            return "redirect:/user/" + userId + "/topup";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/" + userId + "/topup" + (reservasiId != null ? ("?reservasiId=" + reservasiId) : "");
        }
    }

    // Halaman web: riwayat reservasi yang sudah dibayar oleh user
    @GetMapping("/user/{userId}/reservasi/history")
    public String userReservasiHistoryPage(@PathVariable Long userId, Model model) {
        reservresto.reservasi.models.User user = userService.getById(userId);

        List<Reservasi> all = reservasiService.getReservasiByUser(user);
        // filter hanya yang sudah bayar
        List<Reservasi> paid = all.stream()
                .filter(r -> r.getPaymentStatus() != null && r.getPaymentStatus().equals("PAID"))
                .collect(Collectors.toList());

        Map<Long, Integer> totals = new HashMap<>();
        for (Reservasi r : paid) {
            int total = 0;
            if (r.getDetails() != null) {
                for (ReservasiDetail d : r.getDetails()) {
                    if (d != null && d.getSubtotal() != 0) total += d.getSubtotal();
                }
            }
            totals.put(r.getIdReservasi(), total);
        }

        model.addAttribute("reservasiList", paid);
        model.addAttribute("totals", totals);
        model.addAttribute("user", user);
        return "user/reservasi-history";
    }

    @PostMapping("/user/{userId}/reservasi/history/delete")
    public String deleteUserReservasiHistory(@PathVariable Long userId, RedirectAttributes ra, HttpSession session) {
        try {
            Object sid = session.getAttribute("userId");
            if (sid == null || !Long.valueOf(sid.toString()).equals(userId)) {
                // only allow owner (or later: admin) to delete their history
                ra.addFlashAttribute("error", "Akses ditolak");
                return "redirect:/user/" + userId + "/reservasi/history";
            }

            reservresto.reservasi.models.User user = userService.getById(userId);
            reservasiService.deleteReservasiHistoryByUser(user);
            ra.addFlashAttribute("message", "Riwayat reservasi berhasil dihapus.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Gagal menghapus riwayat: " + e.getMessage());
        }
        return "redirect:/user/" + userId + "/reservasi/history";
    }
}
