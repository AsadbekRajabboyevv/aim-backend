# AIM — Backend

Kafe, bar va restoranlar uchun onlayn menyu platformasining REST API qismi. Spring Boot ilovasi:
admin panel (Angular) va mijozlar ko'radigan planshet menyusiga xizmat qiladi.

## Texnologiyalar

- Java 21, Spring Boot 3.3
- Spring Security + JWT (access / refresh token), Google OAuth2, Telegram Login Widget, email tasdiqlash (SMTP)
- PostgreSQL, Spring Data JPA, Liquibase
- MapStruct, Lombok
- springdoc-openapi (Swagger UI)

## Talablar

- JDK 21
- PostgreSQL 13+ (yoki Docker: `docker compose up -d postgres` loyiha ildizidan)

`docker compose` bazani `online_menu` nomi, foydalanuvchisi va paroli bilan yaratadi — bu holda
`DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` ni `online_menu` qilib bering.

Maven alohida o'rnatilishi shart emas — loyihada `mvnw` wrapper bor.

## Ishga tushirish

```bash
cd backend
./mvnw spring-boot:run
```

Windows'da:

```bash
mvnw.cmd spring-boot:run
```

Ilova `http://localhost:8080` da ishga tushadi. Swagger UI: `http://localhost:8080/swagger-ui.html`.

Birinchi ishga tushishda Liquibase barcha migratsiyalarni (`src/main/resources/db/changelog`) qo'llaydi
va boshlang'ich ma'lumotlarni yaratadi: uchta til (uz / en / ru, standart — uz), tizim xabarlari va
super admin:

| Email | Parol |
|---|---|
| `super-admin@onlinemenu.local` | `ChangeMe123!` |

Birinchi kirishdan keyin parolni albatta o'zgartiring.

## Sozlamalar

Barcha qiymatlar `src/main/resources/application.yml` da environment o'zgaruvchilari orqali o'qiladi.
Namuna: loyiha ildizidagi `.env.example`.

| O'zgaruvchi | Vazifasi | Standart qiymat |
|---|---|---|
| `SERVER_PORT` | HTTP port | `8080` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL manzili | `localhost`, `5432`, `online_menu_db` |
| `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL foydalanuvchisi | `postgres`, `1234` |
| `JWT_SECRET` | JWT imzo kaliti (production'da albatta almashtiring) | — |
| `JWT_ACCESS_EXPIRATION_MS` | Access token muddati | 15 daqiqa |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token muddati | 7 kun |
| `CORS_ALLOWED_ORIGINS` | Frontend manzili | `http://localhost:4200` |
| `UPLOAD_BASE_DIR` | Yuklangan rasm/videolar papkasi | `./uploads` |
| `PUBLIC_MENU_BASE_URL` | QR kodlar ochadigan menyu manzili | `http://localhost:4200/menu` |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google orqali kirish | — |
| `OAUTH2_REDIRECT_URI` | Google'dan keyin frontend sahifasi | `http://localhost:4200/auth/google/callback` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | Tasdiqlash xatlari uchun SMTP. `MAIL_HOST` bo'sh bo'lsa kod va havola faqat logga yoziladi | — |
| `EMAIL_VERIFICATION_URL` | Xatdagi havola ochadigan frontend sahifasi | `http://localhost:4200/auth/verify-email` |
| `PASSWORD_RESET_URL` | Parolni tiklash xatidagi havola ochadigan sahifa | `http://localhost:4200/auth/reset-password` |
| `TELEGRAM_BOT_USERNAME`, `TELEGRAM_BOT_TOKEN` | Telegram orqali kirish (@BotFather, `/setdomain` bilan frontend domeni) | — |
| `GEMINI_API_KEY`, `OPENAI_API_KEY` | AI yordamchi kalitlari | — |
| `GEMINI_BASE_URL`, `OPENAI_BASE_URL` | AI provayder manzili (ixtiyoriy) | rasmiy API |
| `LOG_LEVEL` | `com.onlinemenu` log darajasi | `INFO` |

### Lokal maxfiy kalitlar

Kalitlarni repozitoriyga yozmang. Lokal ishlash uchun ularni
`src/main/resources/application-local.yml` fayliga qo'ying — u `.gitignore` da va
`application.yml` tomonidan avtomatik yuklanadi:

```yaml
app:
  oauth2:
    google:
      client-secret: <google-client-secret>
  ai:
    providers:
      gemini:
        api-key: <gemini-api-key>
```

## Rollar

- **SUPER_ADMIN** — barcha kafelar, foydalanuvchilar, tillar, tizim xabarlari, sozlamalar, AI
  vositalari va fayllarni boshqaradi.
