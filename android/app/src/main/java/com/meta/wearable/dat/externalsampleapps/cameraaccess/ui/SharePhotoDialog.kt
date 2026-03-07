/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GradientButton

@Composable
fun SharePhotoDialog(photo: Bitmap, onDismiss: () -> Unit, onShare: (Bitmap) -> Unit) {
  val shape = RoundedCornerShape(24.dp)
  Dialog(onDismissRequest = onDismiss) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f), shape)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.12f))
                ),
                shape,
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(text = stringResource(R.string.photo_captured), color = AppColor.TextPrimary)

      Image(
          bitmap = photo.asImageBitmap(),
          contentDescription = stringResource(R.string.captured_photo),
          modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp)),
      )

      GradientButton(text = stringResource(R.string.share), onClick = { onShare(photo) })
    }
  }
}
