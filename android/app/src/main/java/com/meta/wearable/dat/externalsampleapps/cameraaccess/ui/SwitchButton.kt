/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwitchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
) {
  val shape = RoundedCornerShape(16.dp)
  val accentColor = if (isDestructive) AppColor.Error else AppColor.AccentBlue

  Button(
      modifier = modifier
          .height(56.dp)
          .fillMaxWidth()
          .border(1.dp, accentColor.copy(alpha = 0.3f), shape),
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(
          containerColor = Color.White.copy(alpha = 0.08f),
          disabledContainerColor = Color.White.copy(alpha = 0.04f),
          disabledContentColor = Color.Gray,
          contentColor = Color.White,
      ),
      shape = shape,
      enabled = enabled,
  ) {
    Text(
        text = label,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
  }
}
