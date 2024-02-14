/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022-2024 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.extensions.jfr.events;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import org.jreleaser.model.api.announce.Announcer;
import org.jreleaser.model.api.announce.HttpAnnouncers;
import org.jreleaser.model.api.announce.WebhooksAnnouncer;

/**
 * @author Andres Almiray
 * @since 1.0.0
 */
@Label("Announce Event")
@Description("Announce Event")
public class AnnounceEvent extends Event {
    @Label("Kind")
    public String kind;

    @Label("Type")
    public String type;

    @Label("Name")
    public String name;

    public static void event(String kind, Announcer announcer) {
        if (announcer instanceof HttpAnnouncers || announcer instanceof WebhooksAnnouncer) return;

        AnnounceEvent event = new AnnounceEvent();
        event.kind = kind;
        event.type = announcer.getType();
        event.name = announcer.getName();
        event.commit();
    }
}