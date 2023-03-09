/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2022-2023 The JReleaser authors.
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
import org.jreleaser.model.api.catalog.Cataloger;
import org.jreleaser.model.api.catalog.sbom.SbomCataloger;

/**
 * @author Andres Almiray
 * @since 1.1.0
 */
@Label("Catalog Event")
@Description("Catalog Event")
public class CatalogEvent extends Event {
    private static final String SBOM_CATALOGER = SbomCataloger.class.getSimpleName();
    private static final String CATALOGER = Cataloger.class.getSimpleName();

    @Label("Kind")
    public String kind;

    @Label("Group")
    public String group;

    @Label("Type")
    public String type;

    @Label("Name")
    public String name;

    public static void event(String kind, Cataloger cataloger) {
        CatalogEvent event = new CatalogEvent();
        event.kind = kind;
        event.group = cataloger.getGroup();
        event.type = cataloger.getType();
        event.name = extractName(cataloger.getClass().getSimpleName());
        event.commit();
    }

    // https://github.com/jreleaser/jreleaser/issues/1252
    private static String extractName(String className) {
        if (className.endsWith(SBOM_CATALOGER)) {
            return className.substring(0, className.length() - SBOM_CATALOGER.length());
        }
        return className.substring(0, className.length() - CATALOGER.length());
    }
}