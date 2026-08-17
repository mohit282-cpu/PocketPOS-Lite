package com.example.pocketpos_lite.feature.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketpos_lite.domain.model.Product

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun POSScreen(
    modifier: Modifier = Modifier,
    viewModel: POSViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is POSViewModel.POSEvent.SaleCompleted) {
                searchQuery = ""
                snackbarHostState.showSnackbar("Sale Completed: ${event.saleId}")
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Left Column: Product Search and Results
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchProducts(it)
                    },
                    label = { Text("Search Products / Scan Barcode") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchResults) { product ->
                        ProductItem(product = product, onAdd = { viewModel.addToCart(product) })
                    }
                    
                    if (uiState.searchResults.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            Text("No products found", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }

            VerticalDivider()

            // Right Column: Cart and Summary
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Text("Cart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.cartItems) { item ->
                        CartRow(
                            item = item,
                            onQuantityChange = { viewModel.updateQuantity(item.product, it) },
                            onRemove = { viewModel.removeFromCart(item.product) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                SummarySection(
                    subtotal = uiState.subtotal,
                    discount = uiState.discountAmount,
                    total = uiState.total,
                    onCheckout = { showCheckoutDialog = true }
                )
            }
        }
    }

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
fun ProductItem(product: Product, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAdd
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = "SKU: ${product.sku ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "$${product.price}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CartRow(item: CartItem, onQuantityChange: (Double) -> Unit, onRemove: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = item.product.name, modifier = Modifier.weight(1f), maxLines = 1)
            Text(text = "$${item.subtotal}", fontWeight = FontWeight.Bold)
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = { onQuantityChange(item.quantity - 1) }, label = { Text("-") })
            Text(text = "${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
            AssistChip(onClick = { onQuantityChange(item.quantity + 1) }, label = { Text("+") })
        }
    }
}

@Composable
fun SummarySection(subtotal: Double, discount: Double, total: Double, onCheckout: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal")
            Text("$${subtotal}")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Discount")
            Text("-$${discount}")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("$${total}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("CHECKOUT", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
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
    var discountInput by remember { mutableStateOf(uiState.discountAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Sale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Total: $${uiState.total}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = {
                        discountInput = it
                        onDiscountChange(it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Apply Discount") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment Method", style = MaterialTheme.typography.labelLarge)
                val methods = listOf("cash", "bank", "qr", "card", "credit")
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { method ->
                        FilterChip(
                            selected = uiState.paymentMethod == method,
                            onClick = { onMethodChange(method) },
                            label = { Text(method.uppercase()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !uiState.isProcessing) {
                if (uiState.isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("CONFIRM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
