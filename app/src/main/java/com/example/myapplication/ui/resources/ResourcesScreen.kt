package com.example.myapplication.ui.resources

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.data.local.AppDB
import com.example.myapplication.data.model.ResourceEntry

/** Colors for Professional Help. Matches the app's warm pastel theme. */
private object ResourceColors {
    val Background = Color(0xFFFFFDF9)
    val Card = Color.White
    val TextPrimary = Color(0xFF2B2B2B)
    val TextSecondary = Color(0xFF6B6B6B)
    val TextMuted = Color(0xFFA8A29E)
    val Divider = Color(0xFFECE7E1)
    val Chip = Color(0xFFF7F3EE)
    val Ink = Color(0xFF2B2B2B)
    val Crisis = Color(0xFFFDE2E1)
    val CrisisDark = Color(0xFFB3261E)
    val Verified = Color(0xFF1E6B3A)

    /** Soft avatar colors, picked by name so each person keeps the same color. */
    val Avatars = listOf(
        Color(0xFFDDF4F8) to Color(0xFF1F5F6B),
        Color(0xFFD7F2DF) to Color(0xFF1E6B3A),
        Color(0xFFFBE6CD) to Color(0xFF8A4B00),
        Color(0xFFEDE7F6) to Color(0xFF5E35B1),
        Color(0xFFFBE4B9) to Color(0xFF6B5300)
    )
}

/** A provider with the details pulled out of its description, ready to display. */
private data class Provider(
    val entry: ResourceEntry,
    val specialty: String,
    val city: String?,
    val state: String?
) {
    val location: String? get() = if (city != null && state != null) "$city, $state" else null
}

private val LOCATION_REGEX = Regex("""located in ([^,.]+), ([A-Z]{2})""")
private val SPECIALTY_REGEX = Regex("""is a licensed (.+?) located in""")

private fun ResourceEntry.toProvider(): Provider {
    val location = LOCATION_REGEX.find(description)
    return Provider(
        entry = this,
        specialty = SPECIALTY_REGEX.find(description)?.groupValues?.get(1)?.trim() ?: "Mental Health Provider",
        city = location?.groupValues?.get(1)?.trim()?.split(" ")?.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        },
        state = location?.groupValues?.get(2)
    )
}

