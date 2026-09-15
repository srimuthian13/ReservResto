package reservresto.reservasi.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import reservresto.reservasi.models.Payment;
import reservresto.reservasi.models.Reservasi;
import reservresto.reservasi.models.User;
import reservresto.reservasi.repositories.PaymentRepository;
import reservresto.reservasi.repositories.ReservasiRepository;
import reservresto.reservasi.repositories.UserRepository;
import reservresto.reservasi.constan.Role;
import reservresto.reservasi.repositories.TableResRepository;


@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservasiRepository reservasiRepository;
    private final UserRepository userRepository;
    private final TableResRepository tableResRepository;

    public Payment processPayment(long reservasiId, int totalHarga, String paymentMethod) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        if (totalHarga <= 0) throw new IllegalArgumentException("Total harga harus lebih besar dari 0");

        if (paymentRepository.findByReservasi(reservasi).isPresent()) {
            throw new IllegalArgumentException("Reservasi sudah memiliki pembayaran");
        }

        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Metode pembayaran wajib diisi");
        }

        // if (!isValidPaymentMethod(paymentMethod)) {
        //     throw new IllegalArgumentException("Metode pembayaran tidak valid");
        // }

        Payment payment = new Payment();
        payment.setReservasi(reservasi);
        payment.setTotalHarga(totalHarga);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus("PENDING");

        // For non-balance methods we still simulate gateway success
        boolean paymentSuccess = true;

        if (paymentSuccess) {
            payment.setPaymentStatus("PAID");
            reservasi.setPaymentStatus("PAID");
            // mark reservation as confirmed when payment succeeds
            reservasi.setStatus("CONFIRMED");

            // decrement table quantity if available
            try {
                if (reservasi.getTable() != null) {
                    var table = reservasi.getTable();
                    if (table.getQuantity() > 0) {
                        table.setQuantity(table.getQuantity() - 1);
                        tableResRepository.save(table);
                    }
                }
            } catch (Exception ex) {
                // don't fail payment if table update fails; just log
                org.slf4j.LoggerFactory.getLogger(PaymentService.class).warn("Failed to decrement table quantity", ex);
            }
        } else {
            payment.setPaymentStatus("FAILED");
            reservasi.setPaymentStatus("UNPAID");
            reservasi.setStatus("UNPAID");
        }

        reservasiRepository.save(reservasi);
        return paymentRepository.save(payment);
    }

    /**
     * Save manual payment proof uploaded by user
     */
    public Payment savePaymentProof(long reservasiId, String fileName, String paymentMethod) {
        Reservasi reservasi = reservasiRepository.findById(reservasiId)
                .orElseThrow(() -> new IllegalArgumentException("Reservasi tidak ditemukan"));

        Payment payment = paymentRepository.findByReservasi(reservasi).orElse(new Payment());
        payment.setReservasi(reservasi);
        
        // Calculate total (Biaya Area/Ruangan + Subtotal Pesanan Makanan)
        int total = 0;
        if (reservasi.getTable() != null && reservasi.getTable().getRoomType() != null && reservasi.getTable().getRoomType().getPrice() != null) {
            total += reservasi.getTable().getRoomType().getPrice();
        }
        if (reservasi.getDetails() != null) {
            for (var d : reservasi.getDetails()) {
                if (d != null) {
                    total += d.getSubtotal();
                }
            }
        }
        
        payment.setTotalHarga(total);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus("WAITING_VERIFICATION");
        payment.setProofOfPayment(fileName);
        payment.setPaymentDate(java.time.LocalDateTime.now());

        reservasi.setPaymentStatus("WAITING_VERIFICATION");
        reservasi.setStatus("WAITING_VERIFICATION");

        reservasiRepository.save(reservasi);
        return paymentRepository.save(payment);
    }

    /**
     * Admin verifies payment
     */
    public Payment verifyPayment(long paymentId, boolean isApproved, String adminNote) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment tidak ditemukan"));

        Reservasi reservasi = payment.getReservasi();

        if (isApproved) {
            payment.setPaymentStatus("APPROVED");
            reservasi.setPaymentStatus("PAID");
            reservasi.setStatus("CONFIRMED");
            
            // Tambahkan total pembayaran ke Saldo/Pendapatan Admin
            try {
                User admin = userRepository.findByEmail("adminreserv@gmail.com").orElse(null);
                if (admin == null) {
                    admin = userRepository.findFirstByRole(Role.ADMIN).orElse(null);
                }
                if (admin != null) {
                    int currentBal = admin.getBalance();
                    admin.setBalance(currentBal + payment.getTotalHarga());
                    userRepository.save(admin);
                }
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(PaymentService.class).warn("Failed to add revenue to admin balance", ex);
            }
        } else {
            payment.setPaymentStatus("REJECTED");
            payment.setAdminNote(adminNote);
            reservasi.setPaymentStatus("REJECTED");
            reservasi.setStatus("WAITING_PAYMENT");
        }

        reservasiRepository.save(reservasi);
        return paymentRepository.save(payment);
    }

    // private boolean isValidPaymentMethod(String method) {
    //     return method.equalsIgnoreCase("CASH")
    //             || method.equalsIgnoreCase("E-WALLET")
    //             || method.equalsIgnoreCase("DEBIT_CARD")
    //             || method.equalsIgnoreCase("CREDIT_CARD");
    // }

}
