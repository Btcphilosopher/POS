package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.POSViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegisterApp(viewModel: POSViewModel) {
    val currentStaff by viewModel.currentStaff.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()

    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                // Step 1: Secure Pin Lock Screen
                currentStaff == null -> {
                    PinLockScreen(onAuthenticate = { pin, onResult ->
                        viewModel.loginWithPin(pin, onResult)
                    })
                }
                // Step 2: Inactive Cashier Shift Session
                activeShift == null -> {
                    InitializeTillScreen(
                        staffName = currentStaff?.name ?: "Cashier",
                        onOpenShift = { startingCash ->
                            viewModel.openShift(startingCash)
                        },
                        onLogout = {
                            viewModel.logoutStaff()
                        }
                    )
                }
                // Step 3: Core Primary Operations
                else -> {
                    POSInterface(viewModel = viewModel, staff = currentStaff!!, shift = activeShift!!)
                }
            }
        }
    }
}

/**
 * 🔐 A security lock pin pad for Cashiers, Admins, and Managers.
 * Tom [1111] | Alice [2222] | Bob [1234]
 */
@Composable
fun PinLockScreen(onAuthenticate: (String, (Boolean) -> Unit) -> Unit) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock Secure",
                tint = IndustrialAmber,
                modifier = Modifier.size(56.dp)
            )

            Text(
                text = "REGISTER SECURE TERMINAL",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TerminalOffWhite,
                letterSpacing = 2.sp
            )

            Text(
                text = "Please enter authorize passcode to unlock till:",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalLightGray,
                textAlign = TextAlign.Center
            )

            // Pin Status Indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                repeat(4) { idx ->
                    val active = idx < pinText.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, IndustrialAmber, RoundedCornerShape(8.dp))
                            .background(if (active) IndustrialAmber else Color.Transparent)
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = RefundRed,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pin Pad Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttons = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                buttons.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { char ->
                            Button(
                                onClick = {
                                    errorMessage = null
                                    when (char) {
                                        "C" -> {
                                            if (pinText.isNotEmpty()) pinText = pinText.dropLast(1)
                                        }
                                        "OK" -> {
                                            if (pinText.length >= 4) {
                                                onAuthenticate(pinText) { success ->
                                                    if (!success) {
                                                        errorMessage = "INVALID PASSCODE. ACCESS DENIED."
                                                        pinText = ""
                                                    }
                                                }
                                            } else {
                                                errorMessage = "PASSCODE MUST BE 4 DIGITS."
                                            }
                                        }
                                        else -> {
                                            if (pinText.length < 4) {
                                                pinText += char
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (char == "OK") ContactlessGreen else if (char == "C") TerminalMediumGray else TerminalDarkGray,
                                    contentColor = if (char == "OK") TerminalBlack else TerminalOffWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .border(
                                        1.dp,
                                        if (char == "OK") ContactlessGreen else TerminalMediumGray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .testTag("pin_btn_$char")
                            ) {
                                Text(
                                    text = char,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic trace sheet bypass guides
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, TerminalMediumGray)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🔓 DIAGNOSTIC BYPASS ACCESS PINS:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SyncTeal
                    )
                    Text(
                        text = "STF-001 (Tom Admin): Pin 1111\nSTF-002 (Alice Mgr): Pin 2222\nSTF-003 (Bob Cashier): Pin 1234",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalLightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * 🧾 Initializer flow to open cash drawer before checkout operations.
 */
@Composable
fun InitializeTillScreen(
    staffName: String,
    onOpenShift: (Double) -> Unit,
    onLogout: () -> Unit
) {
    var startingCashText by remember { mutableStateOf("150.00") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalMediumGray),
            modifier = Modifier.widthIn(max = 450.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = "Shift Cash",
                    tint = IndustrialAmber,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "OPEN REGISTER TILL SESSION",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TerminalOffWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Cashier: $staffName\nSystem State: Offline-Resilient Standby Ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TerminalLightGray,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

                Text(
                    text = "Input starting till cash reserve ($):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalOffWhite
                )

                OutlinedTextField(
                    value = startingCashText,
                    onValueChange = { startingCashText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("starting_cash_input")
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = RefundRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val cash = startingCashText.toDoubleOrNull()
                        if (cash != null && cash >= 0.0) {
                            onOpenShift(cash)
                        } else {
                            errorText = "PLEASE TENDER VALID CASH ENCLOSURE AMOUNT."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = TerminalBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_till_button")
                ) {
                    Text(text = "ACTIVATE DRAWER SESSION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                TextButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.textButtonColors(contentColor = RefundRed)
                ) {
                    Text(text = "Switch Staff Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 📺 Standard operations frame containing navigation tabs to shift modules.
 */
@Composable
fun POSInterface(
    viewModel: POSViewModel,
    staff: StaffUser,
    shift: ShiftSession
) {
    var selectedScreenIndex by remember { mutableStateOf(0) }
    val cart by viewModel.cart.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showActiveCartModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalDarkGray)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IndustrialAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "R",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = TerminalBlack,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 20.sp
                                )
                            }
                            Column {
                                Text(
                                    "REGISTER POS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = TerminalOffWhite,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isOnline) ContactlessGreen else TerminalLightGray)
                                    )
                                    Text(
                                        text = if (isOnline) "CLOUD PROV ONLINE" else "OFFLINE TERM MODE",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 10.sp,
                                        color = if (isOnline) ContactlessGreen else RefundRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Connectivity controls & Operations Switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = SyncTeal,
                                    strokeWidth = 2.dp
                                )
                            }

                            Button(
                                onClick = { viewModel.toggleNetworkMode() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOnline) TerminalMediumGray else RefundRed.copy(alpha = 0.2f),
                                    contentColor = if (isOnline) ContactlessGreen else RefundRed
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .border(
                                        1.dp,
                                        if (isOnline) ContactlessGreen.copy(alpha = 0.5f) else RefundRed,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .testTag("network_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = "Network state",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOnline) "ONLINE" else "DISCONNECT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = TerminalMediumGray),
                                modifier = Modifier
                                    .clickable { viewModel.logoutStaff() }
                                    .border(1.dp, TerminalMediumGray, RoundedCornerShape(6.dp)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Operator",
                                        tint = IndustrialAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${staff.name} (${staff.role})",
                                        fontSize = 11.sp,
                                        color = TerminalOffWhite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = "Logout",
                                        tint = RefundRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            if (cart.isNotEmpty()) {
                                IconButton(
                                    onClick = { showActiveCartModal = true },
                                    modifier = Modifier
                                        .background(IndustrialAmber, RoundedCornerShape(18.dp))
                                        .size(36.dp)
                                        .testTag("floating_cart")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Active order",
                                        tint = TerminalBlack,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = TerminalDarkGray,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier
            ) {
                val screens = listOf(
                    Triple("Checkout", Icons.Default.ShoppingCart, 0),
                    Triple("Inventory", Icons.Default.Inventory, 1),
                    Triple("History", Icons.Default.Receipt, 2),
                    Triple("AI Strategist", Icons.Default.Psychology, 3),
                    Triple("Shift Till", Icons.Default.AccountBalanceWallet, 4)
                )

                screens.forEach { (label, icon, index) ->
                    val selected = selectedScreenIndex == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedScreenIndex = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TerminalBlack,
                            selectedTextColor = IndustrialAmber,
                            indicatorColor = IndustrialAmber,
                            unselectedIconColor = TerminalLightGray,
                            unselectedTextColor = TerminalLightGray
                        ),
                        modifier = Modifier.testTag("nav_item_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TerminalBlack)
        ) {
            when (selectedScreenIndex) {
                0 -> CheckoutContainerScreen(viewModel = viewModel)
                1 -> InventoryModuleScreen(viewModel = viewModel)
                2 -> PaymentsHistoryScreen(viewModel = viewModel)
                3 -> ArtificialIntelligenceScreen(viewModel = viewModel)
                4 -> CashShiftControlScreen(viewModel = viewModel, shift = shift)
            }
        }
    }

    if (showActiveCartModal) {
        Dialog(onDismissRequest = { showActiveCartModal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalMediumGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🛒 ACTIVE CHECKOUT ORDER",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TerminalOffWhite
                        )
                        IconButton(onClick = { showActiveCartModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TerminalLightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        CartDetailsLayout(viewModel = viewModel, onPaymentStarted = {
                            showActiveCartModal = false
                        })
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: CHECKOUT TERMINAL SCREEN ROUTER
// ==========================================
@Composable
fun CheckoutContainerScreen(viewModel: POSViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 840.dp

        if (isTablet) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.weight(1.3f)) {
                    ProductCatalogGrid(viewModel = viewModel)
                }
                VerticalDivider(color = TerminalMediumGray, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(380.dp)
                        .background(TerminalDarkGray)
                ) {
                    CartDetailsLayout(viewModel = viewModel)
                }
            }
        } else {
            ProductCatalogGrid(viewModel = viewModel)
        }
    }
}

@Composable
fun ProductCatalogGrid(viewModel: POSViewModel) {
    val products by viewModel.products.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { p ->
            val matchesCategory = selectedCategory == "ALL" || p.category.uppercase() == selectedCategory.uppercase()
            val matchesQuery = p.name.contains(searchQuery, ignoreCase = true) ||
                    p.sku.contains(searchQuery, ignoreCase = true) ||
                    p.barcode == searchQuery
            matchesCategory && matchesQuery
        }
    }

    LaunchedEffect(searchQuery) {
        val exactMatch = products.firstOrNull { it.barcode == searchQuery }
        if (exactMatch != null) {
            viewModel.addToCart(exactMatch)
            viewModel.setSearchQuery("")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Scan Barcode (try 1001, 1005) or search SKU...", color = TerminalLightGray) },
            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = IndustrialAmber) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TerminalLightGray)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("catalog_search_bar")
        )

        // Categories Header Horizontal Filter Tab
        val categories = listOf("ALL", "BEVERAGES", "BAKERY", "FOOD", "MERCHANDISE", "SUPPLIES", "HARDWARE")
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = IndustrialAmber,
            edgePadding = 0.dp,
            divider = {}
        ) {
            categories.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { viewModel.setCategory(cat) },
                    text = {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCategory == cat) IndustrialAmber else TerminalLightGray
                        )
                    },
                    modifier = Modifier.testTag("tab_$cat")
                )
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Inbox, contentDescription = "No results", tint = TerminalMediumGray, modifier = Modifier.size(64.dp))
                    Text("No catalog item matched filters.", color = TerminalLightGray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { item ->
                    ProductCheckoutCard(product = item, onAdd = { variant ->
                        viewModel.addToCart(item, variant)
                    })
                }
            }
        }
    }
}

@Composable
fun ProductCheckoutCard(product: Product, onAdd: (String?) -> Unit) {
    val lowStock = product.stockQuantity <= product.lowStockThreshold

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAdd(null) }
            .border(
                1.dp,
                if (lowStock) RefundRed.copy(alpha = 0.6f) else TerminalMediumGray,
                RoundedCornerShape(8.dp)
            )
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = IndustrialAmber,
                    modifier = Modifier
                        .background(TerminalMediumGray, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    text = "QTY ${product.stockQuantity}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (lowStock) RefundRed else ContactlessGreen
                )
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TerminalOffWhite,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SKU: ${product.sku.takeLast(7)}",
                    fontSize = 9.sp,
                    color = TerminalLightGray,
                    fontFamily = FontFamily.Monospace
                )

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick add",
                    tint = IndustrialAmber,
                    modifier = Modifier
                        .size(20.dp)
                        .background(TerminalMediumGray, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                )
            }

            if (product.variants.isNotBlank()) {
                val variantList = product.variants.split(",").map { it.trim() }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    variantList.take(2).forEach { variant ->
                        Text(
                            text = variant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalOffWhite,
                            modifier = Modifier
                                .background(TerminalMediumGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp))
                                .clickable { onAdd(variant) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 🛒 Shopping cart line details calculation and payment mode triggers
 */
@Composable
fun CartDetailsLayout(viewModel: POSViewModel, onPaymentStarted: () -> Unit = {}) {
    val cart by viewModel.cart.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val taxRate by viewModel.taxRate.collectAsState()

    val subtotal = cart.sumOf { it.price * it.quantity }
    val totalBeforeTax = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount = totalBeforeTax * (taxRate / 100.0)
    val grandTotal = totalBeforeTax + taxAmount

    var showPaymentEngineModal by remember { mutableStateOf(false) }
    var paymentMethodSelected by remember { mutableStateOf("CARD") }

    var showDiscountDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SALE ORDER ITEMS (${cart.sumOf { it.quantity }})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TerminalOffWhite,
                letterSpacing = 1.sp
            )

            HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No checkout items in cart.",
                        color = TerminalLightGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cart) { item ->
                        CartListItemRow(item = item, onQtyChange = { itemCopy, q ->
                            viewModel.updateCartQty(itemCopy, q)
                        })
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .background(TerminalDarkGray)
                .padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showDiscountDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = SyncTeal)
                ) {
                    Icon(Icons.Default.Discount, contentDescription = "Promo", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (discount > 0.0) "Discount Applied: -$${String.format("%.2f", discount)}" else "Apply Promo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (cart.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearCart() },
                        colors = ButtonDefaults.textButtonColors(contentColor = RefundRed)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Void Active", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BillingLine(label = "Subtotal", valStr = "$${String.format("%.2f", subtotal)}")
                if (discount > 0) {
                    BillingLine(label = "Promo Discount", valStr = "-$${String.format("%.2f", discount)}", isRed = true)
                }
                BillingLine(label = "Tax ($taxRate%)", valStr = "$${String.format("%.2f", taxAmount)}")

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL CHARGE", style = MaterialTheme.typography.titleMedium, color = TerminalOffWhite, fontWeight = FontWeight.Bold)
                    Text(
                        text = "$${String.format("%.2f", grandTotal)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = IndustrialAmber,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (cart.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "TENDER TERMINAL PAYMENT PROTOCOL:",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TerminalLightGray,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                paymentMethodSelected = "CARD"
                                showPaymentEngineModal = true
                                onPaymentStarted()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMediumGray, contentColor = TerminalOffWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_payment_card")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CreditCard, contentDescription = "Card", modifier = Modifier.size(16.dp))
                                Text("NFC/CARD", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                paymentMethodSelected = "CASH"
                                showPaymentEngineModal = true
                                onPaymentStarted()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMediumGray, contentColor = TerminalOffWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_payment_cash")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = "Cash", modifier = Modifier.size(16.dp))
                                Text("CASH LOG", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                paymentMethodSelected = "BITCOIN"
                                showPaymentEngineModal = true
                                onPaymentStarted()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMediumGray, contentColor = TerminalOffWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_payment_btc")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CurrencyExchange, contentDescription = "BTC", modifier = Modifier.size(16.dp))
                                Text("LIGHTNING", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDiscountDialog) {
        var discountInputText by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showDiscountDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalMediumGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "APPLY INVOICE PROMO DISCOUNT ($)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TerminalOffWhite
                    )

                    OutlinedTextField(
                        value = discountInputText,
                        onValueChange = { discountInputText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.testTag("discount_modal_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDiscountDialog = false }) {
                            Text("Cancel", color = TerminalLightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val dVal = discountInputText.toDoubleOrNull() ?: 0.0
                                viewModel.applyDiscount(dVal)
                                showDiscountDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = TerminalBlack)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showPaymentEngineModal) {
        PaymentEngineModal(
            viewModel = viewModel,
            paymentMethod = paymentMethodSelected,
            grandTotal = grandTotal,
            onDismiss = { showPaymentEngineModal = false }
        )
    }
}

@Composable
fun CartListItemRow(item: CartProduct, onQtyChange: (CartProduct, Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalBlack, RoundedCornerShape(6.dp))
            .border(1.dp, TerminalMediumGray, RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                color = TerminalOffWhite,
                fontSize = 13.sp
            )
            if (item.selectedVariant != null) {
                Text(
                    text = "SPEC: ${item.selectedVariant}",
                    fontSize = 10.sp,
                    color = SyncTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "$${String.format("%.2f", item.price)} each",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TerminalLightGray
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onQtyChange(item, item.quantity - 1) },
                modifier = Modifier
                    .size(28.dp)
                    .background(TerminalDarkGray, RoundedCornerShape(4.dp))
                    .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Deduct", tint = TerminalOffWhite, modifier = Modifier.size(12.dp))
            }

            Text(
                text = "${item.quantity}",
                color = TerminalOffWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            IconButton(
                onClick = { onQtyChange(item, item.quantity + 1) },
                modifier = Modifier
                    .size(28.dp)
                    .background(TerminalDarkGray, RoundedCornerShape(4.dp))
                    .border(1.dp, TerminalMediumGray, RoundedCornerShape(4.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = TerminalOffWhite, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun BillingLine(label: String, valStr: String, isRed: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = TerminalLightGray)
        Text(
            text = valStr,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (isRed) RefundRed else TerminalOffWhite
        )
    }
}

// ==========================================
// SCREEN 2: DYNAMIC PAYMENTS ENGINE
// ==========================================
@Composable
fun PaymentEngineModal(
    viewModel: POSViewModel,
    paymentMethod: String,
    grandTotal: Double,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var cashTenderedVal by remember { mutableStateOf(String.format("%.2f", grandTotal)) }
    var calculatedChange by remember { mutableStateOf(0.0) }

    val selectedTransaction by viewModel.selectedTransaction.collectAsState()
    var showTapesAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(step) {
        if (step == 1) {
            kotlinx.coroutines.delay(2200)
            viewModel.checkoutCart(paymentMethod) { success ->
                if (success) {
                    step = 2
                } else {
                    onDismiss()
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (step == 0 || step == 3) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalMediumGray),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    0 -> {
                        Text(
                            text = when (paymentMethod) {
                                "CARD" -> "💳 READ TACTILE CHIP READER"
                                "CASH" -> "💵 REGISTER CASH DRAWER LOG"
                                else -> "⚡ BITCOIN LIGHTNING INSTANT LAYER"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TerminalOffWhite
                        )

                        HorizontalDivider(color = TerminalMediumGray)

                        if (paymentMethod == "CASH") {
                            Text("Please tender cashier physical intake amount ($):", color = TerminalLightGray)
                            OutlinedTextField(
                                value = cashTenderedVal,
                                onValueChange = {
                                    cashTenderedVal = it
                                    val tendered = it.toDoubleOrNull() ?: 0.0
                                    calculatedChange = (tendered - grandTotal).coerceAtLeast(0.0)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cash_tendered_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CHANGE TO REFUND:", style = MaterialTheme.typography.bodyMedium, color = TerminalLightGray)
                                Text(
                                    "$${String.format("%.2f", calculatedChange)}",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ContactlessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (paymentMethod == "CARD") {
                            IconButton(
                                onClick = { step = 1 },
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(IndustrialAmber.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                                    .border(2.dp, IndustrialAmber, RoundedCornerShape(50.dp))
                            ) {
                                Icon(Icons.Default.Nfc, contentDescription = "Hold device close", tint = IndustrialAmber, modifier = Modifier.size(48.dp))
                            }
                            Text("TAP / CONTACTLESS OR HOLD REGISTER CARD CLOSE", color = TerminalLightGray, textAlign = TextAlign.Center, fontSize = 12.sp)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .background(Color.White)
                                    .border(4.dp, IndustrialAmber)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Contactless QR code scanner",
                                    tint = TerminalBlack,
                                    modifier = Modifier.size(114.dp)
                                )
                            }
                            Text(
                                "lnbc490n1p38x... Lightning Network Cash Register Invoice",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SyncTeal,
                                maxLines = 1,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = { step = 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = ContactlessGreen, contentColor = TerminalBlack),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_confirm_payment")
                        ) {
                            Text(
                                text = "AUTHORIZE CHARGE $${String.format("%.2f", grandTotal)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    1 -> {
                        Text("AUTHENTICATING PROTOCOL SECURE...", style = MaterialTheme.typography.titleMedium, color = TerminalOffWhite)
                        CircularProgressIndicator(color = IndustrialAmber, modifier = Modifier.size(56.dp))
                        Text(
                            text = "Link terminal to Cloud payment gateway over simulated local network. Stay close...",
                            fontSize = 11.sp,
                            color = TerminalLightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                    2 -> {
                        Text("📠 HARDWARE TELE-PRINT IN PROGRESS", style = MaterialTheme.typography.titleMedium, color = SyncTeal)

                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(10)
                            showTapesAnimation = true
                        }

                        AnimatedVisibility(
                            visible = showTapesAnimation,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
                        ) {
                            if (selectedTransaction != null) {
                                ReceiptTapeDisplay(selectedTransaction!!)
                            }
                        }

                        Button(
                            onClick = { step = 3 },
                            colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = TerminalBlack),
                            modifier = Modifier.fillMaxWidth().testTag("btn_complete_checkout")
                        ) {
                            Text("TAPE RECEIVED • COMPLETE CHECKOUT", fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        Icon(Icons.Default.TaskAlt, contentDescription = "Done", tint = ContactlessGreen, modifier = Modifier.size(56.dp))
                        Text("TRANSACTION AUTHORIZED SUCCESSFUL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ContactlessGreen)
                        Text("Receipt has been successfully archived into SQL. Inventory level reduced.", textAlign = TextAlign.Center, color = TerminalLightGray)

                        Button(
                            onClick = { onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMediumGray, contentColor = TerminalOffWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_exit_checkout")
                        ) {
                            Text("EXIT TERMINAL SCREEN")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📠 A beautiful mockup of physical scrolling invoice Bluetooth receipts thermal printer
 */
@Composable
fun ReceiptTapeDisplay(txn: Transaction) {
    val itemsList = remember(txn) {
        try {
            val m = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val t = Types.newParameterizedType(List::class.java, CartProduct::class.java)
            m.adapter<List<CartProduct>>(t).fromJson(txn.itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, TerminalMediumGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("--- REGISTER LOCAL RECEIPT ---", fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Terminal till id: #01C8", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 10.sp)
            Text(
                "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(txn.timestamp))}",
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text("Auth operator: ${txn.staffName}", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 10.sp)

            Text("================================", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 10.sp)

            itemsList.forEach { p ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${p.quantity}x ${p.name.take(15)}", fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = 11.sp)
                    Text("$${String.format("%.2f", p.price * p.quantity)}", fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = 11.sp)
                }
            }

            Text("--------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 10.sp)

            BillingPrnRow("Discount applied", "-$${String.format("%.2f", txn.discountAmount)}")
            BillingPrnRow("VAT TAX calculated", "$${String.format("%.2f", txn.taxAmount)}")
            BillingPrnRow("TOTAL AMOUNT PAID", "$${String.format("%.2f", txn.totalAmount)}", isBig = true)

            Text("================================", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 10.sp)

            Text("Mode: ${txn.paymentMethod} (AUTHORIZED)", fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Status: ${if (txn.isOffline) "OFFLINE QUEUE SYSTEM" else "CLOUD VERIFIED"}", fontFamily = FontFamily.Monospace, color = Color.DarkGray, fontSize = 10.sp)

            Spacer(modifier = Modifier.height(8.dp))
            Text("THANK YOU FOR YOUR PATRONAGE!", fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Powered by AI Studio Build", fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 9.sp)
        }
    }
}

@Composable
fun BillingPrnRow(label: String, valStr: String, isBig: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = if (isBig) 12.sp else 10.sp, fontWeight = if (isBig) FontWeight.Bold else FontWeight.Normal)
        Text(valStr, fontFamily = FontFamily.Monospace, color = Color.Black, fontSize = if (isBig) 12.sp else 10.sp, fontWeight = if (isBig) FontWeight.Bold else FontWeight.Normal)
    }
}

// ==========================================
// SCREEN 3: PRODUCT & INVENTORY SYSTEM
// ==========================================
@Composable
fun InventoryModuleScreen(viewModel: POSViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddNewDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "📦 INVENTORY MANAGEMENT",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TerminalOffWhite
                )
                Text("Database lists ${products.size} active product SKUs.", color = TerminalLightGray, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = { showAddNewDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = TerminalBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_trigger_add_product")
            ) {
                Icon(Icons.Default.AddBox, contentDescription = "Add Product", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REGISTER SKU", fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { item ->
                InventoryLineRow(item = item, onStockChange = { newStock ->
                    viewModel.updateProductStock(item, newStock)
                }, onDelete = {
                    viewModel.deleteProduct(item)
                })
            }
        }
    }

    if (showAddNewDialog) {
        AddProductDialog(
            onDismiss = { showAddNewDialog = false },
            onAdd = { n, p, c, s, cat, b ->
                viewModel.addProduct(n, p, c, s, cat, b)
                showAddNewDialog = false
            }
        )
    }
}

@Composable
fun InventoryLineRow(
    item: Product,
    onStockChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val lowStock = item.stockQuantity <= item.lowStockThreshold

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, if (lowStock) RefundRed.copy(alpha = 0.6f) else TerminalMediumGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = TerminalOffWhite,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.uppercase(),
                        color = SyncTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• SKU: ${item.sku}",
                        color = TerminalLightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Sell: $${String.format("%.2f", item.price)}", color = IndustrialAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Cost: $${String.format("%.2f", item.costPrice)}", color = TerminalLightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }

                if (lowStock) {
                    Text(
                        "⚠️ LOW STOCK CONFLICT: ORDER IMMEDIATELY",
                        color = RefundRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RefundRed.copy(alpha = 0.5f))
                }

                IconButton(
                    onClick = { onStockChange(item.stockQuantity - 1) },
                    modifier = Modifier
                        .background(TerminalMediumGray, RoundedCornerShape(4.dp))
                        .size(34.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Deduct Stock", tint = TerminalOffWhite)
                }

                Text(
                    text = "${item.stockQuantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (lowStock) RefundRed else ContactlessGreen,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = { onStockChange(item.stockQuantity + 1) },
                    modifier = Modifier
                        .background(TerminalMediumGray, RoundedCornerShape(4.dp))
                        .size(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Stock", tint = TerminalOffWhite)
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(onDismiss: () -> Unit, onAdd: (String, Double, Double, Int, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var stockStr by remember { mutableStateOf("20") }
    var category by remember { mutableStateOf("Beverages") }
    var barcode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalMediumGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "📝 REGISTER NEW RETAIL SKU",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TerminalOffWhite
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name", color = TerminalLightGray) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price ($)", color = TerminalLightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Cost Price ($)", color = TerminalLightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Level", color = TerminalLightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode Code", color = TerminalLightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Category Select:", color = TerminalOffWhite, style = MaterialTheme.typography.titleMedium)

                val categories = listOf("Beverages", "Bakery", "Food", "Merchandise", "Supplies", "Hardware")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndustrialAmber,
                                selectedLabelColor = TerminalBlack,
                                containerColor = TerminalMediumGray,
                                labelColor = TerminalOffWhite
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TerminalLightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val cost = costStr.toDoubleOrNull() ?: (price * 0.40)
                            val stock = stockStr.toIntOrNull() ?: 0
                            if (name.isNotBlank()) {
                                onAdd(name, price, cost, stock, category, barcode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = TerminalBlack)
                    ) {
                        Text("Register SKU", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: TRANSACTIONS HISTORY ARCHIVE
// ==========================================
@Composable
fun PaymentsHistoryScreen(viewModel: POSViewModel) {
    val txns by viewModel.transactions.collectAsState()
    val selectedTxn by viewModel.selectedTransaction.collectAsState()

    var showReceiptDetailModal by remember { mutableStateOf(false) }
    val syncLogs by viewModel.syncLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "🗃️ HISTORIC AUDIT ARCHIVES",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TerminalOffWhite
                )
                Text("Room holds ${txns.size} authorized checkout transactions.", color = TerminalLightGray, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalBlack),
            border = BorderStroke(1.dp, TerminalMediumGray),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("TERMINAL SYSTEM FEED LOGS: (Offline-First State Monitor)", fontSize = 10.sp, color = SyncTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(syncLogs) { log ->
                        Text(log, style = MaterialTheme.typography.bodyMedium, color = TerminalOffWhite)
                    }
                }
            }
        }

        if (txns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No transaction logs locked in sqlite yet.", color = TerminalLightGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(txns) { t ->
                    TransactionItemRow(txn = t, onClick = {
                        viewModel.selectTransactionReceipt(t)
                        showReceiptDetailModal = true
                    })
                }
            }
        }
    }

    if (showReceiptDetailModal && selectedTxn != null) {
        ReceiptDetailsModal(
            txn = selectedTxn!!,
            onDismiss = {
                viewModel.selectTransactionReceipt(null)
                showReceiptDetailModal = false
            },
            onRefund = {
                viewModel.triggerRefund(selectedTxn!!)
            }
        )
    }
}

@Composable
fun TransactionItemRow(txn: Transaction, onClick: () -> Unit) {
    val isRefunded = txn.paymentStatus == "REFUNDED"

    Card(
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, TerminalMediumGray),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = txn.transactionId,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRefunded) RefundRed else TerminalOffWhite
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(txn.timestamp)),
                        fontSize = 11.sp,
                        color = TerminalLightGray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• ${txn.paymentMethod}",
                        fontSize = 11.sp,
                        color = TerminalLightGray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${txn.staffName}",
                        fontSize = 11.sp,
                        color = TerminalLightGray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (txn.synced) ContactlessGreen.copy(alpha = 0.2f) else RefundRed.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (txn.synced) "CLOUD SYNCHED" else "QUEUED LOCAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (txn.synced) ContactlessGreen else RefundRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (isRefunded) {
                        Box(
                            modifier = Modifier
                                .background(RefundRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("VOID RECLAIMED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RefundRed, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Text(
                text = "$${String.format("%.2f", txn.totalAmount)}",
                style = MaterialTheme.typography.titleLarge,
                color = if (isRefunded) RefundRed else IndustrialAmber,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Detailed modal showing receipt with reprint / refund action sheets
 */
@Composable
fun ReceiptDetailsModal(
    txn: Transaction,
    onDismiss: () -> Unit,
    onRefund: () -> Unit
) {
    var showDigitalSuccessBanner by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalMediumGray),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋 INVOICE ARCHIVE RECORD", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TerminalOffWhite)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TerminalLightGray)
                        }
                    }

                    HorizontalDivider(color = TerminalMediumGray)

                    ReceiptTapeDisplay(txn)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(
                        modifier = Modifier
                            .background(TerminalBlack, RoundedCornerShape(6.dp))
                            .border(1.dp, TerminalMediumGray, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📡 TRANSMIT RECEIPT CHANNELS:", fontSize = 10.sp, color = SyncTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Customer Email (e.g. tom@ahyx.org)", color = TerminalLightGray, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )

                        Button(
                            onClick = {
                                if (emailInput.isNotBlank()) {
                                    showDigitalSuccessBanner = true
                                    emailInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalMediumGray, contentColor = TerminalOffWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("TRANSMIT DIGITAL RECORD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (showDigitalSuccessBanner) {
                            Text("⚡ LEDGER CONFLICT RESOLVED: Receipt synced and emailed.", color = ContactlessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (txn.paymentStatus != "REFUNDED") {
                        Button(
                            onClick = {
                                onRefund()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RefundRed, contentColor = TerminalOffWhite),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_trigger_refund")
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Refund", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VOID & REFUND TRANSACTION (RESTORE STOCK)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalMediumGray, contentColor = TerminalOffWhite),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("CLOSE AUDITING INDEX")
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: ARTIFICIAL INTELLIGENCE STRATEGIST
// ==========================================
@Composable
fun ArtificialIntelligenceScreen(viewModel: POSViewModel) {
    val aiInsight by viewModel.aiInsight.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    var customPrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(
                "🧠 SALES INTELLIGENCE SYSTEM",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TerminalOffWhite
            )
            Text(
                "Coordinators Gemini LLM with Room logs to identify stock velocities & season reorders.",
                color = TerminalLightGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

        Text("SELECT ADVISORY TASK TEMPLATE:", fontSize = 10.sp, color = SyncTeal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "Audit stock velocities & low items",
                "What items should I reorder first?",
                "Suggest loyalty reward models",
                "How do I boost my average ticket price?"
            )

            suggestions.forEach { label ->
                FilterChip(
                    selected = customPrompt == label,
                    onClick = {
                        customPrompt = label
                        viewModel.fetchAiInsights(label)
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialAmber,
                        selectedLabelColor = TerminalBlack,
                        containerColor = TerminalDarkGray,
                        labelColor = TerminalOffWhite
                    ),
                    modifier = Modifier.testTag("ai_chip_${label.take(10)}")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                placeholder = { Text("Ask register intelligence custom merchant strategies...", fontSize = 12.sp, color = TerminalLightGray) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("ai_custom_input")
            )

            Button(
                onClick = { viewModel.fetchAiInsights(customPrompt) },
                colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = TerminalBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("btn_trigger_ai")
            ) {
                Text("PROMPT", fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalMediumGray),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (aiLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = IndustrialAmber, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "QUERYING RECON PROTOCOL GEMINI-3.5-FLASH...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalOffWhite
                        )
                    }
                } else if (aiInsight != null) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Terminal, contentDescription = "Terminal Output", tint = SyncTeal, modifier = Modifier.size(16.dp))
                            Text("INTELLIGENCE AUDIT RESPONSE FEED:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SyncTeal)
                        }
                        HorizontalDivider(color = TerminalMediumGray, modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = aiInsight!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TerminalOffWhite,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Waiting", tint = TerminalMediumGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Standing by for analytics commands. Choose an audit query above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalLightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 6: CASH TILL & SHIFT OPERATIONS
// ==========================================
@Composable
fun CashShiftControlScreen(viewModel: POSViewModel, shift: ShiftSession) {
    var endingTillCashText by remember { mutableStateOf("") }
    val cumulativeSalesInCurrent = shift.totalSales

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                "💼 TILL SESSIONS & CASH CONTROLS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TerminalOffWhite
            )
            Text("Audits active cash drawer reserves with historic shift handovers.", color = TerminalLightGray, style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider(color = TerminalMediumGray, thickness = 1.dp)

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, ContactlessGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🟢 ACTIVE TILL SESSION: SECURE STATUS",
                        style = MaterialTheme.typography.titleMedium,
                        color = ContactlessGreen,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.LockOpen, contentDescription = "Lock state", tint = ContactlessGreen)
                    }
                }

                BillingLine(label = "Responsible Cashier", valStr = shift.staffName)
                BillingLine(
                    label = "Session Start Time",
                    valStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(shift.startTime))
                )
                BillingLine(label = "Starting Till Float ($)", valStr = "$${String.format("%.2f", shift.startingCash)}")
                BillingLine(label = "Sales intake counter ($)", valStr = "$${String.format("%.2f", cumulativeSalesInCurrent)}")

                HorizontalDivider(color = TerminalMediumGray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ESTIMATED ON-HAND TILL BALANCE:", style = MaterialTheme.typography.titleMedium, color = TerminalOffWhite)
                    Text(
                        text = "$${String.format("%.2f", shift.startingCash + cumulativeSalesInCurrent)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = ContactlessGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalMediumGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔐 HANDOVER LOCK • VERIFY TILL DETAILS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TerminalOffWhite)

                OutlinedTextField(
                    value = endingTillCashText,
                    onValueChange = { endingTillCashText = it },
                    label = { Text("Tender ending counted cash in drawer ($)", color = TerminalLightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ending_till_cash_input")
                )

                Button(
                    onClick = {
                        val endCash = endingTillCashText.toDoubleOrNull() ?: 0.0
                        viewModel.closeShift(endCash)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RefundRed, contentColor = TerminalOffWhite),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_close_shift")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = "Close Shift")
                    Spacer(modifier = Modifier.width(6.6.dp))
                    Text("VOID SHIFT DRAWER & SYNC OUT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
