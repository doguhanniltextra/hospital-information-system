Api-gateway sistemin giriş kapısıdır, sistemde bulunan diğer mikroservislere ulaşmak için gereken kriterleri belirler. Sistemin güvenliğinden ve yönlendirmeden sorumludur. Gelen bir isteğin gereken tokeni olup olmadığını, kaç kere istek attığını ve engellenip engellenmemesi gerektiği gibi sorulara cevap verir.

src/main/java/com/project/ApiGateway.java ise mikroservisin root yeridir. @SpringBootApplication anotasyonu ile wraplenmiştir. @SpringBootApplication ise @ComponentScan, @EnableAutoConfiguration ve @SpringBootConfiguration anotasyonları ile oluşturulmuştur. @ComponentScan mikroservide bulunması beklenen @Component'leri bulur. @EnableAutoConfiguration ise Pom.xml'de bulunan dependency'lerden gelen .classpath'lerden sorumludur. Örneğin springboot-web-starter'ın oluşturması beklenen Tomcat server'ini burada oluşturur. @SpringBootConfiguration ise Application Context'in içerisinde -Application Context componentlerin tutulduğu bir pooldur ve singleton yapıyı oluşturur- @Bean oluşturulmasına olanak sağlar.

SecurityConfig.java ise güvenlikle ilgili konfigürasyonun yapıldığı dosyadır. @EnableWebFluxSecurity konfigürasyonu ile Flux aktif edilmiştir. Geleneksel Spring MVC (Tomcat) "Thread-per-request" modeliyle çalışır; yani her gelen istek için havuzdan yeni bir thread açar (birden fazla thread oluşturan model budur ve yüksek yükte şişer). WebFlux (Netty) ise "Event-Loop" ve "Non-blocking" çalışır. İşlemci çekirdeği (CPU core) sayısı kadar çok az sayıda thread vardır. İstek gelir, thread onu asenkron bir sürece devreder ve hemen yeni bir isteği karşılamaya döner. Yani çok thread açarak değil, az thread'i hiç bloklamadan (bekletmeden) sürekli kullanarak yüksek performans sağlar. @Configuration ise bu dosyanın component olarak konfigürasyon anotasyonu olduğunu söyler; isimlendirme içindir. @EnableConfigurationProperties ise .yml içerisinde bulunan verileri Java nesnelerine dönüştürmek için -POJO- kullanılır.

```
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable()) // in development
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**", "/api/auth/**", "/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
```

Bu fonksiyonda ise SpringBoot'un tanıması için @Bean anotasyonunu belirtiyoruz. SecurityWebFilterChain filtresini custom bir şekilde yeniden dizayn ediyoruz. CSRF'i mimari bir zorunluluk olarak kapatıyoruz.Bunun sebebi ise Session-Cookie tabanlı değil stateless olarak JWT tabanlı olduğu için bunu kapatıyoruz. CSRF ise çerezlerden yapılan bir saldırı türüdür. `authorizeExchange` ise hangi uç isteğin -endpoint- authenticated olması gerektiğini belirlediğimiz ve `.addFilterBefore` diyerek bu filtre öncesinde custom olarak oluşturduğumuz JWT bekleme fonksiyonunu da giriyoruz. Bu sayede api-gateway'e istek atıldığında eğer ki özellikle olmaması gerektiği belirtilmediyse `SecurityWebFiltersOrder.AUTHENTICATION` filtresi yardımıyla JWT isteyeceğiz.

```
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService() {
        return username -> Mono.empty();
    }
```

Bu @Bean anotasyonu ile tanımlı fonksiyon ise sistemin built-in olarak verdiği güvenlik ayarlamasını kapatıyoruz çünkü biz kendimiz custom yazıyoruz.

```
  @Bean
    public WebFilter jwtAuthenticationFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Skip authentication for permitted paths
            if (path.startsWith("/auth/") || path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    Key signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(signingKey)
                            .setAllowedClockSkewSeconds(30) // Zero Trust: 30 saniye tolerance
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    String subject = claims.getSubject();

                    if (subject != null) {
                        List<String> roles = claims.get("roles", List.class);
                        Collection<SimpleGrantedAuthority> authorities = Collections.emptyList();
                        if (roles != null) {
                            authorities = roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                        }

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(subject,
                                token, authorities);

                        log.debug("JWT authentication successful for user: {}", subject);

                        // Forward caller identity to downstream services as trusted internal headers
                        String rolesHeader = (roles != null) ? String.join(",", roles) : "";
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("X-Auth-User-Id", subject)
                                .header("X-Auth-Roles", rolesHeader)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    }

                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            } else {
                log.debug(path);
            }

            log.debug("No authentication set — will result in 401");
            return chain.filter(exchange);
        };
    }
```

