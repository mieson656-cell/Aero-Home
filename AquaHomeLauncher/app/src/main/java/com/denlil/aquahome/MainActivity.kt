package com.denlil.aquahome

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: android.graphics.drawable.Drawable
)

class MainActivity : Activity() {
    private enum class Screen { HOME, DRAWER, SETTINGS }

    private var screen = Screen.HOME
    private var homePage = 0
    private lateinit var root: FrameLayout
    private var apps: List<AppEntry> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var clockView: TextView? = null
    private var dateView: TextView? = null
    private var touchStartX = 0f
    private var touchStartY = 0f

    private val clockTick = object : Runnable {
        override fun run() {
            clockView?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            dateView?.text = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(0, 132, 181)
        window.navigationBarColor = Color.rgb(0, 108, 154)
        window.decorView.systemUiVisibility = 0
        root = FrameLayout(this)
        root.addView(AeroBackground(this), FrameLayout.LayoutParams(-1, -1))
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(0, bars.top, 0, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            }
            insets
        }
        setContentView(root)
        apps = loadApps()
        renderHome(0)
        handler.post(clockTick)
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) {
            apps = loadApps()
            if (screen == Screen.HOME) renderHome(homePage)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(clockTick)
        super.onDestroy()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val wasHome = screen == Screen.HOME
        val action = ev.actionMasked
        if (wasHome && action == MotionEvent.ACTION_DOWN) {
            touchStartX = ev.rawX
            touchStartY = ev.rawY
        }
        val handled = super.dispatchTouchEvent(ev)
        if (wasHome && action == MotionEvent.ACTION_UP && screen == Screen.HOME) {
            val dx = ev.rawX - touchStartX
            val dy = ev.rawY - touchStartY
            if (abs(dx) > dp(105) && abs(dy) < dp(75)) {
                if (dx < 0) renderHome(1) else renderHome(0)
            } else if (dy < -dp(125) && abs(dx) < dp(90)) {
                renderDrawer(false)
            }
        }
        return handled
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when (screen) {
            Screen.DRAWER, Screen.SETTINGS -> renderHome(homePage)
            Screen.HOME -> super.onBackPressed()
        }
    }

    private fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val ai = info.activityInfo ?: return@mapNotNull null
                try {
                    AppEntry(
                        label = info.loadLabel(packageManager).toString(),
                        packageName = ai.packageName,
                        activityName = ai.name,
                        icon = info.loadIcon(packageManager)
                    )
                } catch (_: Exception) { null }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun addAeroBackdrop() {
        root.addView(AeroBackground(this), FrameLayout.LayoutParams(-1, -1))
    }

