# 🚀 Development Setup

Maven Wrapper kullanarak Docker build olmadan tüm servisleri çalıştırır.

## 🎯 Çok Kolay Başlangıç

### En Basit Yol: Batch Dosyaları
```batch
# 1. Core servisleri başlat (çift tıkla)
start-all.bat

# 2. Ek servisleri başlat (isteğe bağlı)
start-apps.bat
```

### Alternatif: PowerShell (Gelişmiş)
```powershell
# Menü ile
.\dev.ps1

# Direkt komutlar
.\dev.ps1 -Action all     # Her şeyi başlat
.\dev.ps1 -Action infra   # Sadece infrastructure
.\dev.ps1 -Action core    # Core servisler
.\dev.ps1 -Action status  # Durum kontrol
.\dev.ps1 -Action stop    # Durdur
```

## 📊 Ne Çalışıyor?

### Infrastructure (Docker ile)
- 🐘 **PostgreSQL**: localhost:5432 (postgres/123456)
- 🍃 **MongoDB**: localhost:27017 (admin/admin123)  
- 📡 **Kafka**: localhost:9092
- 🐰 **RabbitMQ**: http://localhost:15672 (admin/admin123)
- 🔴 **Redis**: localhost:6379

### Core Servisler (Maven Wrapper ile)
- ⚙️ **Config Server**: http://localhost:8888
- 🔍 **Discovery Server**: http://localhost:8761
- 🌐 **Gateway**: http://localhost:8080 ← Ana URL
- 🔐 **Auth Service**: http://localhost:8081

### Application Servisler
- 📝 **Log Collector**: localhost:8082
- 🤖 **Anomaly Detector**: localhost:8083
- 🚨 **Alert Manager**: localhost:8084
- 📧 **Notification**: localhost:8085
- 🔧 **Auto Responder**: localhost:8086
- 📊 **Incident Tracker**: localhost:8087
- 📈 **Dashboard**: localhost:8088

## 🛠️ Development Workflow

```powershell
# 1. Her şeyi başlat
dev.bat all

# 2. Geliştirme yap
# Maven Wrapper otomatik reload yapar!

# 3. Test et
curl http://localhost:8080/actuator/health

# 4. Durdur
dev.bat stop
```

## 🧪 Test Etme

```bash
# Health checks
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8761                  # Discovery UI
curl http://localhost:8080/swagger-ui.html  # API Docs

# Kullanıcı kayıt/login
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123"}'
```

## 🎯 Avantajlar

- ❌ **Docker build yok** = İnternet tasarrufu
- 🔄 **Maven Wrapper** = Dependency otomatik çözümü
- ⚡ **Hot reload** = Kod değişikliği → otomatik restart
- 🐛 **IDE debug** = Full debug support
- 📱 **Modüler** = İstediğiniz servisleri çalıştırın

## 🆘 Sorun Giderme

### Port çakışması
```powershell
# Hangi process kullanıyor?
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Maven cache temizleme
```powershell
cd gateway-service
.\mvnw.cmd clean compile
```

### Logları görme
```powershell
# PowerShell job logları
Get-Job
Receive-Job -Id <JobID>

# Veya logs/ klasöründen
Get-Content logs\gateway-service.log -Wait
```

---

**🎉 Bu kadar! Artık Docker build olmadan hızlı development yapabilirsiniz!** 