/** The two crisis lines are always shown in their own card, not in the provider list. */
private fun ResourceEntry.isCrisisLine(): Boolean =
    name.contains("988") || name.contains("Crisis Text Line", ignoreCase = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(navController: NavController) {
    val context = LocalContext.current
    val dao = AppDB.getDatabase(context).resourceDAO()
    val viewModel: ResourcesViewModel = viewModel(factory = ResourceViewModelFactory(dao))

    val allEntries by viewModel.ResourceList.collectAsState()
    val query by viewModel.query.collectAsState()
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var selectedProvider by remember { mutableStateOf<Provider?>(null) }

    val providers = remember(allEntries) {
        allEntries.filterNot { it.isCrisisLine() }.map { it.toProvider() }.sortedBy { it.entry.name }
    }
    val cities = remember(providers) { providers.mapNotNull { it.city }.distinct().sorted() }
    val shown = providers.filter { p ->
        (selectedCity == null || p.city == selectedCity) &&
                (query.isBlank() || p.entry.name.contains(query.trim(), ignoreCase = true) ||
                        p.specialty.contains(query.trim(), ignoreCase = true) ||
                        (p.city?.contains(query.trim(), ignoreCase = true) == true))
    }

    Scaffold(
        containerColor = ResourceColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Professional Help", fontWeight = FontWeight.Bold, color = ResourceColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { if (navController.previousBackStackEntry != null) navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ResourceColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ResourceColors.Background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "crisis") {
                CrisisLinesCard(
                    onCall988 = { dial(context, "988") },
                    onText988 = { text(context, "988", null) },
                    onTextLine = { text(context, "741741", "HOME") }
                )
            }

            item(key = "intro") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Find a professional", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ResourceColors.TextPrimary)
                    Text(
                        "Licensed psychologists across Dallas–Fort Worth, verified in the National Provider Registry.",
                        fontSize = 14.sp,
                        color = ResourceColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            item(key = "search") {
                SearchField(query = query, onQueryChange = viewModel::updateSearchQuery)
            }

            if (cities.isNotEmpty()) {
                item(key = "cities") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(label = "All areas", selected = selectedCity == null) { selectedCity = null }
                        }
                        items(cities) { city ->
                            FilterChip(label = city, selected = selectedCity == city) {
                                selectedCity = if (selectedCity == city) null else city
                            }
                        }
                    }
                }
            }

            when {
                providers.isEmpty() -> item(key = "loading") { LoadingCard() }
                shown.isEmpty() -> item(key = "empty") {
                    EmptyResultsCard(query = query, city = selectedCity)
                }
                else -> {
                    item(key = "count") {
                        Text(
                            "${shown.size} ${if (shown.size == 1) "professional" else "professionals"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ResourceColors.TextSecondary
                        )
                    }
                    items(shown, key = { it.entry.id }) { provider ->
                        ProviderCard(
                            provider = provider,
                            onClick = { selectedProvider = provider },
                            onCall = { dial(context, provider.entry.phoneNum) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            item(key = "footer") {
                Text(
                    "Mindful doesn't endorse specific providers. Contact details come from the public NPI Registry " +
                            "and may change. If you're in immediate danger, call 911.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = ResourceColors.TextMuted,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    selectedProvider?.let { provider ->
        ProviderSheet(
            provider = provider,
            onDismiss = { selectedProvider = null },
            onCall = { dial(context, provider.entry.phoneNum) },
            onMap = { openMap(context, provider) }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Crisis lines
// ---------------------------------------------------------------------------------------------

@Composable
private fun CrisisLinesCard(onCall988: () -> Unit, onText988: () -> Unit, onTextLine: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ResourceColors.Crisis)
            .padding(18.dp)
    ) {
        Text("💛  Need to talk to someone now?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ResourceColors.CrisisDark)
        Text(
            "Free, confidential and available 24/7.",
            fontSize = 14.sp,
            color = ResourceColors.TextPrimary,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("988 Suicide & Crisis Lifeline", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ResourceColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(
                onClick = onCall988,
                colors = ButtonDefaults.buttonColors(containerColor = ResourceColors.CrisisDark, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Call 988", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onText988,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ResourceColors.CrisisDark),
                border = BorderStroke(1.dp, ResourceColors.CrisisDark.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Text 988", fontWeight = FontWeight.SemiBold) }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.7f))
                .clickable(role = Role.Button, onClick = onTextLine)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Crisis Text Line", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = ResourceColors.TextPrimary)
                Text("Text HOME to 741741", fontSize = 13.sp, color = ResourceColors.TextSecondary)
            }
            Text("Text →", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ResourceColors.CrisisDark)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Search and filters
// ---------------------------------------------------------------------------------------------

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by name, specialty or city") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ResourceColors.TextSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = ResourceColors.TextSecondary)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = ResourceColors.TextPrimary,
            unfocusedBorderColor = ResourceColors.Divider,
            cursorColor = ResourceColors.TextPrimary,
            selectionColors = TextSelectionColors(
                handleColor = ResourceColors.TextPrimary,
                backgroundColor = ResourceColors.TextPrimary.copy(alpha = 0.2f)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ResourceColors.Ink else Color.White)
            .border(1.dp, if (selected) ResourceColors.Ink else ResourceColors.Divider, RoundedCornerShape(50))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "$label${if (selected) ", selected" else ""}" }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color.White else ResourceColors.TextPrimary
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Provider list
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit, onCall: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ResourceColors.Card)
            .border(1.dp, ResourceColors.Divider, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp)
    ) {
        Avatar(provider.entry.name, size = 48)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                provider.entry.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ResourceColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                provider.specialty,
                fontSize = 13.sp,
                color = ResourceColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            provider.location?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ResourceColors.TextMuted, modifier = Modifier.size(14.dp))
                    Text(it, fontSize = 12.sp, color = ResourceColors.TextMuted, modifier = Modifier.padding(start = 2.dp))
                }
            }
        }
        if (provider.entry.phoneNum.any { it.isDigit() }) {
            IconButton(onClick = onCall) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD7F2DF))
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call ${provider.entry.name}",
                        tint = ResourceColors.Verified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String, size: Int) {
    val initials = name.split(" ", ",")
        .filter { it.isNotBlank() && it.first().isLetter() && !it.endsWith(".") }
        .let { words -> listOfNotNull(words.firstOrNull(), words.lastOrNull()?.takeIf { words.size > 1 }) }
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
    val (background, foreground) = ResourceColors.Avatars[(name.hashCode() and Int.MAX_VALUE) % ResourceColors.Avatars.size]
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
    ) {
        Text(initials, fontSize = (size / 2.6).sp, fontWeight = FontWeight.Bold, color = foreground)
    }
}

@Composable
private fun LoadingCard() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ResourceColors.Card)
            .border(1.dp, ResourceColors.Divider, RoundedCornerShape(20.dp))
            .padding(28.dp)
    ) {
        CircularProgressIndicator(color = ResourceColors.Ink, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        Text(
            "Finding professionals near you…",
            fontWeight = FontWeight.SemiBold,
            color = ResourceColors.TextPrimary,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            "This needs an internet connection the first time.",
            fontSize = 13.sp,
            color = ResourceColors.TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun EmptyResultsCard(query: String, city: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ResourceColors.Card)
            .border(1.dp, ResourceColors.Divider, RoundedCornerShape(20.dp))
            .padding(28.dp)
    ) {
        Text("🔍", fontSize = 32.sp)
        Text(
            if (query.isNotBlank()) "No matches for \"$query\"" else "No professionals found",
            fontWeight = FontWeight.SemiBold,
            color = ResourceColors.TextPrimary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            if (city != null) "Try another area or clear the search." else "Try a different name, specialty or city.",
            fontSize = 13.sp,
            color = ResourceColors.TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Provider details
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSheet(provider: Provider, onDismiss: () -> Unit, onCall: () -> Unit, onMap: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ResourceColors.Background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Avatar(provider.entry.name, size = 76)
            Text(
                provider.entry.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ResourceColors.TextPrimary,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(provider.specialty, fontSize = 15.sp, color = ResourceColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFD7F2DF))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = ResourceColors.Verified, modifier = Modifier.size(16.dp))
                Text(" Verified in the NPI Registry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ResourceColors.Verified)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ResourceColors.Card)
                    .border(1.dp, ResourceColors.Divider, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                provider.location?.let { DetailRow(Icons.Default.LocationOn, "Location", it) }
                if (provider.entry.phoneNum.isNotBlank()) DetailRow(Icons.Default.Call, "Phone", provider.entry.phoneNum)
                Text(provider.entry.description, fontSize = 14.sp, lineHeight = 20.sp, color = ResourceColors.TextSecondary)
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (provider.entry.phoneNum.any { it.isDigit() }) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = ResourceColors.Ink, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Call ${provider.entry.phoneNum}", fontWeight = FontWeight.SemiBold)
                }
            }
            if (provider.location != null) {
                OutlinedButton(
                    onClick = onMap,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ResourceColors.TextPrimary),
                    border = BorderStroke(1.dp, ResourceColors.Divider),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Find on a map", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ResourceColors.Chip)
        ) {
            Icon(icon, contentDescription = null, tint = ResourceColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, fontSize = 12.sp, color = ResourceColors.TextMuted)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ResourceColors.TextPrimary)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// External navigation (MR-REQ-4)
// ---------------------------------------------------------------------------------------------

/** Opens the phone dialer with the number filled in. Never places the call automatically. */
private fun dial(context: Context, phone: String) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isEmpty()) return
    open(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits")))
}

/** Opens the messaging app to [number], optionally with a message already typed. */
private fun text(context: Context, number: String, body: String?) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
    body?.let { intent.putExtra("sms_body", it) }
    open(context, intent)
}

/** Opens the maps app searching for the provider in their city. */
private fun openMap(context: Context, provider: Provider) {
    val search = Uri.encode("${provider.entry.name} ${provider.location ?: ""}")
    open(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$search")))
}

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No app can handle it (e.g. no dialer on some emulators). Details are still shown on screen.
    }
}