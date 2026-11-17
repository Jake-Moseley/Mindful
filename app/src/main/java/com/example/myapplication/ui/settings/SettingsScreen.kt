package com.example.myapplication.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.myapplication.data.security.PasscodeManager
import com.example.myapplication.ui.home.HomeLayoutViewModel
import com.example.myapplication.ui.home.WidgetSlot

private object SettingsColors {
    val Background = Color(0xFFFFFDF9)
    val Card = Color.White
    val Border = Color(0xFFECE7E1)
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF6B6B6B)
    val IconBadge = Color(0xFFDDF4F8)
    val OnGreen = Color(0xFF1E6B3A)
    val GreenBg = Color(0xFFD7F2DF)
    val OffBg = Color(0xFFF7F3EE)
    val Destructive = Color(0xFFB3261E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, layoutViewModel: HomeLayoutViewModel) {
    val context = LocalContext.current
    val passcodeManager = remember { PasscodeManager(context) }
    var passcodeOn by remember { mutableStateOf(passcodeManager.isPasscodeEnabled()) }

    // Re-check when coming back from setting up a PIN
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) passcodeOn = passcodeManager.isPasscodeEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = SettingsColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = SettingsColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SettingsColors.TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            layoutViewModel.isEditing = true
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsColors.TextPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit home", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SettingsColors.Background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("Security") }
            item {
                PasscodeCard(
                    enabled = passcodeOn,
                    onSetUp = { navController.navigate("passcode_setup") },
                    onTurnOff = {
                        passcodeManager.disablePasscode()
                        passcodeOn = false
                    }
                )
            }

            item {
                SectionTitle("Home screen", modifier = Modifier.padding(top = 12.dp))
                Text(
                    "Choose which widgets show, their size and their order. You can also hold any widget on the home screen for 4 seconds to edit and drag widgets right there.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = SettingsColors.TextSecondary
                )
            }
            items(layoutViewModel.slots, key = { it.widget.name }) { slot ->
                val visible = layoutViewModel.visibleSlots
                WidgetRow(
                    slot = slot,
                    isFirst = visible.firstOrNull()?.widget == slot.widget,
                    isLast = visible.lastOrNull()?.widget == slot.widget,
                    onVisibleChange = { show ->
                        if (show) layoutViewModel.add(slot.widget) else layoutViewModel.remove(slot.widget)
                    },
                    onToggleSize = { layoutViewModel.toggleSize(slot.widget) },
                    onMove = { up -> layoutViewModel.move(slot.widget, up) },
                    modifier = Modifier.animateItem()
                )
            }
            item {
                OutlinedButton(
                    onClick = { layoutViewModel.reset() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsColors.TextPrimary),
                    border = BorderStroke(1.dp, SettingsColors.Border),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Reset layout", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        color = SettingsColors.TextSecondary,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SettingsColors.Card),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SettingsColors.Border)
    ) { content() }
}

@Composable
private fun PasscodeCard(enabled: Boolean, onSetUp: () -> Unit, onTurnOff: () -> Unit) {
    SettingsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge { Icon(if (enabled) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, tint = SettingsColors.TextPrimary) }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Passcode lock", fontWeight = FontWeight.Bold, color = SettingsColors.TextPrimary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusPill(enabled)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (enabled) "Mindful asks for your 4-digit PIN every time it opens."
                else "Add a 4-digit PIN and a recovery question to keep your journal private.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = SettingsColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSetUp,
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsColors.TextPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) { Text(if (enabled) "Change PIN" else "Turn on passcode", fontWeight = FontWeight.SemiBold) }
                if (enabled) {
                    OutlinedButton(
                        onClick = onTurnOff,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsColors.Destructive),
                        border = BorderStroke(1.dp, SettingsColors.Border),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Turn off", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(enabled: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) SettingsColors.GreenBg else SettingsColors.OffBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (enabled) SettingsColors.OnGreen else Color(0xFFA8A29E))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            if (enabled) "On" else "Off",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) SettingsColors.OnGreen else SettingsColors.TextSecondary
        )
    }
}

@Composable
private fun IconBadge(content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SettingsColors.IconBadge)
    ) { content() }
}

@Composable
private fun WidgetRow(
    slot: WidgetSlot,
    isFirst: Boolean,
    isLast: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onToggleSize: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val widget = slot.widget
    SettingsCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
        ) {
            IconBadge {
                Icon(
                    widget.icon,
                    contentDescription = null,
                    tint = if (slot.visible) SettingsColors.TextPrimary else SettingsColors.TextSecondary.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    widget.label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (slot.visible) SettingsColors.TextPrimary else SettingsColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                when {
                    !slot.visible -> Text("Hidden", fontSize = 12.sp, color = SettingsColors.TextSecondary)
                    widget.canResize -> SizeToggle(wide = slot.wide, onToggle = onToggleSize)
                    else -> Text("Wide", fontSize = 12.sp, color = SettingsColors.TextSecondary)
                }
            }
            if (slot.visible) {
                IconButton(onClick = { onMove(true) }, enabled = !isFirst, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${widget.label} up")
                }
                IconButton(onClick = { onMove(false) }, enabled = !isLast, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${widget.label} down")
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
                checked = slot.visible,
                onCheckedChange = onVisibleChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = SettingsColors.TextPrimary,
                    checkedThumbColor = Color.White
                )
            )
        }
    }
}

/** Two-option "Small | Wide" chip. */
@Composable
private fun SizeToggle(wide: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SettingsColors.OffBg)
            .clickable(onClickLabel = if (wide) "Make smaller" else "Make wider") { onToggle() }
            .padding(2.dp)
    ) {
        listOf("Small" to !wide, "Wide" to wide).forEach { (label, selected) ->
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else SettingsColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) SettingsColors.TextPrimary else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}
