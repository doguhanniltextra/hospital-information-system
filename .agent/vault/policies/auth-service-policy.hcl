# Ortak JWT imzalama/doğrulama anahtarını okuma yetkisi
path "secret/data/hospital/shared/jwt" {
  capabilities = ["read"]
}

# auth-service'e özel veritabanı ve API anahtarlarını okuma yetkisi
path "secret/data/hospital/auth-service/*" {
  capabilities = ["read"]
}
