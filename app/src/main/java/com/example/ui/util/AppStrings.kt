package com.example.ui.util

enum class AppLanguage(val code: String, val displayName: String) {
    PERSIAN("fa", "فارسی"),
    ENGLISH("en", "English")
}

object AppStrings {
    fun slogan(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "اینترنت آزاد، سریع و امن" else "Free, Fast and Secure Internet"

    fun welcome(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "به دنیای اینترنت آزاد خوش آمدید" else "Welcome to the world of open internet"

    fun loginTitle(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "ورود به حساب" else "Sign In"

    fun loginSubtitle(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "نام کاربری و رمزی که از پشتیبانی دریافت کرده‌اید را وارد کنید" else "Enter the credentials provided by support"

    fun username(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "نام کاربری" else "Username"

    fun password(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "رمز عبور" else "Password"

    fun signIn(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "ورود" else "Log In"

    fun telegramHelp(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "در صورت مشکل به ما در تلگرام پیام دهید" else "Need support? Message us on Telegram"

    fun enterUsernameError(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "لطفاً نام کاربری خود را وارد کنید" else "Please enter your username"

    fun enterPasswordError(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "لطفاً رمز عبور خود را وارد کنید" else "Please enter your password"

    // Nav
    fun tabHome(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "خانه" else "Home"
    fun tabServers(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "سرورها" else "Servers"
    fun tabSubscription(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "اشتراک" else "Subscription"
    fun tabSettings(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "تنظیمات" else "Settings"

    // Home
    fun tapToConnect(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "برای اتصال ضربه بزنید" else "Tap to Connect"
    fun connecting(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "در حال اتصال به سرور..." else "Connecting to server..."
    fun connected(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "متصل هستید" else "Connected"
    fun disconnected(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "قطع هستید" else "Disconnected"
    fun ping(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "پینگ" else "Ping"
    fun sessionUsage(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "مصرف این اتصال" else "Session usage"
    fun remainingData(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "حجم باقیمانده" else "Remaining Data"
    fun remainingTime(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "زمان باقیمانده" else "Remaining Time"
    fun gigabyte(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "گیگ" else "GB"
    fun megabyte(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "مگ" else "MB"
    fun days(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "روز" else "Days"
    fun of(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "از" else "of"

    // Servers
    fun servers(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "سرورها" else "Servers"
    fun serversAvailable(count: Int, lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "$count سرور در دسترس" else "$count servers available"
    fun smartServer(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "سرور هوشمند" else "Smart Server"
    fun smartServerDesc(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "اتصال خودکار به کمترین پینگ" else "Auto-connect to lowest ping"
    fun locations(count: String, lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        count else count.replace("لوکیشن", "locations")

    // Subscription
    fun mySubscription(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "اشتراک من" else "My Subscription"
    fun totalUsage(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "مصرف کل" else "Total Usage"
    fun totalQuota(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "حجم کل اشتراک" else "Total Quota"
    fun purchasedDays(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "روزهای خریداری‌شده" else "Purchased Plan"
    fun subscriptionExpiry(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "پایان اشتراک" else "Subscription Expiry"
    fun planType(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "نوع اشتراک" else "Plan Type"
    fun connectedDevices(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "دستگاه‌های متصل" else "Active Devices"
    fun haveGiftCode(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "کد هدیه دارید؟" else "Have a gift code?"
    fun giftCodePlaceholder(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "کد تخفیف یا هدیه (مثلاً ALPHA5)" else "Discount or gift code (e.g. ALPHA5)"
    fun apply(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "ثبت" else "Apply"

    // Settings
    fun settings(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "تنظیمات" else "Settings"
    fun appearance(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "ظاهر برنامه" else "Appearance"
    fun displayMode(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "حالت نمایش" else "Theme Mode"
    fun dark(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "تیره" else "Dark"
    fun light(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "روشن" else "Light"
    fun auto(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "خودکار" else "Auto"
    fun language(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "زبان برنامه" else "Language"
    fun connection(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "اتصال" else "Connection"
    fun appWhitelist(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "لیست سفید برنامه‌ها" else "App Split Tunneling"
    fun appWhitelistDesc(count: Int, lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "همه برنامه‌ها از VPN استفاده می‌کنند ($count برنامه فعال)" else "All apps route through VPN ($count active)"
    fun directIranianSites(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "عبور مستقیم سایت‌های ایرانی (Bypass IR)" else "Direct Domestic Sites Bypass (IR-CIDR)"
    fun directIranianSitesDesc(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "۱,۴۲۰ رنج IP ملی و دامنه‌های ir. مستقیماً با سرعت داخلی و بدون کسر حجم باز می‌شوند" else "1,420 domestic IP ranges & .ir domains bypass tunnel with 0 quota consumed"
    fun adBlocking(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "مسدودسازی تبلیغات (DNS AdBlock)" else "DNS Ad & Tracker Blocking"
    fun adBlockingDesc(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "مسدودسازی هوشمند تبلیغات درون‌برنامه‌ای و بنرها با AdGuard DNS" else "Blocks in-app ads and trackers via AdGuard DNS"
    fun editUsername(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "ویرایش نام کاربری" else "Edit Username"
    fun usernameCopied(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN)
        "نام کاربری کپی شد" else "Username copied to clipboard"
    fun save(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "ذخیره" else "Save"
    fun cancel(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel"
    fun selectAll(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "انتخاب همه" else "Select All"
    fun deselectAll(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "لغو همه" else "Deselect All"
    fun searchApps(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "جستجوی برنامه..." else "Search apps..."
    fun logout(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "خروج" else "Log out"
    fun deviceId(lang: AppLanguage) = if (lang == AppLanguage.PERSIAN) "شناسه دستگاه" else "Device ID"
}
