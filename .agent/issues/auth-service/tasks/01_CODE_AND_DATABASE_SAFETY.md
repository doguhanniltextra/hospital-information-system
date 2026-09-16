# Task 01: Uygulama Yapılandırması ve Veritabanı Güvenliği

Bu görev, `auth-service` mikroservisinin `application.properties` dosyasındaki kod içi fallback secret'ın (`app.secret`) kaldırılmasını ve Hibernate `ddl-auto=update` ayarının güvenli hale getirilmesini kapsar.

---

## 🎯 Amaç ve İlgili Bulgular
- **İlgili Bulgular:** [logs.md (Bulgu 2)](file:///home/doguhan/SoftwareProjects/hospital-information-system/.agent/issues/auth-service/logs.md#L21-L32)
- **Öncelik:** 🔴 P1 (Kritik Güvenlik & DB Bütünlüğü)

---

## 🛠️ Yapılacak Değişiklikler

### 1. `auth-service/src/main/resources/application.properties` Güncellemesi

1. **Fallback Secret'ın Kaldırılması:**
   ```properties
   # Ortam değişkeni zorunlu kılınır, kod içi varsayılan şifre temizlenir:
   app.secret=${APP_SECRET}
   ```

2. **JPA/Hibernate Güvenliği:**
   ```properties
   spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:validate}
   ```

---

## 📋 Görev Tamamlanma Kriterleri (Checklist)

- [x] `application.properties` içindeki `app.secret` tanımından varsayılan metin kaldırıldı.
- [x] `ddl-auto` için dinamik parametre (`validate` varsayılanı ile) eklendi.
- [x] `mvn clean test` ile tüm birim testlerin (16/16) başarılı olduğu doğrulandı.
