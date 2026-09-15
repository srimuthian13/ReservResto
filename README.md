# ReservResto

ReservResto adalah aplikasi sistem manajemen reservasi restoran berbasis web yang dibangun menggunakan Java dan Spring Boot. Aplikasi ini memungkinkan pelanggan untuk melakukan pemesanan meja dan makanan secara online, dengan antarmuka pengguna yang elegan dan premium.

## 🚀 Fitur Utama
* **Autentikasi & Otorisasi Pengguna:** Pendaftaran dan login pengguna dengan pembagian peran (Admin dan Pelanggan).
* **Reservasi Meja:** Pelanggan dapat memesan meja sesuai dengan tanggal, waktu, dan jenis ruangan (VIP, Reguler, dll).
* **Manajemen Menu:** Katalog hidangan dan minuman spesial yang bisa dilihat oleh pelanggan.
* **Manajemen Fasilitas/Area:** Menampilkan berbagai fasilitas dan area eksklusif yang ditawarkan oleh restoran.
* **Sistem Pembayaran:** Mendukung fitur upload bukti pembayaran (Top-Up) dan konfirmasi pembayaran.
* **Admin Dashboard:** Panel admin khusus untuk mengelola menu, meja, daftar ruangan, reservasi pengguna, dan riwayat pembayaran.

## 🛠️ Teknologi yang Digunakan
* **Backend:** Java 21, Spring Boot, Spring Data JPA
* **Frontend:** HTML5, CSS3, Thymeleaf (Template Engine)
* **Database:** MySQL
* **Build Tool:** Maven

## 📋 Prasyarat
Pastikan Anda telah menginstal perangkat lunak berikut sebelum menjalankan aplikasi:
* Java Development Kit (JDK) 21 atau lebih baru
* Maven
* MySQL Server (berjalan di port default 3306)

## ⚙️ Cara Menjalankan Aplikasi Secara Lokal

1. **Clone repositori ini:**
   ```bash
   git clone https://github.com/srimuthian13/ReservResto.git
   cd ReservResto
   ```

2. **Konfigurasi Database:**
   * Buat database baru di MySQL dengan nama `reservasi` (atau sesuaikan dengan konfigurasi di `application.properties`).
   * Pastikan `spring.datasource.username` dan `spring.datasource.password` di `src/main/resources/application.properties` sudah sesuai dengan kredensial MySQL lokal Anda.

3. **Jalankan Aplikasi:**
   Gunakan Maven wrapper yang sudah disediakan untuk menjalankan aplikasi:
   ```bash
   ./mvnw spring-boot:run
   ```
   Atau untuk pengguna Windows:
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```

4. **Akses Aplikasi:**
   Buka browser Anda dan kunjungi:
   ```text
   http://localhost:8080
   ```

## 📂 Struktur Direktori Utama
* `src/main/java/reservresto/reservasi/controllers`: Berisi controller untuk mengatur routing dan logika permintaan (Admin, User, Reservasi, Pembayaran, dll).
* `src/main/java/reservresto/reservasi/models`: Kelas entitas JPA untuk merepresentasikan tabel database.
* `src/main/java/reservresto/reservasi/repositories`: Interface repository Spring Data JPA untuk interaksi ke database.
* `src/main/java/reservresto/reservasi/service`: Layer service yang berisi logika bisnis aplikasi.
* `src/main/resources/templates`: Kumpulan file HTML Thymeleaf untuk tampilan frontend.
* `src/main/resources/static`: Aset statis seperti CSS, gambar (assets), dan file pendukung lainnya.

## 📄 Lisensi
Hak cipta dilindungi. Proyek ini dikembangkan untuk tujuan pembelajaran dan portofolio pelatihan.
