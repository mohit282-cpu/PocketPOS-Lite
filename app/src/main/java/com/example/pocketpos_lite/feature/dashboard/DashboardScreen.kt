package com.example.pocketpos_lite.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pocketpos_lite.feature.auth.AuthViewModel

import com.example.pocketpos_lite.domain.model.Sale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        authViewModel.event.collect { event ->
            if (event is AuthViewModel.AuthEvent.Logout) {
                onLogout()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(
                            title = "Today's Sales",
                            value = "$${uiState.stats.todaySales}",
                            icon = Icons.Default.AttachMoney,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Transactions",
                            value = "${uiState.stats.todayTransactions}",
                            icon = Icons.Default.Receipt,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(
                            title = "Total Products",
                            value = "${uiState.stats.totalProducts}",
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Low Stock",
                            value = "${uiState.stats.lowStockProducts}",
                            icon = Icons.Default.Warning,
                            modifier = Modifier.weight(1f),
                            contentColor = if (uiState.stats.lowStockProducts > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                item {
                    Text("Recent Sales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                
                if (uiState.recentSales.isEmpty()) {
                    item {
                        EmptyState("No recent sales found")
                    }
                } else {
                    items(uiState.recentSales) { sale ->
                        SaleItem(sale)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun SaleItem(sale: Sale) {
    ListItem(
        headlineContent = { Text("Sale #${sale.id?.takeLast(6)}") },
        supportingContent = { Text("${sale.created_at?.take(10)}") },
        trailingContent = { Text("$${sale.net_amount}", fontWeight = FontWeight.Bold) }
    )
}
