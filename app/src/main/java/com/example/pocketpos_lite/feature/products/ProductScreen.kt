package com.example.pocketpos_lite.feature.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    modifier: Modifier = Modifier,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Products") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.products) { product ->
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = { Text("Stock: ${product.stock_quantity}") },
                        trailingContent = { Text("$${product.price}") }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, stock ->
                viewModel.addProduct(name, price, stock)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Initial Stock") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(name, price.toDoubleOrNull() ?: 0.0, stock.toDoubleOrNull() ?: 0.0)
            }) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
