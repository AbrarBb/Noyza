package com.khatibstudio.noyza.ui.screens.premium

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.khatibstudio.noyza.ui.viewmodel.PremiumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val benefits = listOf(
        "Unlimited history",
        "Advanced reports",
        "Unlimited saved places",
        "Custom activities",
        "Advanced place comparison",
        "Home screen widget",
        "CSV export",
        "No advertisements"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "⭐",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Unlock the full\nNoyza experience",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Understand your environments in more detail.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(24.dp))

            // Benefits list
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                benefit,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Pricing cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PricingCard(
                    title = "Monthly",
                    price = "৳149",
                    period = "/month",
                    isBestValue = false,
                    isSelected = uiState.selectedPlan == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectPlan(0) }
                )
                PricingCard(
                    title = "Yearly",
                    price = "৳999",
                    period = "/year",
                    isBestValue = true,
                    isSelected = uiState.selectedPlan == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectPlan(1) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Lifetime option
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                onClick = { viewModel.selectPlan(2) },
                border = if (uiState.selectedPlan == 2)
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lifetime", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text("One-time purchase", style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ))
                    }
                    Text("৳1,999", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Remove ads only option
            TextButton(onClick = { viewModel.purchaseRemoveAds() }) {
                Text("Just remove ads for ৳249", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))
        }

        // Bottom CTA (above nav insets)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { viewModel.startPurchase() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isPurchasing
            ) {
                if (uiState.isPurchasing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Start Premium", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { viewModel.restorePurchases() }) {
                    Text("Restore Purchases", color = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                "No hidden fees. Cancel anytime.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    period: String,
    isBestValue: Boolean,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Box(modifier = modifier) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            onClick = onClick,
            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
                Text(
                    price,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(period, style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ))
            }
        }

        if (isBestValue) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-12).dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "BEST VALUE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
