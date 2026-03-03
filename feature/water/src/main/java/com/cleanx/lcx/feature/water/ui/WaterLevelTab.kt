package com.cleanx.lcx.feature.water.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cleanx.lcx.core.theme.LcxSpacing
import com.cleanx.lcx.core.ui.LcxButton
import com.cleanx.lcx.core.ui.LcxCard
import com.cleanx.lcx.core.ui.LcxTextField
import com.cleanx.lcx.feature.water.data.TANK_CAPACITY_LITERS
import com.cleanx.lcx.feature.water.data.WATER_PROVIDERS
import com.cleanx.lcx.feature.water.data.WaterProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterLevelTab(
    state: WaterUiState,
    onSliderChange: (Int) -> Unit,
    onPercentageTextChange: (String) -> Unit,
    onLitersTextChange: (String) -> Unit,
    onSaveLevel: () -> Unit,
    onSelectProvider: (String) -> Unit,
    onOrderWater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = LcxSpacing.md, vertical = LcxSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(LcxSpacing.md),
    ) {
        // Critical alert banner
        if (state.isCritical) {
            CriticalAlertBanner()
        }

        // Tank indicator card
        LcxCard(title = "Nivel Actual") {
            WaterTankIndicator(
                percentage = state.inputPercentage,
                liters = state.inputLiters,
                status = state.inputStatus,
            )

            Spacer(modifier = Modifier.height(LcxSpacing.sm))

            // Status text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = statusColor(state.inputStatus)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color),
                )
                Spacer(modifier = Modifier.width(LcxSpacing.sm))
                Text(
                    text = statusLabel(state.inputStatus),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }

            Spacer(modifier = Modifier.height(LcxSpacing.xs))

            // Capacity info
            Text(
                text = "%,d / %,d L".format(state.inputLiters, TANK_CAPACITY_LITERS),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        // Slider card
        LcxCard(title = "Ajustar Nivel") {
            Slider(
                value = state.inputPercentage.toFloat(),
                onValueChange = { onSliderChange(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = statusColor(state.inputStatus),
                    activeTrackColor = statusColor(state.inputStatus),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(LcxSpacing.sm))

            // Manual input fields side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LcxSpacing.sm),
            ) {
                LcxTextField(
                    value = state.percentageText,
                    onValueChange = onPercentageTextChange,
                    label = "Porcentaje (%)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                LcxTextField(
                    value = state.litersText,
                    onValueChange = onLitersTextChange,
                    label = "Litros (L)",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(LcxSpacing.md))

            // Save button
            LcxButton(
                text = "Guardar Nivel",
                onClick = onSaveLevel,
                isLoading = state.isSaving,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            // Success message
            if (state.saveSuccess) {
                Spacer(modifier = Modifier.height(LcxSpacing.sm))
                Text(
                    text = "Nivel guardado correctamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF22C55E),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // Save error message
            if (state.saveError != null) {
                Spacer(modifier = Modifier.height(LcxSpacing.sm))
                Text(
                    text = state.saveError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        // Order water section - only when critical
        if (state.isCritical) {
            OrderWaterCard(
                state = state,
                onSelectProvider = onSelectProvider,
                onOrderWater = onOrderWater,
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(LcxSpacing.lg))
    }
}

@Composable
private fun CriticalAlertBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.1f))
            .padding(LcxSpacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LcxSpacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Alerta",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = "Nivel Critico de Agua",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                )
                Text(
                    text = "El nivel de agua esta por debajo del 20%. Considera pedir agua.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444).copy(alpha = 0.8f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderWaterCard(
    state: WaterUiState,
    onSelectProvider: (String) -> Unit,
    onOrderWater: () -> Unit,
) {
    LcxCard(title = "Pedir Agua") {
        // Provider selector dropdown
        var expanded by remember { mutableStateOf(false) }
        val selectedProvider = state.selectedProvider

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedProvider?.name ?: "Seleccionar proveedor",
                onValueChange = {},
                readOnly = true,
                label = { Text("Proveedor") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                WATER_PROVIDERS.forEach { provider ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = provider.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "$${provider.price} - ${provider.deliveryTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onSelectProvider(provider.id)
                            expanded = false
                        },
                    )
                }
            }
        }

        // Selected provider details
        if (selectedProvider != null) {
            Spacer(modifier = Modifier.height(LcxSpacing.md))
            ProviderDetailCard(provider = selectedProvider)

            Spacer(modifier = Modifier.height(LcxSpacing.md))

            // Call provider button
            LcxButton(
                text = "Llamar Proveedor",
                onClick = onOrderWater,
                isLoading = state.isOrdering,
                enabled = !state.isOrdering,
                modifier = Modifier.fillMaxWidth(),
            )

            // Order success message
            if (state.orderSuccess) {
                Spacer(modifier = Modifier.height(LcxSpacing.sm))
                Text(
                    text = "Pedido registrado correctamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF22C55E),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // Order error
            if (state.orderError != null) {
                Spacer(modifier = Modifier.height(LcxSpacing.sm))
                Text(
                    text = state.orderError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ProviderDetailCard(provider: WaterProvider) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(LcxSpacing.md),
        verticalArrangement = Arrangement.spacedBy(LcxSpacing.xs),
    ) {
        Text(
            text = provider.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Precio:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$${provider.price}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tiempo de entrega:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = provider.deliveryTime,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Calificacion:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = provider.rating.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Telefono:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = provider.phone,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
