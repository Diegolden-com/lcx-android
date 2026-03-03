package com.cleanx.lcx.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Generic placeholder screen used for tabs that are not yet implemented.
 * Shows a centered icon, title, and subtitle so testers know the tab is
 * wired up but feature work is pending.
 */
@Composable
private fun PlaceholderScreen(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Concrete placeholder screens ────────────────────────────────────

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.Home,
        title = "Inicio",
        subtitle = "Panel principal (proximamente)",
        modifier = modifier,
    )
}

@Composable
fun WaterScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.WaterDrop,
        title = "Agua",
        subtitle = "Modulo de agua (proximamente)",
        modifier = modifier,
    )
}

@Composable
fun ChecklistScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        icon = Icons.Filled.Checklist,
        title = "Checklist",
        subtitle = "Checklists del dia (proximamente)",
        modifier = modifier,
    )
}

