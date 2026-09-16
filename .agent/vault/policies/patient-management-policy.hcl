# Ortak JWT imzalama/doğrulama anahtarını okuma yetkisi
path "secret/data/hospital/shared/jwt" {
  capabilities = ["read"]
}

# patient-management'a ait veritabanı ve şifreleme anahtarlarını okuma yetkisi
path "secret/data/hospital/patient-management/*" {
  capabilities = ["read"]
}
