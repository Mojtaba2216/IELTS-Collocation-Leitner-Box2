# راه‌اندازی خروجی Release امضاشده برای کافه‌بازار

## فایل صحیح برای انتشار

در کافه‌بازار فایل Debug را بارگذاری نکنید:

`app-debug.apk`

فایل صحیح باید از Workflow زیر ساخته شود:

`Build Signed Release APK`

Artifact نهایی:

`Bazaar-Signed-Release-APK`

فایل APK داخل Artifact:

`ielts-collocation-bazaar-release.apk`

## ساخت GitHub Secrets

در Repository وارد این مسیر شوید:

`Settings → Secrets and variables → Actions → New repository secret`

چهار Secret زیر را بسازید:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

مقادیر این چهار Secret داخل بسته خصوصی امضای Release قرار دارند.

## نکات امنیتی

- فایل JKS و رمزهای آن را داخل Repository عمومی قرار ندهید.
- بسته امضا را برای شخص دیگری ارسال نکنید.
- برای تمام نسخه‌های بعدی برنامه از همین کلید ثابت استفاده کنید.
- پس از اولین انتشار، `applicationId` را تغییر ندهید.
- برای هر بروزرسانی، `versionCode` باید از نسخه قبلی بزرگ‌تر باشد.

## ساخت خروجی

پس از ثبت Secrets، Workflow را از بخش Actions اجرا کنید:

`Build Signed Release APK`

پس از سبزشدن، Artifact با نام زیر را دانلود کنید:

`Bazaar-Signed-Release-APK`
