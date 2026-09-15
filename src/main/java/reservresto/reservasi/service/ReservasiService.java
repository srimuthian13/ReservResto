package reservresto.reservasi.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import reservresto.reservasi.models.Reservasi;
import reservresto.reservasi.models.TableRes;
import reservresto.reservasi.repositories.ReservasiRepository;
import reservresto.reservasi.repositories.TableResRepository;
import reservresto.reservasi.models.User;
import reservresto.reservasi.models.Payment;
import reservresto.reservasi.repositories.PaymentRepository;
import reservresto.reservasi.repositories.UserRepository;
import reservresto.reservasi.constan.Role;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservasiService {

    private final ReservasiRepository reservasiRepository;
    private final TableResRepository tableResRepository;
    private final reservresto.reservasi.service.RoomTypeService roomTypeService;
    private final reservresto.reservasi.service.MenuService menuService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReservasiService.class);

    @Value("${restaurant.open:09:00}")
    private String openTimeStr;

    @Value("${restaurant.close:22:00}")
    private String closeTimeStr;

    public Reservasi createReservasi(long userId, long tableId, LocalDateTime reservasiTime, int durationMinutes, int totalGuest, String customerName, String customerPhone, String notes) {

        log.info("createReservasi: userId={}, tableId={}, time={}, duration={}", userId, tableId, reservasiTime, durationMinutes);

        // load user from repository so we can check role and log email
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));

        if (reservasiTime == null) {
            throw new IllegalArgumentException("Waktu reservasi tidak boleh kosong");
        }

        if (user.getRole() == null || !user.getRole().name().equals("USER")) {
            throw new IllegalArgumentException("Hanya USER yang boleh membuat reservasi");
        }

        TableRes table = tableResRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Meja dengan ID " + tableId + " tidak ditemukan"));

        // minimal reservasi adalah sekarang atau nanti (tidak boleh di masa lalu)
        if (reservasiTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Waktu reservasi harus sekarang atau di masa depan");
        }

        // validate opening hours
        java.time.LocalTime open = java.time.LocalTime.parse(openTimeStr);
        java.time.LocalTime close = java.time.LocalTime.parse(closeTimeStr);
        java.time.LocalTime rTime = reservasiTime.toLocalTime();
        if (rTime.isBefore(open) || rTime.isAfter(close.minusMinutes(1))) {
            throw new IllegalArgumentException(
                    "Waktu reservasi di luar jam buka restoran (" + open + " - " + close + ")");
        }

        if (durationMinutes < 30 || durationMinutes % 30 != 0) {
            throw new IllegalArgumentException("Durasi harus kelipatan 30 menit dan minimal 30.");
        }

        java.time.LocalDateTime endTime = reservasiTime.plusMinutes(durationMinutes);

        int overlapping = reservasiRepository.countOverlapping(table, reservasiTime, endTime);
        if (overlapping >= table.getQuantity()) {
            throw new IllegalArgumentException(
                    "Tidak ada meja tersedia pada waktu dan kapasitas tersebut (sudah penuh)");
        }

        Reservasi reservasi = new Reservasi();
        // associate the reservation with the user so queries by user work
        reservasi.setUser(user);
        reservasi.setTable(table);
        reservasi.setBookingTime(LocalDateTime.now());
        reservasi.setReservasiTime(reservasiTime);
        reservasi.setDurationMinutes(durationMinutes);
        reservasi.setEndTime(endTime);
        reservasi.setTotalGuest(totalGuest);
        reservasi.setCustomerName(customerName);
        reservasi.setCustomerPhone(customerPhone);
        reservasi.setNotes(notes);
        
        reservasi.setStatus("WAITING_PAYMENT");
        reservasi.setPaymentStatus("UNPAID");


        try {
            Reservasi saved = reservasiRepository.save(reservasi);
            log.info("Reservasi saved id={}", saved.getIdReservasi());
            return saved;
        } catch (Exception e) {
            log.error("Failed to save reservasi", e);
            throw e;
        }
    }

    public void addDetail(long reservasiId, long roomTypeId, int qty) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        reservresto.reservasi.models.RoomType rt = roomTypeService.getById(roomTypeId);
        if (rt == null)
            throw new IllegalArgumentException("Room type tidak ditemukan");

        reservresto.reservasi.models.ReservasiDetail d = new reservresto.reservasi.models.ReservasiDetail();
        d.setReservasi(reservasi);
        d.setRoomType(rt);
        d.setQuantity(qty <= 0 ? 1 : qty);
        int price = (rt.getPrice() == null) ? 0 : rt.getPrice();
        int duration = Math.max(30, reservasi.getDurationMinutes());
        int multiplier = Math.max(1, duration / 30);
        d.setSubtotal(price * d.getQuantity() * multiplier);

        if (reservasi.getDetails() == null)
            reservasi.setDetails(new java.util.ArrayList<>());
        reservasi.getDetails().add(d);

        reservasiRepository.save(reservasi);
    }

    public void addMenuDetail(long reservasiId, long menuId, int qty) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        reservresto.reservasi.models.Menu m = menuService.getMenuById(menuId);
        if (m == null)
            throw new IllegalArgumentException("Menu tidak ditemukan");

        reservresto.reservasi.models.ReservasiDetail d = new reservresto.reservasi.models.ReservasiDetail();
        d.setReservasi(reservasi);
        d.setMenu(m);
        d.setQuantity(qty <= 0 ? 1 : qty);
        int price = (m.getPrice() == null) ? 0 : m.getPrice();
        d.setSubtotal(price * d.getQuantity());

        if (reservasi.getDetails() == null)
            reservasi.setDetails(new java.util.ArrayList<>());
        reservasi.getDetails().add(d);

        reservasiRepository.save(reservasi);
    }

    public Reservasi getReservasiById(long id) {
        return reservasiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));
    }

    public List<Reservasi> getAllReservasi() {
        return reservasiRepository.findAll();
    }

    public List<Reservasi> getReservasiByUser(reservresto.reservasi.models.User user) {
        java.util.List<Reservasi> list = reservasiRepository.findByUser(user);
        if (list == null || list.isEmpty()) {
            log.info("No reservations found with findByUser for user id={}", user.getId());
            try {
                list = reservasiRepository.findByUserId(user.getId());
                log.info("findByUserId returned {} reservations for user id={}", (list == null ? 0 : list.size()), user.getId());
            } catch (Exception ex) {
                log.warn("findByUserId fallback failed for user id={}", user.getId(), ex);
            }
        } else {
            log.info("findByUser returned {} reservations for user id={}", list.size(), user.getId());
        }
        return (list == null) ? java.util.Collections.emptyList() : list;
    }

    /** Delete all reservations belonging to a user (including details/payments via cascade). */
    public void deleteReservasiHistoryByUser(User user) {
        List<Reservasi> list = getReservasiByUser(user);
        if (list == null || list.isEmpty()) return;
        try {
            reservasiRepository.deleteAll(list);
            log.info("Deleted {} reservations for user {}", list.size(), user.getId());
        } catch (Exception ex) {
            log.error("Failed to delete reservation history for user {}", user.getId(), ex);
            throw ex;
        }
    }

    public void confirmReservasi(long id) {
        Reservasi r = reservasiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservasi tidak ditemukan"));
        r.setStatus("CONFIRMED");
        reservasiRepository.save(r);
    }

    public void completeReservasi(long id) {
        Reservasi r = reservasiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservasi tidak ditemukan"));
        r.setStatus("COMPLETED");

        try {
            TableRes t = r.getTable();
            if (t != null) {
                t.setQuantity(t.getQuantity() + 1);
                tableResRepository.save(t);
            }
        } catch (Exception ex) {
            log.warn("Gagal mengembalikan kuantitas meja", ex);
        }

        reservasiRepository.save(r);
    }

    public void updateStatus(long id, String status) {
        Reservasi r = reservasiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservasi tidak ditemukan"));
        r.setStatus(status);
        reservasiRepository.save(r);
    }

    public void batalkanReservasi(long reservasiId) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        if (reservasi.getPaymentStatus() != null && "PAID".equalsIgnoreCase(reservasi.getPaymentStatus())) {
            throw new IllegalArgumentException("Tidak dapat membatalkan reservasi yang sudah dibayar");
        }

        try {
            reservasiRepository.delete(reservasi);
            log.info("Deleted unpaid reservation id={}", reservasiId);
        } catch (Exception ex) {
            log.error("Failed to delete unpaid reservation id={}", reservasiId, ex);
            throw new RuntimeException("Gagal menghapus reservasi");
        }
    }

    
    public void adminCancelReservasi(long reservasiId) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        try {
            TableRes t = reservasi.getTable();
            if (t != null) {
                t.setQuantity(t.getQuantity() + 1);
                tableResRepository.save(t);
            }
        } catch (Exception ex) {
            log.warn("Gagal mengembalikan kuantitas meja saat admin cancel", ex);
        }

        if (reservasi.getPaymentStatus() != null && "PAID".equalsIgnoreCase(reservasi.getPaymentStatus())) {
            try {
                java.util.Optional<Payment> opt = paymentRepository.findByReservasi(reservasi);
                if (opt.isPresent()) {
                    Payment p = opt.get();
                    int total = p.getTotalHarga();

                    try {
                        User u = reservasi.getUser();
                        if (u != null) {
                            u.setBalance(u.getBalance() + total);
                            userRepository.save(u);
                        }
                    } catch (Exception ex) {
                        log.warn("Gagal mengembalikan saldo user saat refund", ex);
                    }

                    try {
                        java.util.Optional<User> adminOpt = userRepository.findFirstByRole(Role.ADMIN);
                        if (adminOpt.isPresent()) {
                            User admin = adminOpt.get();
                            admin.setBalance(admin.getBalance() - total);
                            userRepository.save(admin);
                        }
                    } catch (Exception ex) {
                        log.warn("Gagal men-debit saldo admin saat refund", ex);
                    }

                    p.setPaymentStatus("REFUNDED");
                    paymentRepository.save(p);
                }
            } catch (Exception ex) {
                log.warn("Gagal memproses refund saat admin cancel", ex);
            }

            reservasi.setPaymentStatus("REFUNDED");
        }

        reservasi.setStatus("CANCELLED");
        reservasiRepository.save(reservasi);
    }

    public void deleteReservasi(long reservasiId) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        if (reservasi.getPaymentStatus() != null && "PAID".equalsIgnoreCase(reservasi.getPaymentStatus())) {
            throw new IllegalArgumentException("Tidak dapat menghapus reservasi yang sudah dibayar");
        }

        try {
            reservasiRepository.delete(reservasi);
            log.info("Permanently deleted reservation id={}", reservasiId);
        } catch (Exception ex) {
            log.error("Failed to delete reservation id={}", reservasiId, ex);
            throw new RuntimeException("Gagal menghapus reservasi");
        }
    }

}