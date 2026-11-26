package com.kevinandrey.qrscanner.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevinandrey.qrscanner.R
import com.kevinandrey.qrscanner.core.ContentKind
import com.kevinandrey.qrscanner.core.DraftProblem
import com.kevinandrey.qrscanner.core.ScanFilter
import com.kevinandrey.qrscanner.core.WifiSecurity

val ContentKind.icon: ImageVector
    get() =
        when (this) {
            ContentKind.URL -> Icons.Outlined.Link
            ContentKind.WIFI -> Icons.Outlined.Wifi
            ContentKind.CONTACT -> Icons.Outlined.Person
            ContentKind.EMAIL -> Icons.Outlined.Email
            ContentKind.PHONE -> Icons.Outlined.Call
            ContentKind.SMS -> Icons.Outlined.Sms
            ContentKind.GEO -> Icons.Outlined.Place
            ContentKind.TEXT -> Icons.AutoMirrored.Outlined.Notes
        }

@get:StringRes
val ContentKind.label: Int
    get() =
        when (this) {
            ContentKind.URL -> R.string.kind_url
            ContentKind.WIFI -> R.string.kind_wifi
            ContentKind.CONTACT -> R.string.kind_contact
            ContentKind.EMAIL -> R.string.kind_email
            ContentKind.PHONE -> R.string.kind_phone
            ContentKind.SMS -> R.string.kind_sms
            ContentKind.GEO -> R.string.kind_geo
            ContentKind.TEXT -> R.string.kind_text
        }

@get:StringRes
val ScanFilter.label: Int
    get() =
        when (this) {
            ScanFilter.ALL -> R.string.filter_all
            ScanFilter.FAVORITES -> R.string.filter_favorites
            ScanFilter.LINKS -> R.string.filter_links
            ScanFilter.WIFI -> R.string.filter_wifi
            ScanFilter.CONTACTS -> R.string.filter_contacts
            ScanFilter.OTHER -> R.string.filter_other
        }

@get:StringRes
val WifiSecurity.label: Int
    get() =
        when (this) {
            WifiSecurity.WPA -> R.string.security_wpa
            WifiSecurity.WEP -> R.string.security_wep
            WifiSecurity.OPEN -> R.string.security_open
        }

@get:StringRes
val DraftProblem.message: Int
    get() =
        when (this) {
            DraftProblem.EMPTY_TEXT -> R.string.problem_empty_text
            DraftProblem.INVALID_URL -> R.string.problem_invalid_url
            DraftProblem.EMPTY_SSID -> R.string.problem_empty_ssid
            DraftProblem.INVALID_WPA_PASSWORD -> R.string.problem_invalid_wpa_password
            DraftProblem.EMPTY_PASSWORD -> R.string.problem_empty_password
            DraftProblem.EMPTY_NAME -> R.string.problem_empty_name
            DraftProblem.INVALID_PHONE -> R.string.problem_invalid_phone
            DraftProblem.INVALID_EMAIL -> R.string.problem_invalid_email
            DraftProblem.TOO_LONG -> R.string.problem_too_long
        }

/** Kind icon inside a tonal circle, used in lists and headers. */
@Composable
fun ContentKindBadge(
    kind: ContentKind,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Surface(modifier = modifier.size(size), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = kind.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * ICON_RATIO),
            )
        }
    }
}

private const val ICON_RATIO = 0.55f
