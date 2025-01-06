# Dağıtık Abonelik Sistemi (Distributed Subscriber Service)

Bu proje, hata tolerans prensiplerine dayalı bir dağıtık abonelik sistemi geliştirmeyi hedefler. Proje kapsamında, sunucuların hata toleransı seviyesine göre abonelik listesini paylaşmaları ve güncellemeleri sağlanmıştır. Ayrıca bir plotter aracı ile bu güncellemelerin görselleştirilmesi gerçekleştirilmiştir.

## Özellikler

### ServerX.java Özellikleri
- [x] admin.rb üzerinden başlama komutlarını alma.
- [x] Hata toleransı 1 seviyesine göre liste paylaşımı ve güncelleme.
- [x] Hata toleransı 2 seviyesine göre liste paylaşımı ve güncelleme.
- [x] Sunucular arası haberleşme ve Configuration nesnesi paylaşımı.
- [x] İstemcilerle haberleşme ve Subscriber nesnesi paylaşımı.
- [x] Capacity talep sistemi geliştirme.

### ClientX.java Özellikleri
- [x] İlgili sunucuya bağlanarak abonelik talebi (SUBS) gönderme.
- [x] Abone durumu güncellemeleri yapma (ONLN ve OFFLN).
- [x] Hata tolerans seviyesine göre doğru sunucularla iletişim kurma.
- [x] Başarılı veya başarısız taleplerin loglanması.

### plotter.py Özellikleri
- [x] Sunuculardan admin.rb'ye gelen capacity verilerini alma ve görselleştirme.
- [x] 5 saniyede bir grafik güncelleme.
- [x] Sunucuların renk kodlarıyla ayrılması.

### admin.rb Özellikleri
- [x] Hata toleransı seviyesini ayarlama.
- [x] Sunuculara başlama komutları gönderme.
- [x] Kapasite talepleri gönderme ve sonuçlarını işleme.
- [x] Plotter'a kapasite verilerini gönderme.

## Ekip Üyeleri
- 20060810, Mustafa Enes TÜZÜN
- 21060996, Özgür ERCAN
- 21060612, Mehmet Akif CEBECİ
- 20060331, Mustafa ÇETİN

## Sunum Videosu Linki
- [Sunum Videosu Linki Buraya Eklenmelidir](#)

Ekip üyeleri, proje detaylarını ve kodun çalışma prensibini video üzerinden sunmuştur. Sunumda, proje kodlarının çalıştırılması ve logların gösterimi yapılmıştır.
