package com.ionutv.livelinesdetection.features.camera

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ionutv.livelinesdetection.features.ml_checks.LivelinessDetectionOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LivelinessDetectionOptionSelection(
    list: List<LivelinessDetectionOption>,
    onSelectedChanged: (LivelinessDetectionOption) -> Unit,
    selectedOption: LivelinessDetectionOption
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = {
        expanded = it
    }) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOption.name,
            onValueChange = { },
            label = { Text("Categories") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            list.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(text = selectionOption.name) },
                    onClick = {
                        onSelectedChanged(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}