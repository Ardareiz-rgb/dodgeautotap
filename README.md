# Dodge Auto Tap

GitHub Actions automatically builds the Android APK in the cloud.

## Telefonda yapacağın

1. GitHub'da yeni bir repository oluştur.
2. Bu ZIP'in içindeki dosyaları repository'ye yükle.
3. **Actions** sekmesine gir.
4. **Build Dodge Auto Tap APK** workflow'unu seç.
5. **Run workflow** veya push sonrası otomatik çalışan işlemi bekle.
6. Workflow tamamlanınca **Artifacts** bölümünden `DodgeAutoTap-debug` dosyasını indir.
7. ZIP'i açıp `app-debug.apk` dosyasını telefona kur.

APK, Accessibility Service kullanır ve ekranda tam olarak `Dodge` metnini bulduğunda, toggle açıksa o metnin merkezine dokunur.
