path "secret/data/hospital/shared/jwt" {
  capabilities = ["read"]
}

path "secret/data/hospital/api-gateway/*" {
  capabilities = ["read"]
}