    private fun renderHome(page: Int) {
        screen = Screen.HOME
        homePage = page.coerceIn(0, 1)
        root.removeAllViews()
        addAeroBackdrop()
        clockView = null
        dateView = null

        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
        }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val branding = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        branding.addView(text("AQUA  •  DESKTOP", 20f, Color.WHITE, true))
        branding.addView(text("A BREATH OF FRESH AIR", 9f, 0xFFE5FAFF.toInt(), true).apply { letterSpacing = 0.18f })
        header.addView(branding, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(actionPill("⚙  Параметры") { renderSettings() })
        content.addView(header)

        val clockCard = glassCard(22).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(15), dp(20), dp(17))
        }
        clockCard.addView(text(if (homePage == 0) "ПОНЕДЕЛЬНИК НАЧИНАЕТСЯ ЗДЕСЬ" else "ТВОЯ ЦИФРОВАЯ СРЕДА", 10f, 0xFF327D9D.toInt(), true))
        val time = text("--:--", 52f, 0xFF075B83.toInt(), true)
        time.setShadowLayer(dp(5).toFloat(), 0f, dp(2).toFloat(), 0x44FFFFFF)
        clockView = time
        clockCard.addView(time)
        val date = text("", 14f, 0xFF247697.toInt(), true)
        dateView = date
        clockCard.addView(date)
        val micro = text("SKY  •  WATER  •  LIGHT", 9f, 0xFF478DA8.toInt(), true).apply {
            setPadding(0, dp(9), 0, 0)
        }
        clockCard.addView(micro)
        val clockParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(23) }
        content.addView(clockCard, clockParams)

        val search = glassRow().apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            isClickable = true
            isFocusable = true
            setOnClickListener { renderDrawer(true) }
        }
        search.addView(text("⌕", 27f, 0xFF1388B1.toInt(), true))
        search.addView(text("Найти приложение или открыть…", 14f, 0xFF377F9D.toInt(), false).apply {
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        search.addView(text("ОТКРЫТЬ  ›", 9f, 0xFF2583A5.toInt(), true))
        val searchParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(13) }
        content.addView(search, searchParams)

        if (homePage == 0) {
            sectionTitle(content, "ИЗБРАННОЕ", "БЫСТРЫЙ ДОСТУП")
            val favorites = favoriteApps(8)
            content.addView(appGrid(favorites, 4), LinearLayout.LayoutParams(-1, -2))
            val info = glassCard(18).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(15), dp(13), dp(15), dp(13))
            }
            info.addView(text("◉", 27f, 0xFF1B9CC5.toInt(), true))
            val infoText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
            infoText.addView(text("A little more blue", 14f, 0xFF176D91.toInt(), true))
            infoText.addView(text("Чистый воздух. Мягкий свет. Тихое море.", 10f, 0xFF4A8BA3.toInt(), false))
            info.addView(infoText, LinearLayout.LayoutParams(0, -2, 1f))
            content.addView(info, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            content.addView(sectionTitleView("СИСТЕМА", "БЫСТРЫЕ ПЕРЕХОДЫ"))
            val quickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            quickRow.addView(quickSetting("◉", "Wi-Fi", "СЕТЬ") {
                try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (_: Exception) {}
            }, LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginEnd = dp(5) })
            quickRow.addView(quickSetting("ᛒ", "Bluetooth", "УСТРОЙСТВА") {
                try { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) } catch (_: Exception) {}
            }, LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginStart = dp(5); marginEnd = dp(5) })
            quickRow.addView(quickSetting("☼", "Экран", "ЯРКОСТЬ") {
                try { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) } catch (_: Exception) {}
            }, LinearLayout.LayoutParams(0, dp(76), 1f).apply { marginStart = dp(5) })
            content.addView(quickRow)
            val nature = glassCard(20).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(15), dp(13), dp(15), dp(13))
            }
            nature.addView(text("THE AQUA EDITION", 9f, 0xFF2583A5.toInt(), true).apply { letterSpacing = 0.16f })
            nature.addView(text("Светлое небо, зелёные холмы и прозрачная вода — знакомая эстетика цифрового будущего.", 12f, 0xFF397F99.toInt(), false).apply { setPadding(0, dp(5), 0, 0) })
            content.addView(nature, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        } else {
            sectionTitle(content, "МОЯ КОЛЛЕКЦИЯ", "ВСЕ УСТАНОВЛЕННЫЕ")
            content.addView(appGrid(favoriteApps(16), 4), LinearLayout.LayoutParams(-1, -2))
            val hint = glassCard(18).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15)) }
            hint.addView(text("Хочешь найти что-то ещё?", 15f, 0xFF176D91.toInt(), true))
            hint.addView(text("Открой полный список приложений через поиск или кнопку ниже.", 11f, 0xFF4A8BA3.toInt(), false).apply { setPadding(0, dp(5), 0, 0) })
            hint.addView(actionPill("Открыть все приложения  ›") { renderDrawer(false) }.apply { setPadding(0, dp(12), 0, 0) })
            content.addView(hint, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }

        val dock = glassRow().apply {
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }
        val dockApps = favoriteApps(4)
        dockApps.forEach { app ->
            dock.addView(dockTile(app), LinearLayout.LayoutParams(0, dp(62), 1f))
        }
        val dockParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        shell.addView(dock, dockParams)

        val pageDots = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        repeat(2) { index ->
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (index == homePage) Color.WHITE else 0x779DE4F4.toInt())
                }
                isClickable = true
                setOnClickListener { renderHome(index) }
            }
            pageDots.addView(dot, LinearLayout.LayoutParams(dp(if (index == homePage) 18 else 7), dp(7)).apply {
                marginStart = dp(4); marginEnd = dp(4)
            })
        }
        shell.addView(pageDots)
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        clockTick.run()
    }

    private fun renderDrawer(focusSearch: Boolean) {
        screen = Screen.DRAWER
        root.removeAllViews()
        addAeroBackdrop()
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(actionPill("‹  Назад") { renderHome(homePage) })
        top.addView(text("МОИ ПРИЛОЖЕНИЯ", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), 0x55005A86)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text("${apps.size}", 12f, 0xFFE8FAFF.toInt(), true))
        shell.addView(top)

        val search = EditText(this).apply {
            hint = "Поиск приложения…"
            textSize = 15f
            setTextColor(0xFF155F7E.toInt())
            setHintTextColor(0xFF69A7BC.toInt())
            isSingleLine = true
            setPadding(dp(15), 0, dp(15), 0)
            background = glassDrawable(18)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        }
        shell.addView(search, LinearLayout.LayoutParams(-1, dp(49)).apply { topMargin = dp(15); bottomMargin = dp(12) })
        val scroll = ScrollView(this).apply { clipToPadding = false }
        val grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        scroll.addView(grid, ViewGroup.LayoutParams(-1, -2))
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))

        fun updateGrid(query: String) {
            grid.removeAllViews()
            val filtered = apps.filter { it.label.contains(query.trim(), true) || it.packageName.contains(query.trim(), true) }
            filtered.forEach { app ->
                val tile = appTile(app, true)
                val lp = GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED)
                ).apply {
                    width = 0
                    height = dp(104)
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                grid.addView(tile, lp)
            }
            if (filtered.isEmpty()) {
                val empty = text("Ничего не найдено", 14f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(30), 0, 0) }
                grid.addView(empty, GridLayout.LayoutParams(GridLayout.spec(0, 1), GridLayout.spec(0, 4)).apply { width = -1 })
            }
        }
        updateGrid("")
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateGrid(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        if (focusSearch) {
            search.requestFocus()
            search.post {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(search, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun renderSettings() {
        screen = Screen.SETTINGS
        root.removeAllViews()
        addAeroBackdrop()
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(20))
        }
        shell.addView(actionPill("‹  На рабочий стол") { renderHome(homePage) })
        shell.addView(text("ПАРАМЕТРЫ СРЕДЫ", 20f, Color.WHITE, true).apply {
            setPadding(0, dp(25), 0, dp(16))
            setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), 0x55005A86)
        })
        val card = glassCard(22).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(17), dp(17), dp(17), dp(17)) }
        card.addView(text("Назначить главным экраном", 16f, 0xFF176D91.toInt(), true))
        card.addView(text("После выбора Aqua Home будет открываться при нажатии кнопки «Домой».", 12f, 0xFF4A8BA3.toInt(), false).apply { setPadding(0, dp(6), 0, dp(13)) })
        card.addView(actionPill("Открыть настройки главного экрана  ›") { openHomeSettings() })
        shell.addView(card)

        val about = glassCard(22).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(17), dp(17), dp(17), dp(17)) }
        about.addView(text("О лаунчере", 16f, 0xFF176D91.toInt(), true))
        about.addView(text("Aqua Home 1.0\nFrutiger Aero • Android 8+\nЛёгкий нативный лаунчер без сторонних библиотек.", 12f, 0xFF4A8BA3.toInt(), false).apply { setPadding(0, dp(8), 0, 0) })
        shell.addView(about, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
    }

    private fun openHomeSettings() {
        val intent = try {
            if (android.os.Build.VERSION.SDK_INT >= 29) Intent(Settings.ACTION_HOME_SETTINGS)
            else Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } catch (_: Exception) { Intent(Settings.ACTION_SETTINGS) }
        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun favoriteApps(limit: Int): List<AppEntry> {
        val preferred = listOf(
            "com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer",
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
            "com.android.chrome", "com.sec.android.app.camera", "com.google.android.youtube",
            "com.android.settings", "com.sec.android.app.sbrowser"
        )
        val chosen = mutableListOf<AppEntry>()
        preferred.forEach { pkg -> apps.firstOrNull { it.packageName == pkg }?.let { if (chosen.none { a -> a.packageName == it.packageName }) chosen.add(it) } }
        apps.forEach { if (chosen.size < limit && chosen.none { a -> a.packageName == it.packageName }) chosen.add(it) }
        return chosen.take(limit)
    }

    private fun appGrid(entries: List<AppEntry>, columns: Int): GridLayout {
        val grid = GridLayout(this).apply { columnCount = columns; useDefaultMargins = false }
        entries.forEach { app ->
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED)
            ).apply { width = 0; height = dp(104); setMargins(dp(2), dp(2), dp(2), dp(2)) }
            grid.addView(appTile(app, false), lp)
        }
        return grid
    }

    private fun appTile(app: AppEntry, inDrawer: Boolean): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(3), dp(7), dp(3), dp(5))
            isClickable = true
            isFocusable = true
            setOnClickListener { launchApp(app) }
            setOnLongClickListener { openAppInfo(app); true }
            background = if (inDrawer) glassDrawable(16) else transparentRipple()
        }
        val iconWrap = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xF2FFFFFF.toInt(), 0xBDE4FAFF.toInt(), 0x99A8E6F2.toInt())).apply { cornerRadius = dp(17).toFloat(); setStroke(dp(1), 0xE6FFFFFF.toInt()) }
            elevation = dp(3).toFloat()
        }
        val image = ImageView(this).apply {
            setImageDrawable(app.icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(7), dp(7), dp(7), dp(7))
            contentDescription = app.label
        }
        iconWrap.addView(image, FrameLayout.LayoutParams(dp(50), dp(50), Gravity.CENTER))
        tile.addView(iconWrap, LinearLayout.LayoutParams(dp(58), dp(58)))
        val label = text(app.label, 10.5f, if (inDrawer) 0xFF155F7E.toInt() else Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(5), 0, 0)
            if (!inDrawer) setShadowLayer(dp(3).toFloat(), 0f, dp(1).toFloat(), 0x77005C80)
        }
        tile.addView(label, LinearLayout.LayoutParams(-1, -2))
        return tile
    }

    private fun dockTile(app: AppEntry): View {
        val tile = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = transparentRipple()
            setOnClickListener { launchApp(app) }
            setOnLongClickListener { openAppInfo(app); true }
        }
        val image = ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = app.label }
        tile.addView(image, LinearLayout.LayoutParams(dp(43), dp(43)))
        return tile
    }

    private fun launchApp(app: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.packageName, app.activityName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(app: AppEntry) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
        } catch (_: Exception) { Toast.makeText(this, "Настройки приложения недоступны", Toast.LENGTH_SHORT).show() }
    }

    private fun sectionTitleView(title: String, subtitle: String): View {
        val row = LinearLayout(this).apply { gravity = Gravity.BOTTOM; setPadding(dp(2), dp(18), dp(2), dp(7)) }
        row.addView(text(title, 11f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(subtitle, 8f, 0xFFD9F5FF.toInt(), true))
        return row
    }

    private fun quickSetting(symbol: String, title: String, caption: String, action: () -> Unit): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glassDrawable(17)
            elevation = dp(3).toFloat()
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
            setPadding(dp(4), dp(8), dp(4), dp(7))
            addView(text(symbol, 22f, 0xFF1388B1.toInt(), true))
            addView(text(title, 10f, 0xFF176D91.toInt(), true))
            addView(text(caption, 7f, 0xFF4A8BA3.toInt(), true))
        }
    }

    private fun sectionTitle(parent: LinearLayout, title: String, subtitle: String) {
        val row = LinearLayout(this).apply { gravity = Gravity.BOTTOM; setPadding(dp(2), dp(22), dp(2), dp(7)) }
        row.addView(text(title, 12f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f).apply { gravity = Gravity.BOTTOM })
        row.addView(text(subtitle, 8f, 0xFFD9F5FF.toInt(), true))
        parent.addView(row)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        includeFontPadding = true
    }

    private fun glassCard(radius: Int): LinearLayout = LinearLayout(this).apply {
        background = glassDrawable(radius)
        elevation = dp(5).toFloat()
    }

    private fun glassRow(): LinearLayout = LinearLayout(this).apply {
        background = glassDrawable(22)
        elevation = dp(4).toFloat()
    }

    private fun glassDrawable(radius: Int): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(0xDDFBFFFF.toInt(), 0x88D5F5FF.toInt(), 0x66B6EAF8.toInt())
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), 0xCCFFFFFF.toInt())
    }

    private fun actionPill(label: String, action: () -> Unit): TextView = text(label, 11f, 0xFF176D91.toInt(), true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(13), dp(10), dp(13), dp(10))
        background = glassDrawable(30)
        elevation = dp(3).toFloat()
        isClickable = true
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun transparentRipple(): android.graphics.drawable.Drawable {
        val content = GradientDrawable().apply { setColor(0x22FFFFFF); cornerRadius = dp(15).toFloat() }
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x55FFFFFF), content, null
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}