Bu @Bean anotasyonlu fonksiyonda ise custom bir Json Web Token authentication filter tanımlıyoruz. İlk önce ` ServerHttpRequest request = exchange.getRequest();` diyerek isteği karşılıyoruz. Daha sonra ise `String path = request.getPath().value();` diyerek gelen istekten URL'i alıyoruz.

```
    if (path.startsWith("/auth/") || path.startsWith("/api/auth/") || path.startsWith("/actuator/")) {
                return chain.filter(exchange);
            }
```

Burada ise gelen URL'in beklenen endpointlerden olup olmadığına bakıyoruz. Eğer öyle ise zincire dahil etmeden geçiriyoruz, çünkü bu endpointlerden herhangi bir token isteğimiz olmayacak.

`String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);` authHeader variable'sinin içerisine gelen isteğin Header'ını alıyoruz ve ilk parametre olan `Authorization: Bearer <Token>` kısmını almış olduk.

```
  if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
```

eğer ki authHeader null değil ise ve authHeader `Bearer` ile başlıyorsa if-else condition'unun başlatıyoruz. Bu conditionun içerisinde ilk olarak authHeader'e substring uygulayarak 7.indeksten sonrasını token variable'sine saklıyoruz.

```
                try {
                    Key signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
```

Oluşturacağımız business logic'i try-catch içerisine alarak oluşacak herhangi bir error'ü catchleyebilecek şekle sokuyoruz. signinKey variablesinin içerisine .env'den aldığımız secret'in hmacShaKeyFor halini yani en açık formunu alıyoruz.

```
                   Claims claims = Jwts.parserBuilder()
                            .setSigningKey(signingKey)
                            .setAllowedClockSkewSeconds(30) // Zero Trust: 30 saniye tolerance
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
```

Bir claim oluşturuyoruz. Claim ise gelen token bilgisi ile elimizde bulunan secret bilgisini karşılaştırarak doğruluğunu kanıtlamaya çalışacağız. Jwts kütüphanesinden parserBuilder() sınıfını çağırıyoruz ve ilk önce `setSigninKey()` diyerek kendi key'imizi koyuyoruz. `setAllowedClockSkewSeconds` diyerek en fazla otuz saniye içerisinde olması gerektiğini söylüyoruz. Daha sonra `parseClaimsJws()` diyerek gelen istekten aldığımız Authorization bilgisini giriyoruz ve bir `body` almayı beliyoruz.

`String subject = claims.getSubject();` diyerek subject içerisine tokenin decrypted edilmesi sonucu beklediğimiz subjectleri saklıyoruz. Bu subjectlerin içinde `roles` gibi parametreler var.

```
     if (subject != null) {
                        List<String> roles = claims.get("roles", List.class);
                        Collection<SimpleGrantedAuthority> authorities = Collections.emptyList();
                        if (roles != null) {
                            authorities = roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                        }`
```

Eğer ki subject boş değilse bir if condition'u başlatıyoruz. Bu condition içerisinde `roles`'in içerisinde bulunan parametreleri String bekleyen List'in içerisine alıyoruz. `Collection<SimpleGrantedAuthority>` ise Spring Security'de default olarak gelen bir otorite sınıfıdır.

```
                        if (roles != null) {
                            authorities = roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                        }
```

Eğer ki rol boş değilse yeni bir if condition'u başlatıyoruz ve otoriteleri stream ederek List'in içerisine yüklüyoruz.

`UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(subject,token, authorities);` ise Spring Security'e kullanıcının doğrulanmış olduğunu belirtmek için oluşturduğumuz bir instancedir. Bu instance ile birlikte bir security context oluşturmuş oluyoruz.

```
            String rolesHeader = (roles != null) ? String.join(",", roles) : "";
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("X-Auth-User-Id", subject)
                                .header("X-Auth-Roles", rolesHeader)
                                .build();
```

Burada ise internal mikroservisler için ek header'lar oluşturuyoruz ve kullanabilmeleri için `roles` parametresinden çıkan valueleri içine ekliyoruz. Bu sayede eğer ki kullanıcı bir PATIENT ise patient-service bunu doğrudan buradan extract edebilecek.

RateLimitConfig.java ise api-gateway üzerinde Rate limit yaparak IP ve userId kullanarak brute-force'yi engellemek için kurulmuştur.
