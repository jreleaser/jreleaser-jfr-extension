/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022 The JReleaser authors.
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

/**
 * @author Andres Almiray
 * @since 1.0.0
 */
@Label("Workflow Step Event")
@Description("Workflow Step Event")
public class WorkflowStepEvent extends Event {
    @Label("Kind")
    public String kind;

    @Label("Step")
    public String step;

    public static void event(String kind, String step) {
        WorkflowStepEvent event = new WorkflowStepEvent();
        event.kind = kind;
        event.step = step;
        event.commit();
    }
}