private class AeroBackground(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = resources.displayMetrics.density

    private fun shape(canvas: Canvas, color: Int, build: Path.() -> Unit) {
        p.shader = null; p.style = Paint.Style.FILL; p.color = color
        canvas.drawPath(Path().apply(build), p)
    }

    private fun cloud(canvas: Canvas, x: Float, y: Float, s: Float, a: Int) {
        p.shader = RadialGradient(x, y, s * 1.55f,
            intArrayOf((a shl 24) or 0xFFFFFF, (a / 2 shl 24) or 0xE8FAFF, 0x00FFFFFF),
            floatArrayOf(0f, .66f, 1f), Shader.TileMode.CLAMP)
        canvas.drawOval(x - s * 1.8f, y - s * .34f, x + s * 1.8f, y + s * .55f, p)
        canvas.drawCircle(x - s * .68f, y - s * .12f, s * .58f, p)
        canvas.drawCircle(x, y - s * .42f, s * .77f, p)
        canvas.drawCircle(x + s * .72f, y - s * .08f, s * .52f, p)
        p.shader = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        p.shader = android.graphics.LinearGradient(0f, 0f, w * .25f, h,
            intArrayOf(0xFF68DDF0.toInt(), 0xFF29BFE1.toInt(), 0xFF078FC4.toInt(), 0xFF0875A9.toInt()),
            floatArrayOf(0f, .36f, .72f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, p)
        p.shader = RadialGradient(w * .76f, h * .12f, w * .72f,
            intArrayOf(0x99FFFFFF.toInt(), 0x44D9FFFF, 0x00FFFFFF),
            floatArrayOf(0f, .48f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, p); p.shader = null

        cloud(canvas, w * .18f, h * .19f, w * .105f, 0x66)
        cloud(canvas, w * .86f, h * .34f, w * .085f, 0x55)
        cloud(canvas, w * .50f, h * .08f, w * .06f, 0x44)

        shape(canvas, 0xFF73D69A.toInt()) {
            moveTo(0f, h * .68f)
            cubicTo(w * .17f, h * .56f, w * .30f, h * .61f, w * .45f, h * .69f)
            cubicTo(w * .66f, h * .80f, w * .82f, h * .58f, w, h * .65f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        shape(canvas, 0xFF36B979.toInt()) {
            moveTo(0f, h * .78f)
            cubicTo(w * .20f, h * .67f, w * .36f, h * .82f, w * .56f, h * .77f)
            cubicTo(w * .74f, h * .72f, w * .86f, h * .69f, w, h * .75f)
            lineTo(w, h); lineTo(0f, h); close()
        }

        p.shader = android.graphics.LinearGradient(0f, h * .79f, 0f, h,
            intArrayOf(0xCC39C9E7.toInt(), 0xDD159BCB.toInt(), 0xFF0875A8.toInt()), null, Shader.TileMode.CLAMP)
        val water = Path().apply {
            moveTo(0f, h * .81f)
            cubicTo(w * .22f, h * .75f, w * .43f, h * .86f, w * .63f, h * .80f)
            cubicTo(w * .80f, h * .75f, w * .91f, h * .78f, w, h * .76f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        canvas.drawPath(water, p); p.shader = null

        p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND
        for (i in 0..8) {
            val y = h * (.84f + i * .018f)
            p.strokeWidth = (1.2f + (i % 3)) * d
            p.color = if (i % 2 == 0) 0x55FFFFFF else 0x338BFAFF
            canvas.drawLine(w * (.08f + (i % 3) * .12f), y, w * (.50f + (i % 4) * .10f), y, p)
        }
        p.style = Paint.Style.FILL

        val bubbles = listOf(
            floatArrayOf(.91f, .16f, .055f), floatArrayOf(.08f, .47f, .034f),
            floatArrayOf(.84f, .53f, .043f), floatArrayOf(.22f, .88f, .052f),
            floatArrayOf(.94f, .91f, .025f), floatArrayOf(.57f, .28f, .020f),
            floatArrayOf(.39f, .60f, .026f)
        )
        bubbles.forEach { b ->
            val x = w * b[0]; val y = h * b[1]; val r = w * b[2]
            p.shader = RadialGradient(x - r * .30f, y - r * .38f, r * 1.35f,
                intArrayOf(0x88FFFFFF.toInt(), 0x33DFFFFF, 0x1179D8F0),
                floatArrayOf(0f, .52f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, r, p); p.shader = null
            p.style = Paint.Style.STROKE; p.strokeWidth = maxOf(1f, d * 1.15f)
            p.color = 0x77FFFFFF; canvas.drawCircle(x, y, r, p)
            p.style = Paint.Style.FILL; p.color = 0x77FFFFFF
            canvas.drawOval(x - r * .52f, y - r * .62f, x + r * .08f, y - r * .43f, p)
        }

        p.style = Paint.Style.STROKE; p.strokeWidth = 18f * d; p.color = 0x18FFFFFF
        p.strokeCap = Paint.Cap.ROUND
        val ribbon = Path().apply {
            moveTo(-w * .10f, h * .42f)
            cubicTo(w * .20f, h * .32f, w * .38f, h * .49f, w * .70f, h * .37f)
            cubicTo(w * .88f, h * .30f, w * .98f, h * .34f, w * 1.12f, h * .27f)
        }
        canvas.drawPath(ribbon, p)
        p.style = Paint.Style.FILL; p.shader = null
    }
}
