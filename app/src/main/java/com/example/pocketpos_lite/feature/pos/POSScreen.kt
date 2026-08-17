package com.example.pocketpos_lite.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketpos_lite.domain.model.Customer
import com.example.pocketpos_lite.domain.model.Product
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun POSScreen(
    modifier: Modifier = Modifier,
    viewModel: POSViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerMenu by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is POSViewModel.POSEvent.SaleCompleted -> {
                    snackbarHostState.showSnackbar("Sale Completed Successfully!")
                }
                is POSViewModel.POSEvent.BarcodeScanned -> {
                    snackbarHostState.showSnackbar("Added '${event.productName}' via Barcode Scan")
                }
                is POSViewModel.POSEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header Bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cashier POS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Customer Selector Dropdown
                        Box {
                            AssistChip(
                                onClick = { showCustomerMenu = true },
                                label = {
                                    Text(
                                        text = uiState.selectedCustomer?.name ?: "Customer: Walk-in",
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            )

                            DropdownMenu(
                                expanded = showCustomerMenu,
                                onDismissRequest = { showCustomerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Walk-in Customer") },
                                    onClick = {
                                        viewModel.selectCustomer(null)
                                        showCustomerMenu = false
                                    }
                                )
                                HorizontalDivider()
                                uiState.customers.forEach { customer ->
                                    DropdownMenuItem(
                                        text = { Text(customer.name) },
                                        onClick = {
                                            viewModel.selectCustomer(customer)
                                            showCustomerMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { viewModel.loadInitialData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Products")
                        }
                    }
                }
            }

            // Error Banner if present
            uiState.error?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Main Split Content
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Search & Product Catalog Grid
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text("Search product name / SKU / Scan Barcode") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            } else {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            }
                        },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (uiState.searchQuery.isBlank()) "No active products found" else "No matching products",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.searchResults, key = { it.id ?: it.name }) { product ->
                                ProductGridCard(
                                    product = product,
                                    onAdd = { viewModel.addToCart(product) }
                                )
                            }
                        }
                    }
                }

                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

                // Right Column: Cart Panel & Summary
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cart (${uiState.totalItemCount})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.cartItems.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearCart() }) {
                                Text("Clear Cart", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Cart is empty",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Tap products on the left to add items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.cartItems, key = { it.product.id ?: it.product.name }) { item ->
                                CartItemRow(
                                    item = item,
                                    onQuantityChange = { viewModel.updateQuantity(item.product, it) },
                                    onRemove = { viewModel.removeFromCart(item.product) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Summary & Checkout CTA
                    SummarySection(
                        subtotal = uiState.subtotal,
                        discount = uiState.discountAmount,
                        total = uiState.total,
                        cartEmpty = uiState.cartItems.isEmpty(),
                        onApplyDiscountClick = { showDiscountDialog = true },
                        onCheckout = { showCheckoutDialog = true }
                    )
                }
            }
        }
    }

    // Discount Modal
    if (showDiscountDialog) {
        DiscountDialog(
            currentDiscount = uiState.discountAmount,
            subtotal = uiState.subtotal,
            onDismiss = { showDiscountDialog = false },
            onConfirm = { amount ->
                viewModel.setDiscount(amount)
                showDiscountDialog = false
            }
        )
    }

    // Checkout Modal
    if (showCheckoutDialog) {
        CheckoutDialog(
            uiState = uiState,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = {
                viewModel.completeSale()
                showCheckoutDialog = false
            },
            onMethodChange = { viewModel.setPaymentMethod(it) },
            onDiscountChange = { viewModel.setDiscount(it) }
        )
    }
}

@Composable
fun ProductGridCard(product: Product, onAdd: () -> Unit) {
    val isOutOfStock = product.stock_quantity <= 0
    val isLowStock = product.stock_quantity in 1.0..5.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock, onClick = onAdd),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOutOfStock) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )

                // Stock Badge
                val badgeColor = when {
                    isOutOfStock -> MaterialTheme.colorScheme.error
                    isLowStock -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
                val badgeText = when {
                    isOutOfStock -> "Out of Stock"
                    isLowStock -> "Low: ${product.stock_quantity.toInt()}"
                    else -> "Stock: ${product.stock_quantity.toInt()}"
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            product.barcode?.let { barcode ->
                Text(
                    text = "Barcode: $barcode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onAdd,
                    enabled = !isOutOfStock
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add to Cart",
                        tint = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val isMaxStock = item.quantity >= item.product.stock_quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.product.price)} each",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "${item.quantity.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FilledTonalIconButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        enabled = !isMaxStock,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.subtotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SummarySection(
    subtotal: Double,
    discount: Double,
    total: Double,
    cartEmpty: Boolean,
    onApplyDiscountClick: () -> Unit,
    onCheckout: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
            Text("$${String.format(Locale.US, "%.2f", subtotal)}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Discount", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onApplyDiscountClick, contentPadding = PaddingValues(0.dp)) {
                    Text(if (discount > 0) "Edit" else "Add", fontSize = 12.sp)
                }
            }
            Text("-$${String.format(Locale.US, "%.2f", discount)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Final Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "$${String.format(Locale.US, "%.2f", total)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCheckout,
            enabled = !cartEmpty,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("COMPLETE SALE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DiscountDialog(
    currentDiscount: Double,
    subtotal: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var discountText by remember { mutableStateOf(if (currentDiscount > 0) currentDiscount.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Discount") },
        text = {
            Column {
                Text("Subtotal: $${String.format(Locale.US, "%.2f", subtotal)}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = discountText,
                    onValueChange = { discountText = it },
                    label = { Text("Discount Amount ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = discountText.toDoubleOrNull() ?: 0.0
                onConfirm(amt)
            }) {
                Text("APPLY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckoutDialog(
    uiState: POSUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onMethodChange: (String) -> Unit,
    onDiscountChange: (Double) -> Unit
) {
    val methods = listOf(
        "cash" to Icons.Default.Money,
        "bank" to Icons.Default.AccountBalance,
        "qr" to Icons.Default.QrCode,
        "card" to Icons.Default.CreditCard,
        "credit" to Icons.Default.Payments
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Sale & Payment")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Amount Due", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "$${String.format(Locale.US, "%.2f", uiState.total)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Customer:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        uiState.selectedCustomer?.name ?: "Walk-in Customer",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                Text("Select Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methods.forEach { (methodKey, icon) ->
                        val isSelected = uiState.paymentMethod.equals(methodKey, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMethodChange(methodKey) },
                            label = { Text(methodKey.uppercase(Locale.US)) },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("CONFIRM PAYMENT")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isProcessing) {
                Text("CANCEL")
            }
        }
    )
}