- **CAFE_ADMIN** — faqat o'z kafelarini, menyusini, chegirmalarini va QR kodlarini boshqaradi.
  `/auth/register` (emailga yuborilgan 6 xonali kod yoki havola bilan tasdiqlangach), Google yoki
  Telegram orqali birinchi kirishda yaratiladi. Bitta foydalanuvchi ochishi
  mumkin bo'lgan kafelar soni va boshqa limitlar uning tarifiga bog'liq (pastda).

## API

Barcha endpointlar `/api/v1` ostida. Har bir javob `BaseResponse<T>` ko'rinishida, ro'yxatlar esa
`PageResponse<T>` bilan qaytadi. Obyektlar URL'larda UUID bilan emas, **slug** bilan aniqlanadi.
Til `X-Lang` headeri orqali tanlanadi.

| Yo'l | Tavsif |
|---|---|
| `/auth` | Kirish, ro'yxatdan o'tish, email tasdiqlash (`/verify-email`, `/verify-email/link`, `/verify-email/resend`), parolni tiklash (`/password/forgot`, `/password/reset`, `/password/reset/link`), Telegram (`/telegram`), token yangilash, chiqish |
| `/profile` | O'z profili va parolni o'zgartirish |
| `/users` | Foydalanuvchilar (super admin) |
| `/cafes` | Kafelar |
| `/cafes/{cafeSlug}/categories` | Menyu kategoriyalari |
| `/cafes/{cafeSlug}/products` | Mahsulotlar, stop-list |
| `/cafes/{cafeSlug}/discounts`, `/discounts` | Chegirmalar |
| `/cafes/{cafeSlug}/files` | Kafe rasmlari va videolari |
| `/files/orphans` | Hech qayerda ishlatilmayotgan fayllarni tozalash |
| `/languages` | Tillar |
| `/system-messages` | Tizim va interfeys matnlari tarjimalari |
| `/settings` | Platforma sozlamalari |
| `/public/menu` | Ochiq planshet menyusi va QR kod (autentifikatsiyasiz) |
| `/ai`, `/ai/settings`, `/ai/tools` | AI yordamchi: chat, sozlamalar, vositalar |
| `/billing`, `/billing/admin` | Tarif, limitlar, obuna, to'lov; super admin uchun tariflar va cheklovlar |
| `/public/billing/plans`, `/public/payments/{provider}` | Landing uchun tariflar; to'lov tizimlari callback'lari |

To'liq ro'yxat va sxemalar — Swagger UI'da.

## AI yordamchi

Admin panelda chat, u super admin **ishga tushgan paytda** yaratadigan vositalar (tools) orqali
ishlaydi — har bir yangi vosita uchun kod yozish shart emas.

- **Provayderlar:** Gemini (standart, bepul tarif bor) va OpenAI. Faol provayder va model super
  admin sahifasida tanlanadi. Chat tarixi provayderdan mustaqil formatda saqlanadi, shuning uchun
  provayder almashtirilganda tarix saqlanib qoladi.
- **Vositalar:** nomi, model uchun tavsif, shu API'dagi HTTP metod va yo'l, argumentlar uchun JSON
  Schema. Vosita chat qilayotgan foydalanuvchining **o'z tokeni** bilan chaqiriladi, shuning uchun
  AI foydalanuvchi o'zi qila olmaydigan ishni qila olmaydi. `/api/v1/ai`, `/api/v1/auth` va
  `/api/v1/profile/password` ga ruxsat yo'q.
- **Tasdiqlash:** `requiresConfirmation` belgilangan vosita darhol bajarilmaydi — chatda sxema
  asosida forma chiqadi va foydalanuvchi tasdiqlagandan keyingina ishlaydi.
- Yangi provayder qo'shish uchun `AiChatProvider` interfeysini Spring bean sifatida amalga
  oshiring va kalitini `app.ai.providers.<id>` ostiga qo'shing.

## Tariflar, limitlar va to'lovlar

Kafe administratorlari tarif bo'yicha cheklanadi; super admin cheklanmaydi. Hammasi super admin
panelidagi **Tariflar** sahifasidan boshqariladi (`/api/v1/billing/admin/**`).

