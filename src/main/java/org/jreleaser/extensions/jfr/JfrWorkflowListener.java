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
package org.jreleaser.extensions.jfr;

import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import org.jreleaser.extensions.api.workflow.WorkflowListener;
import org.jreleaser.extensions.jfr.events.AnnounceEvent;
import org.jreleaser.extensions.jfr.events.AssembleEvent;
import org.jreleaser.extensions.jfr.events.DeployEvent;
import org.jreleaser.extensions.jfr.events.DistributionEvent;
import org.jreleaser.extensions.jfr.events.DownloadEvent;
import org.jreleaser.extensions.jfr.events.PackagerEvent;
import org.jreleaser.extensions.jfr.events.ReleaseEvent;
import org.jreleaser.extensions.jfr.events.SessionEvent;
import org.jreleaser.extensions.jfr.events.UploadEvent;
import org.jreleaser.extensions.jfr.events.WorkflowStepEvent;
import org.jreleaser.model.api.JReleaserContext;
import org.jreleaser.model.api.announce.Announcer;
import org.jreleaser.model.api.assemble.Assembler;
import org.jreleaser.model.api.deploy.Deployer;
import org.jreleaser.model.api.distributions.Distribution;
import org.jreleaser.model.api.download.Downloader;
import org.jreleaser.model.api.hooks.ExecutionEvent;
import org.jreleaser.model.api.packagers.Packager;
import org.jreleaser.model.api.project.Project;
import org.jreleaser.model.api.release.Releaser;
import org.jreleaser.model.api.upload.Uploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Map;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

/**
 * @author Andres Almiray
 * @since 1.0.0
 */
public final class JfrWorkflowListener implements WorkflowListener {
    private static final String CONTINUE_ON_ERROR = "continueOnError";

    private boolean continueOnError;
    private boolean enabled;
    private Recording recording;

    @Override
    public void init(JReleaserContext context, Map<String, Object> properties) {
        enabled = FlightRecorder.isAvailable();

        context.getLogger().info(enabled ? RB.$("jfr.enabled") : RB.$("jfr.disabled"));

        if (properties.containsKey(CONTINUE_ON_ERROR)) {
            continueOnError = isTrue(properties.get(CONTINUE_ON_ERROR));
        }
    }

    @Override
    public boolean isContinueOnError() {
        return false;
    }

    @Override
    public void onSessionStart(JReleaserContext context) {
        if (!enabled) return;

        Project project = context.getModel().getProject();

        try {
            Configuration configuration = Configuration.getConfiguration(project.getName());
            recording = new Recording(configuration);
        } catch (NoSuchFileException e) {
            recording = new Recording();
        } catch (IOException | ParseException e) {
            if (continueOnError) {
                enabled = false;
            } else {
                throw new IllegalStateException(e);
            }
        }

        try {
            String timestamp = ZonedDateTime.now().format(new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendValue(YEAR, 4, 4, SignStyle.EXCEEDS_PAD)
                .appendValue(MONTH_OF_YEAR, 2)
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral('T')
                .appendValue(HOUR_OF_DAY, 2)
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter());

            Path destination = context.getOutputDirectory()
                .resolve("jfr")
                .resolve(project.getName() + "-" + project.getVersion() + "-" + timestamp + ".jfr");
            Files.createDirectories(destination.getParent());
            recording.setDestination(destination);
            recording.start();
        } catch (IOException e) {
            if (continueOnError) {
                enabled = false;
            } else {
                throw new IllegalStateException(e);
            }
        }

        if (enabled) {
            SessionEvent.startSession();
        }
    }

    @Override
    public void onSessionEnd(JReleaserContext context) {
        if (!enabled) return;
        SessionEvent.endSession();

        recording.stop();
        recording.close();
        context.getLogger().info(RB.$("jfr.recording.written.to",
            context.relativizeToBasedir(recording.getDestination())));
    }

    @Override
    public void onWorkflowStep(ExecutionEvent event, JReleaserContext context) {
        if (!enabled) return;

        WorkflowStepEvent.event(event.getType().toString(), event.getName());
    }

    @Override
    public void onAnnounceStep(ExecutionEvent event, JReleaserContext context, Announcer announcer) {
        if (!enabled) return;

        AnnounceEvent.event(event.getType().toString(), announcer);
    }

    @Override
    public void onAssembleStep(ExecutionEvent event, JReleaserContext context, Assembler assembler) {
        if (!enabled) return;

        AssembleEvent.event(event.getType().toString(), assembler);
    }

    @Override
    public void onDeployStep(ExecutionEvent event, JReleaserContext context, Deployer deployer) {
        if (!enabled) return;

        DeployEvent.event(event.getType().toString(), deployer);
    }

    @Override
    public void onDownloadStep(ExecutionEvent event, JReleaserContext context, Downloader downloader) {
        if (!enabled) return;

        DownloadEvent.event(event.getType().toString(), downloader);
    }

    @Override
    public void onUploadStep(ExecutionEvent event, JReleaserContext context, Uploader uploader) {
        if (!enabled) return;

        UploadEvent.event(event.getType().toString(), uploader);
    }

    @Override
    public void onReleaseStep(ExecutionEvent event, JReleaserContext context, Releaser releaser) {
        if (!enabled) return;

        ReleaseEvent.event(event.getType().toString(), releaser);
    }

    @Override
    public void onPackagerPrepareStep(ExecutionEvent event, JReleaserContext context, Distribution distribution, Packager packager) {
        if (!enabled) return;

        PackagerEvent.event(event.getType().toString(), "PREPARE", distribution, packager);
    }

    @Override
    public void onPackagerPackageStep(ExecutionEvent event, JReleaserContext context, Distribution distribution, Packager packager) {
        if (!enabled) return;

        PackagerEvent.event(event.getType().toString(), "PACKAGE", distribution, packager);
    }

    @Override
    public void onPackagerPublishStep(ExecutionEvent event, JReleaserContext context, Distribution distribution, Packager packager) {
        if (!enabled) return;

        PackagerEvent.event(event.getType().toString(), "PUBLISH", distribution, packager);
    }

    @Override
    public void onDistributionStart(JReleaserContext context, Distribution distribution) {
        if (!enabled) return;

        DistributionEvent.startDistribution(distribution);
    }

    @Override
    public void onDistributionEnd(JReleaserContext context, Distribution distribution) {
        if (!enabled) return;

        DistributionEvent.endDistribution(distribution);
    }

    private static boolean isTrue(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return "true".equalsIgnoreCase(String.valueOf(o).trim());
    }
}
