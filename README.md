# ollamaDesktopClient

Ollama Desktop Client, Ollama sunucusuna bağlanarak masaüstünden kolayca sohbet edebileceğiniz, modelleri seçebileceğiniz ve sunucu adreslerini yönetebileceğiniz bir Java Swing uygulamasıdır.

## Özellikler
- Sunucu adreslerini ekleme, silme ve seçme
- Sunucudan kurulu modelleri otomatik olarak çekip seçebilme
- Chat geçmişini hatırlama ve sohbeti sıfırlama ("Yeni Sohbet")
- Chat ekranında Kullanıcı ve Asistan başlıkları farklı renklerde gösterilir
- Model listesini güncellemek için "Modelleri Güncelle" butonu
- Mesaj gönderildikten sonra soru alanı otomatik temizlenir
- Model ve sunucu değişiminde otomatik güncelleme
- JSON tabanlı API ile hızlı ve kolay bağlantı
- Türkçe arayüz

## Gereksinimler
- Java JDK 21 (veya uyumlu bir sürüm)
- Ollama sunucusu (örn: http://localhost:11434)
- gson-2.8.9.jar ve json-20250107.jar kütüphaneleri

## Kurulum ve Derleme (Windows)
1. Gerekli jar dosyalarını (gson-2.8.9.jar, json-20250107.jar) proje klasörüne indirin.
2. Java JDK 21 kurulu olduğundan emin olun ve JAVA_HOME değişkenini güncelleyin.
3. Komut satırında proje klasörüne gelin.
4. Derlemek ve çalıştırmak için:

```
compile.cmd
```
veya
```
run.cmd
```

## Manuel Derleme (Elle derlemek için)

```
javac -encoding UTF-8 -cp .;json-20250107.jar;gson-2.8.9.jar app\ollama.java
jar --create --file=ollama.jar --main-class=app.ollama -C . app
java -cp .;json-20250107.jar;gson-2.8.9.jar -jar ollama.jar
```

## Kullanım
- Uygulama açıldığında sunucu adresi ve model seçebilirsiniz.
- "Soruyu Gönder" ile mesajınızı iletebilir, "Yeni Sohbet" ile geçmişi temizleyebilirsiniz.
- "Modelleri Güncelle" butonu ile model listesini güncelleyebilirsiniz.
- Mesaj gönderdikten sonra soru alanı otomatik temizlenir.
- Sunucuya yeni adres ekleyebilir veya mevcut adresleri silebilirsiniz.
- Sohbet ekranında Kullanıcı ve Asistan başlıkları farklı renklerde gösterilir.

## Lisans
MIT