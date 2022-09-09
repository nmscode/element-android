/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.settings.devices.v2.details

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.copyOnLongClick

@EpoxyModelClass
abstract class SessionDetailsContentItem : VectorEpoxyModel<SessionDetailsContentItem.Holder>(R.layout.item_session_details_content) {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute
    var hasDivider: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.sessionDetailsContentTitle.text = title
        holder.sessionDetailsContentDescription.text = description
        holder.sessionDetailsContentDescription.copyOnLongClick()
        holder.sessionDetailsContentDivider.isVisible = hasDivider
    }

    class Holder : VectorEpoxyHolder() {
        val sessionDetailsContentTitle by bind<TextView>(R.id.sessionDetailsContentTitle)
        val sessionDetailsContentDescription by bind<TextView>(R.id.sessionDetailsContentDescription)
        val sessionDetailsContentDivider by bind<View>(R.id.sessionDetailsContentDivider)
    }
}