- **Tariflar** (`billing_plan`) — nom, oylik va yillik narx (so'm), har bir cheklov qiymati. Boshlang'ich
  tariflar landing sahifadagi summalar bo'yicha: Kichik 290 000, O'rta 590 000, Katta 890 000,
  Tarmoq 1 290 000 so'm/oy, yillik — 20% chegirma. `bepul` — sinov yoki to'langan muddat tugaganda
  amal qiladigan yashirin tarif.
- **Cheklovlar** (`billing_limit_definition`) — tariflarda qiymat oladigan limitlar, `-1` — cheksiz:
  - `COUNT` — hozir nechta borligi (`cafes`, `products`), `UsageCounter` bean orqali hisoblanadi;
  - `METERED` — davr bo'yicha hisoblagich (`ai.messages`, `ai.actions` — oyiga);
  - `FEATURE` — bor/yo'q (`discounts`).

  **Yangi limit qo'shish uchun kod shart emas:** `METERED` yoki `FEATURE` cheklovga HTTP metod va yo'l
  namunasi (`/api/v1/cafes/*/discounts`) berilsa, `BillingRequestInterceptor` mos so'rovlarni o'zi
  to'xtatadi/hisoblaydi. Yo'lsiz cheklov faqat tariflar sahifasida ko'rsatiladi.
- **AI cheklovlari:** har bir xabar `ai.messages` dan, ma'lumotni o'zgartiruvchi har bir AI amali
  (GET bo'lmagan vosita) `ai.actions` dan hisoblanadi. Amallar tugasa, AI foydalanuvchiga tarifni
  oshirishni aytadi.
- **Limit oshsa** API `402 Payment Required` va `data` da `PlanLimitErrorDto` (qaysi limit, qancha
  ishlatilgan, tavsiya etilgan tarif) qaytaradi — panel shu asosda upgrade oynasini ochadi.
  `/api/v1/billing/me` limitlardan foydalanish va `suggestion` ni (limit to'lyapti, sinov tugayapti,
  muddat o'tgan) beradi.
- **Obuna:** yangi kafe administratori `billing.trial_days` kunlik bepul sinov oladi
  (`billing.trial_plan`). To'lovdan keyin: o'sha tarif va davr — muddat oxiriga qo'shiladi; boshqa
  tarif — darhol boshlanadi, eskisidan qolgan pul yangi tarifga kunlar sifatida o'tadi. Super admin
  naqd/bank o'tkazmasi uchun tarifni qo'lda berishi mumkin.

Sozlamalar (`billing.*`, `payment.<provider>.enabled`) `app_setting` jadvalida.

### To'lov tizimlari

Payme, Click va Uzum Bank ulangan. Kalitlar muhit o'zgaruvchilarida (`.env.example` ga qarang),
to'lov tizimi callback'lari bitta manzilga keladi:

```
<PAYMENT_CALLBACK_BASE_URL>/api/v1/public/payments/<payme|click|uzum>
```

Shu manzilni merchant kabinetiga kiriting (Uzum `/check`, `/create`, `/confirm`, `/reverse`,
`/status` ni shu manzil ostiga qo'shadi). Uzum xato kodlari va checkout havolasi Uzum ulashda beradigan
hujjat bilan solishtirib chiqilishi kerak: `UzumError` va `UZUM_CHECKOUT_URL`.

**Yangi to'lov tizimi qo'shish:** `billing.payment.PaymentProvider` ni Spring bean sifatida amalga
oshiring (`code`, `checkoutUrl`, `handleCallback`) va kalitlarini `app.payment.providers.<code>`
ostiga qo'shing. Buyurtma holatini faqat `PaymentService.markPaid` / `cancel` orqali o'zgartiring —
obuna shular ichida yangilanadi. Checkout, admin paneldagi yoqish/o'chirish va callback yo'li
qo'shimcha kodsiz ishlaydi. Lokal sinov uchun `PAYMENT_SANDBOX_ENABLED=true` — soxta provayder,
buyurtmani darhol to'langan qiladi (production'da yoqmang).

## Loyiha tuzilishi

```
src/main/java/com/onlinemenu/
  ai/              AI chat, provayderlar, vositalar, sozlamalar
  billing/         Tariflar, cheklovlar, obuna, to'lov tizimlari (Payme, Click, Uzum)
  auth/            Kirish, ro'yxatdan o'tish, refresh token
  cafe/            Kafelar, tema, QR
  menu/            Kategoriyalar, mahsulotlar, ochiq menyu
  discount/        Chegirmalar
  file/            Fayl yuklash va tozalash
  language/        Tillar
  systemmessage/   Tarjima qilinadigan matnlar
  setting/         Platforma sozlamalari
  profile/         Foydalanuvchi profili
  user/            Foydalanuvchilar
  security/        JWT, OAuth2, ruxsatlar
  common/          Konfiguratsiya, konstantalar, umumiy DTO va xatolar
```

Har bir modul `domain / dto + mapper / repository / service / controller` qatlamlariga bo'lingan.
Barcha konstantalar (rollar, kesh nomlari, limitlar) `common.constant` ichida saqlanadi.

## Build

```bash
./mvnw clean package -DskipTests
java -jar target/online-menu-backend-0.0.1-SNAPSHOT.jar
```
