/* Copyright (c) 2012-2013, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.qtiworks.services.candidate;

import uk.ac.ed.ph.qtiworks.QtiWorksLogicException;
import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.domain.DomainEntityNotFoundException;
import uk.ac.ed.ph.qtiworks.domain.entities.AssessmentPackage;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateEvent;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateEventNotification;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateItemEventType;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateSession;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateTestEventType;
import uk.ac.ed.ph.qtiworks.domain.entities.Delivery;
import uk.ac.ed.ph.qtiworks.domain.entities.ItemDeliverySettings;
import uk.ac.ed.ph.qtiworks.domain.entities.User;
import uk.ac.ed.ph.qtiworks.rendering.AbstractRenderingOptions;
import uk.ac.ed.ph.qtiworks.rendering.AbstractRenderingRequest;
import uk.ac.ed.ph.qtiworks.rendering.AssessmentRenderer;
import uk.ac.ed.ph.qtiworks.rendering.AuthorViewRenderingOptions;
import uk.ac.ed.ph.qtiworks.rendering.ItemAuthorViewRenderingRequest;
import uk.ac.ed.ph.qtiworks.rendering.ItemRenderingOptions;
import uk.ac.ed.ph.qtiworks.rendering.ItemRenderingRequest;
import uk.ac.ed.ph.qtiworks.rendering.TerminatedRenderingRequest;
import uk.ac.ed.ph.qtiworks.rendering.TestAuthorViewRenderingRequest;
import uk.ac.ed.ph.qtiworks.rendering.TestRenderingMode;
import uk.ac.ed.ph.qtiworks.rendering.TestRenderingOptions;
import uk.ac.ed.ph.qtiworks.rendering.TestRenderingRequest;
import uk.ac.ed.ph.qtiworks.services.AssessmentDataService;
import uk.ac.ed.ph.qtiworks.services.AssessmentPackageFileService;
import uk.ac.ed.ph.qtiworks.services.CandidateAuditLogger;
import uk.ac.ed.ph.qtiworks.services.CandidateDataService;
import uk.ac.ed.ph.qtiworks.services.FilespaceManager;
import uk.ac.ed.ph.qtiworks.services.RequestTimestampContext;
import uk.ac.ed.ph.qtiworks.services.ServiceUtilities;
import uk.ac.ed.ph.qtiworks.services.dao.CandidateSessionDao;
import uk.ac.ed.ph.qtiworks.services.domain.OutputStreamer;

import uk.ac.ed.ph.jqtiplus.internal.util.Assert;
import uk.ac.ed.ph.jqtiplus.notification.NotificationLevel;
import uk.ac.ed.ph.jqtiplus.notification.NotificationRecorder;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;
import uk.ac.ed.ph.jqtiplus.running.TestSessionController;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.state.TestPlanNodeKey;
import uk.ac.ed.ph.jqtiplus.state.TestSessionState;
import uk.ac.ed.ph.jqtiplus.validation.AssessmentObjectValidationResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for rendering the candidate state of assessments, connecting the domain
 * layer with the low-level {@link AssessmentRenderer}
 *
 * @see AssessmentRenderer
 *
 * @author David McKain
 */
@Service
@Transactional(propagation=Propagation.REQUIRED)
public class CandidateRenderingService {

    @Resource
    private RequestTimestampContext requestTimestampContext;

    @Resource
    private CandidateAuditLogger candidateAuditLogger;

    @Resource
    private AssessmentPackageFileService assessmentPackageFileService;

    @Resource
    private FilespaceManager filespaceManager;

    @Resource
    private AssessmentDataService assessmentDataService;

    @Resource
    private CandidateDataService candidateDataService;

    @Resource
    private AssessmentRenderer assessmentRenderer;

    @Resource
    private CandidateItemDeliveryService candidateItemDeliveryService;

    @Resource
    private CandidateTestDeliveryService candidateTestDeliveryService;

    @Resource
    private CandidateSessionDao candidateSessionDao;

    //----------------------------------------------------
    // Item rendering

    /**
     * Renders the current state of the item {@link CandidateSession} having
     * the given ID (xid).
     */
    public void renderCurrentCandidateItemSessionState(final long xid, final String sessionToken,
            final ItemRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws CandidateException, DomainEntityNotFoundException, IOException {
        Assert.notNull(sessionToken, "sessionToken");
        final CandidateSession candidateSession = candidateItemDeliveryService.lookupCandidateItemSession(xid, sessionToken);
        renderCurrentCandidateItemSessionState(candidateSession, renderingOptions, outputStreamer);
    }

    public void renderCurrentCandidateItemSessionState(final CandidateSession candidateSession,
            final ItemRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws IOException, CandidateException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(renderingOptions, "renderingOptions");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* Create temporary file to hold the output before it gets streamed */
        final File resultFile = filespaceManager.createTempFile();
        try {
            /* Render to temp file */
            FileOutputStream resultOutputStream = null;
            try {
                resultOutputStream = new FileOutputStream(resultFile);
                renderCurrentCandidateItemSessionState(candidateSession, renderingOptions, new StreamResult(resultOutputStream));
            }
            catch (final IOException e) {
                throw new QtiWorksRuntimeException("Unexpected IOException", e);
            }
            finally {
                ServiceUtilities.ensureClose(resultOutputStream);
            }

            /* Finally stream to caller */
            streamRenderingResultFile(resultFile, outputStreamer, renderingOptions);
        }
        finally {
            if (!resultFile.delete()) {
                throw new QtiWorksRuntimeException("Could not delete result file " + resultFile.getPath());
            }
        }
    }

    private void renderCurrentCandidateItemSessionState(final CandidateSession candidateSession,
            final ItemRenderingOptions renderingOptions, final StreamResult result)
            throws CandidateException {
        if (candidateSession.isExploded()) {
            renderExploded(candidateSession, renderingOptions, result);
        }
        else if (candidateSession.isTerminated()) {
            renderTerminated(candidateSession, renderingOptions, result);
        }
        else {
            /* Look up most recent event */
            final CandidateEvent latestEvent = ensureSessionEntered(candidateSession);

            /* Load the ItemSessionState */
            final ItemSessionState itemSessionState = candidateDataService.loadItemSessionState(latestEvent);

            /* Touch the session's duration state if appropriate */
            if (itemSessionState.isEntered() && !itemSessionState.isEnded() && !itemSessionState.isSuspended()) {
                final Date timestamp = requestTimestampContext.getCurrentRequestTimestamp();
                final ItemSessionController itemSessionController = createItemSessionController(candidateSession, itemSessionState);
                itemSessionController.touchDuration(timestamp);
            }

            /* Render event */
            renderItemEvent(latestEvent, itemSessionState, renderingOptions, result);
        }
    }

    private void renderItemEvent(final CandidateEvent candidateEvent, final ItemSessionState itemSessionState,
            final ItemRenderingOptions renderingOptions, final StreamResult result) {
        final CandidateItemEventType itemEventType = candidateEvent.getItemEventType();
        final CandidateSession candidateSession = candidateEvent.getCandidateSession();
        final User candidate = candidateSession.getCandidate();
        final Delivery delivery = candidateSession.getDelivery();
        final ItemDeliverySettings itemDeliverySettings = (ItemDeliverySettings) assessmentDataService.getEffectiveDeliverySettings(candidate, delivery);

        /* Create and partially configure rendering request */
        final ItemRenderingRequest renderingRequest = new ItemRenderingRequest();
        initRenderingRequest(candidateSession, renderingRequest, renderingOptions);
        renderingRequest.setItemSessionState(itemSessionState);
        renderingRequest.setPrompt(itemDeliverySettings.getPrompt());

        /* If session has terminated, render appropriate state and exit */
        if (itemSessionState.isExited()) {
            assessmentRenderer.renderTeminated(createTerminatedRenderingRequest(candidateSession, renderingRequest.getRenderingOptions()), result);
            return;
        }

        /* Detect "modal" events. These will cause a particular rendering state to be
         * displayed, which candidate will then leave.
         */
        if (itemEventType==CandidateItemEventType.SOLUTION) {
            renderingRequest.setSolutionMode(true);
        }

        /* Now set candidate action permissions depending on state of session */
        if (itemEventType==CandidateItemEventType.SOLUTION || itemSessionState.isEnded()) {
            /* Item session is ended (closed) */
            renderingRequest.setEndAllowed(false);
            renderingRequest.setHardResetAllowed(itemDeliverySettings.isAllowHardResetWhenEnded());
            renderingRequest.setSoftResetAllowed(itemDeliverySettings.isAllowSoftResetWhenEnded());
            renderingRequest.setSolutionAllowed(itemDeliverySettings.isAllowSolutionWhenEnded());
            renderingRequest.setCandidateCommentAllowed(false);
        }
        else if (itemSessionState.isOpen()) {
            /* Item session is open (interacting) */
            renderingRequest.setEndAllowed(itemDeliverySettings.isAllowEnd());
            renderingRequest.setHardResetAllowed(itemDeliverySettings.isAllowHardResetWhenOpen());
            renderingRequest.setSoftResetAllowed(itemDeliverySettings.isAllowSoftResetWhenOpen());
            renderingRequest.setSolutionAllowed(itemDeliverySettings.isAllowSolutionWhenOpen());
            renderingRequest.setCandidateCommentAllowed(itemDeliverySettings.isAllowCandidateComment());
        }

        else {
            throw new QtiWorksLogicException("Item has not been entered yet. We do not currently support rendering of this state.");
        }

        /* Finally pass to rendering layer */
        candidateAuditLogger.logItemRendering(candidateEvent);
        final List<CandidateEventNotification> notifications = candidateEvent.getNotifications();
        assessmentRenderer.renderItem(renderingRequest, notifications, result);
    }

    //----------------------------------------------------
    // Item Author View rendering

    public void renderCurrentCandidateItemSessionStateAuthorView(final long xid, final String sessionToken,
            final AuthorViewRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws CandidateException, DomainEntityNotFoundException, IOException {
        Assert.notNull(sessionToken, "sessionToken");
        final CandidateSession candidateSession = candidateItemDeliveryService.lookupCandidateItemSession(xid, sessionToken);
        renderCurrentCandidateItemSessionStateAuthorView(candidateSession, renderingOptions, outputStreamer);
    }

    public void renderCurrentCandidateItemSessionStateAuthorView(final CandidateSession candidateSession,
            final AuthorViewRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws IOException, CandidateException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(renderingOptions, "renderingOptions");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* Look up most recent event */
        final CandidateEvent latestEvent = ensureSessionEntered(candidateSession);

        /* Load the ItemSessionState */
        final ItemSessionState itemSessionState = candidateDataService.loadItemSessionState(latestEvent);

        /* Create temporary file to hold the output before it gets streamed */
        final File resultFile = filespaceManager.createTempFile();
        try {
            /* Render to temp file */
            FileOutputStream resultOutputStream = null;
            try {
                resultOutputStream = new FileOutputStream(resultFile);
                renderItemEventAuthorView(latestEvent, itemSessionState, renderingOptions, new StreamResult(resultOutputStream));
            }
            catch (final IOException e) {
                throw new QtiWorksRuntimeException("Unexpected IOException", e);
            }
            finally {
                ServiceUtilities.ensureClose(resultOutputStream);
            }

            /* Finally stream to caller */
            streamRenderingResultFile(resultFile, outputStreamer, renderingOptions);
        }
        finally {
            if (!resultFile.delete()) {
                throw new QtiWorksRuntimeException("Could not delete result file " + resultFile.getPath());
            }
        }
    }

    private void renderItemEventAuthorView(final CandidateEvent candidateEvent, final ItemSessionState itemSessionState,
            final AuthorViewRenderingOptions renderingOptions, final StreamResult result) {
        final CandidateSession candidateSession = candidateEvent.getCandidateSession();
        final ItemAuthorViewRenderingRequest renderingRequest = new ItemAuthorViewRenderingRequest();
        initRenderingRequest(candidateSession, renderingRequest, renderingOptions);
        renderingRequest.setItemSessionState(itemSessionState);

        candidateAuditLogger.logItemAuthorViewRendering(candidateEvent);
        final List<CandidateEventNotification> notifications = candidateEvent.getNotifications();
        assessmentRenderer.renderItemAuthorView(renderingRequest, notifications, result);
    }

    //----------------------------------------------------
    // Test rendering

    /**
     * Renders the current state of the test {@link CandidateSession} having
     * the given ID (xid).
     */
    public void renderCurrentCandidateTestSessionState(final long xid, final String sessionToken,
            final TestRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws CandidateException, DomainEntityNotFoundException, IOException {
        final CandidateSession candidateSession = candidateTestDeliveryService.lookupCandidateTestSession(xid, sessionToken);
        renderCurrentCandidateTestSessionState(candidateSession, renderingOptions, outputStreamer);
    }

    public void renderCurrentCandidateTestSessionState(final CandidateSession candidateSession,
            final TestRenderingOptions renderingOptions,
            final OutputStreamer outputStreamer)
            throws IOException, CandidateException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(renderingOptions, "renderingOptions");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* Create temporary file to hold the output before it gets streamed */
        final File resultFile = filespaceManager.createTempFile();
        try {
            /* Render to temp file */
            FileOutputStream resultOutputStream = null;
            try {
                resultOutputStream = new FileOutputStream(resultFile);
                renderCurrentCandidateTestSessionState(candidateSession, renderingOptions, new StreamResult(resultOutputStream));
            }
            catch (final IOException e) {
                throw new QtiWorksRuntimeException("Unexpected IOException", e);
            }
            finally {
                ServiceUtilities.ensureClose(resultOutputStream);
            }

            /* Finally stream to caller */
            streamRenderingResultFile(resultFile, outputStreamer, renderingOptions);
        }
        finally {
            if (!resultFile.delete()) {
                throw new QtiWorksRuntimeException("Could not delete result file " + resultFile.getPath());
            }
        }
    }

    private void renderCurrentCandidateTestSessionState(final CandidateSession candidateSession,
            final TestRenderingOptions renderingOptions, final StreamResult result)
            throws CandidateException {
        if (candidateSession.isExploded()) {
            renderExploded(candidateSession, renderingOptions, result);
        }
        else if (candidateSession.isTerminated()) {
            renderTerminated(candidateSession, renderingOptions, result);
        }
        else {
            /* Look up most recent event */
            final CandidateEvent latestEvent = ensureSessionEntered(candidateSession);

            /* Load the TestSessionState and create a TestSessionController */
            final TestSessionState testSessionState = candidateDataService.loadTestSessionState(latestEvent);
            final TestSessionController testSessionController = createTestSessionController(candidateSession, testSessionState);

            /* Touch the session's duration state if appropriate */
            if (testSessionState.isEntered() && !testSessionState.isEnded()) {
                final Date timestamp = requestTimestampContext.getCurrentRequestTimestamp();
                testSessionController.touchDurations(timestamp);
            }

            /* Render event */
            renderTestEvent(latestEvent, testSessionController, renderingOptions, result);
        }
    }

    private void renderTestEvent(final CandidateEvent candidateEvent, final TestSessionController testSessionController,
            final TestRenderingOptions renderingOptions, final StreamResult result) {
        final CandidateTestEventType testEventType = candidateEvent.getTestEventType();
        final CandidateSession candidateSession = candidateEvent.getCandidateSession();

        /* Create and partially configure rendering request */
        final TestRenderingRequest renderingRequest = new TestRenderingRequest();
        initRenderingRequest(candidateSession, renderingRequest, renderingOptions);
        renderingRequest.setTestSessionController(testSessionController);

        /* If session has terminated, render appropriate state and exit */
        final TestSessionState testSessionState = testSessionController.getTestSessionState();
        if (candidateSession.isTerminated() || testSessionState.isExited()) {
            assessmentRenderer.renderTeminated(createTerminatedRenderingRequest(candidateSession, renderingRequest.getRenderingOptions()), result);
            return;
        }

        /* Check for "modal" events first. These cause a particular rendering state to be
         * displayed, which candidate will then leave.
         */
        if (testEventType==CandidateTestEventType.REVIEW_ITEM) {
            /* Extract item to review */
            renderingRequest.setTestRenderingMode(TestRenderingMode.ITEM_REVIEW);
            renderingRequest.setModalItemKey(extractTargetItemKey(candidateEvent));
        }
        else if (testEventType==CandidateTestEventType.SOLUTION_ITEM) {
            /* Extract item to show solution */
            renderingRequest.setTestRenderingMode(TestRenderingMode.ITEM_SOLUTION);
            renderingRequest.setModalItemKey(extractTargetItemKey(candidateEvent));
        }

        /* Pass to rendering layer */
        candidateAuditLogger.logTestRendering(candidateEvent);
        final List<CandidateEventNotification> notifications = candidateEvent.getNotifications();
        assessmentRenderer.renderTest(renderingRequest, notifications, result);
    }

    private TestPlanNodeKey extractTargetItemKey(final CandidateEvent candidateEvent) {
        final String keyString = candidateEvent.getTestItemKey();
        try {
            return TestPlanNodeKey.fromString(keyString);
        }
        catch (final Exception e) {
            throw new QtiWorksLogicException("Unexpected Exception parsing TestPlanNodeKey " + keyString);
        }
    }

    //----------------------------------------------------
    // Test Author View rendering

    public void renderCurrentCandidateTestSessionStateAuthorView(final long xid, final String sessionToken,
            final AuthorViewRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws CandidateException, DomainEntityNotFoundException, IOException {
        Assert.notNull(sessionToken, "sessionToken");
        final CandidateSession candidateSession = candidateTestDeliveryService.lookupCandidateTestSession(xid, sessionToken);
        renderCurrentCandidateTestSessionStateAuthorView(candidateSession, renderingOptions, outputStreamer);
    }

    public void renderCurrentCandidateTestSessionStateAuthorView(final CandidateSession candidateSession,
            final AuthorViewRenderingOptions renderingOptions, final OutputStreamer outputStreamer)
            throws IOException, CandidateException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(renderingOptions, "renderingOptions");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* Look up most recent event */
        final CandidateEvent latestEvent = ensureSessionEntered(candidateSession);

        /* Load the TestSessionState and create a TestSessionController */
        final TestSessionState testSessionState = candidateDataService.loadTestSessionState(latestEvent);
        final TestSessionController testSessionController = createTestSessionController(candidateSession, testSessionState);

        /* Create temporary file to hold the output before it gets streamed */
        final File resultFile = filespaceManager.createTempFile();
        try {
            /* Render to temp file */
            FileOutputStream resultOutputStream = null;
            try {
                resultOutputStream = new FileOutputStream(resultFile);
                renderTestEventAuthorView(latestEvent, testSessionController, renderingOptions, new StreamResult(resultOutputStream));
            }
            catch (final IOException e) {
                throw new QtiWorksRuntimeException("Unexpected IOException", e);
            }
            finally {
                ServiceUtilities.ensureClose(resultOutputStream);
            }

            /* Finally stream to caller */
            streamRenderingResultFile(resultFile, outputStreamer, renderingOptions);
        }
        finally {
            if (!resultFile.delete()) {
                throw new QtiWorksRuntimeException("Could not delete result file " + resultFile.getPath());
            }
        }
    }

    private void renderTestEventAuthorView(final CandidateEvent candidateEvent, final TestSessionController testSessionController,
            final AuthorViewRenderingOptions renderingOptions, final StreamResult result) {
        final CandidateSession candidateSession = candidateEvent.getCandidateSession();
        final TestAuthorViewRenderingRequest renderingRequest = new TestAuthorViewRenderingRequest();
        initRenderingRequest(candidateSession, renderingRequest, renderingOptions);
        renderingRequest.setTestSessionController(testSessionController);

        candidateAuditLogger.logTestAuthorViewRendering(candidateEvent);
        final List<CandidateEventNotification> notifications = candidateEvent.getNotifications();
        assessmentRenderer.renderTestAuthorView(renderingRequest, notifications, result);
    }

    //----------------------------------------------------
    // Access to additional package resources (e.g. images/CSS)

    public void streamAssessmentPackageFile(final long xid, final String sessionToken, final String fileSystemIdString,
            final OutputStreamer outputStreamer)
            throws CandidateException, IOException, DomainEntityNotFoundException {
        Assert.notNull(sessionToken, "sessionToken");
        Assert.notNull(fileSystemIdString, "fileSystemIdString");
        Assert.notNull(outputStreamer, "outputStreamer");
        final CandidateSession candidateSession = lookupCandidateSession(xid, sessionToken);
        streamAssessmentPackageFile(candidateSession, fileSystemIdString, outputStreamer);
    }

    public void streamAssessmentPackageFile(final CandidateSession candidateSession, final String fileSystemIdString,
            final OutputStreamer outputStreamer)
            throws CandidateException, IOException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(fileSystemIdString, "fileSystemIdString");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* We shall revoke candidate access to resources after the session has been terminated */
        ensureSessionNotTerminated(candidateSession);

        /* Make sure requested file is whitelisted for access */
        final Delivery delivery = candidateSession.getDelivery();
        final AssessmentPackage assessmentPackage = assessmentDataService.ensureSelectedAssessmentPackage(delivery);
        String resultingFileHref = null;
        for (final String safeFileHref : assessmentPackage.getSafeFileHrefs()) {
            final URI fileUri = assessmentPackageFileService.createAssessmentFileUri(assessmentPackage, safeFileHref);
            if (fileUri.toString().equals(fileSystemIdString)) {
                resultingFileHref = safeFileHref;
                break;
            }
        }

        if (resultingFileHref!=null) {
            /* Safe to stream */
            assessmentPackageFileService.streamAssessmentPackageFile(assessmentPackage, resultingFileHref, outputStreamer);
        }
        else {
            /* Blacklisted file. Log and throw Exception */
            candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.ACCESS_BLACKLISTED_ASSESSMENT_FILE);
        }
    }

    //----------------------------------------------------
    // Candidate Source access

    public void streamAssessmentSource(final long xid, final String sessionToken, final OutputStreamer outputStreamer)
            throws CandidateException, IOException, DomainEntityNotFoundException {
        Assert.notNull(outputStreamer, "outputStreamer");
        final CandidateSession candidateSession = lookupCandidateSession(xid, sessionToken);
        streamAssessmentSource(candidateSession, outputStreamer);
    }

    public void streamAssessmentSource(final CandidateSession candidateSession, final OutputStreamer outputStreamer)
            throws CandidateException, IOException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(outputStreamer, "outputStreamer");
        ensureCallerMayAccessAuthorInfo(candidateSession);
        final Delivery itemDelivery = candidateSession.getDelivery();
        final AssessmentPackage assessmentPackage = assessmentDataService.ensureSelectedAssessmentPackage(itemDelivery);

        /* Forbid results if the candidate session is closed */
        ensureSessionNotTerminated(candidateSession);

        /* Make sure candidate can access authoring info */
        ensureCallerMayAccessAuthorInfo(candidateSession);

        assessmentPackageFileService.streamAssessmentPackageSource(assessmentPackage, outputStreamer);
        candidateAuditLogger.logAction(candidateSession, "ACCESS_SOURCE");
    }

    //----------------------------------------------------
    // Candidate state access

    public void streamAssessmentState(final long xid, final String sessionToken, final OutputStreamer outputStreamer)
            throws CandidateException, DomainEntityNotFoundException, IOException {
        Assert.notNull(outputStreamer, "outputStreamer");
        final CandidateSession candidateSession = lookupCandidateSession(xid, sessionToken);
        streamAssessmentState(candidateSession, outputStreamer);
    }

    public void streamAssessmentState(final CandidateSession candidateSession, final OutputStreamer outputStreamer)
            throws CandidateException, IOException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* Make sure candidate can access authoring info */
        ensureCallerMayAccessAuthorInfo(candidateSession);

        /* Get most recent event */
        final CandidateEvent mostRecentEvent = ensureSessionEntered(candidateSession);

        /* Generate result Object from current state */
        final File sessionStateFile = candidateDataService.ensureSessionStateFile(mostRecentEvent);

        /* Record action */
        candidateAuditLogger.logAction(candidateSession, "ACCESS_STATE");

        /* Stream result file */
        final Date lastModifiedTime = mostRecentEvent.getTimestamp();
        ServiceUtilities.streamFile(sessionStateFile, "application/xml", lastModifiedTime, outputStreamer);
    }

    //----------------------------------------------------
    // Candidate Result access

    public void streamAssessmentResult(final long xid, final String sessionToken, final OutputStreamer outputStreamer)
            throws CandidateException, DomainEntityNotFoundException, IOException {
        Assert.notNull(outputStreamer, "outputStreamer");
        final CandidateSession candidateSession = lookupCandidateSession(xid, sessionToken);
        streamAssessmentResult(candidateSession, outputStreamer);
    }

    public void streamAssessmentResult(final CandidateSession candidateSession, final OutputStreamer outputStreamer)
            throws CandidateException, IOException {
        Assert.notNull(candidateSession, "candidateSession");
        Assert.notNull(outputStreamer, "outputStreamer");

        /* Make sure candidate can access authoring info */
        ensureCallerMayAccessAuthorInfo(candidateSession);

        /* Generate result Object from current state */
        final File assessmentResultFile = candidateDataService.ensureAssessmentResultFile(candidateSession);

        /* Log action */
        candidateAuditLogger.logAction(candidateSession, "ACCESS_RESULT");

        /* Stream result file */
        final Date lastModifiedTime = new Date(assessmentResultFile.lastModified());
        ServiceUtilities.streamFile(assessmentResultFile, "application/xml", lastModifiedTime, outputStreamer);
    }

    //----------------------------------------------------

    public <E extends AssessmentObjectValidationResult<?>> E
    generateValidationResult(final long xid, final String sessionToken)
            throws CandidateException, DomainEntityNotFoundException {
        final CandidateSession candidateSession = lookupCandidateSession(xid, sessionToken);
        return generateValidationResult(candidateSession);
    }

    public <E extends AssessmentObjectValidationResult<?>> E
    generateValidationResult(final CandidateSession candidateSession)
            throws CandidateException {
        Assert.notNull(candidateSession, "candidateSession");

        /* Make sure candidate can access authoring info */
        ensureCallerMayAccessAuthorInfo(candidateSession);

        /* Validate package */
        final AssessmentPackage assessmentPackage = assessmentDataService.ensureSelectedAssessmentPackage(candidateSession.getDelivery());
        return assessmentPackageFileService.loadAndValidateAssessment(assessmentPackage);
    }

    //----------------------------------------------------

    private ItemSessionController createItemSessionController(final CandidateSession candidateSession, final ItemSessionState itemSessionState) {
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        return candidateDataService.createItemSessionController(candidateSession,
                itemSessionState, notificationRecorder);
    }

    private TestSessionController createTestSessionController(final CandidateSession candidateSession, final TestSessionState testSessionState) {
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        return candidateDataService.createTestSessionController(candidateSession,
                testSessionState, notificationRecorder);
    }

    private void renderExploded(final CandidateSession candidateSession, final AbstractRenderingOptions renderingOptions, final StreamResult result) {
        assessmentRenderer.renderExploded(createTerminatedRenderingRequest(candidateSession, renderingOptions), result);
    }

    private void renderTerminated(final CandidateSession candidateSession, final AbstractRenderingOptions renderingOptions, final StreamResult result) {
        assessmentRenderer.renderTeminated(createTerminatedRenderingRequest(candidateSession, renderingOptions), result);
    }

    //----------------------------------------------------

    private TerminatedRenderingRequest createTerminatedRenderingRequest(final CandidateSession candidateSession, final AbstractRenderingOptions renderingOptions) {
        final TerminatedRenderingRequest renderingRequest = new TerminatedRenderingRequest();
        initRenderingRequest(candidateSession, renderingRequest, renderingOptions);
        renderingRequest.setExitSessionUrl(candidateSession.getExitUrl());
        return renderingRequest;
    }

    private <P extends AbstractRenderingOptions> void initRenderingRequest(final CandidateSession candidateSession,
            final AbstractRenderingRequest<P> renderingRequest, final P renderingOptions) {
        final Delivery delivery = candidateSession.getDelivery();
        final AssessmentPackage assessmentPackage = assessmentDataService.ensureSelectedAssessmentPackage(delivery);

        renderingRequest.setRenderingOptions(renderingOptions);
        renderingRequest.setAssessmentResourceLocator(assessmentPackageFileService.createResolvingResourceLocator(assessmentPackage));
        renderingRequest.setAssessmentResourceUri(assessmentPackageFileService.createAssessmentObjectUri(assessmentPackage));
        renderingRequest.setAuthorMode(candidateSession.isAuthorMode());
        renderingRequest.setValidated(assessmentPackage.isValidated());
        renderingRequest.setLaunchable(assessmentPackage.isLaunchable());
        renderingRequest.setErrorCount(assessmentPackage.getErrorCount());
        renderingRequest.setWarningCount(assessmentPackage.getWarningCount());
        renderingRequest.setValid(assessmentPackage.isValid());
    }

    //----------------------------------------------------
    // Result streaming

    private void streamRenderingResultFile(final File resultFile, final OutputStreamer outputStreamer,
            final AbstractRenderingOptions renderingOptions)
            throws IOException {
        final String contentType = renderingOptions.getSerializationMethod().getContentType();
        final long contentLength = resultFile.length();
        FileInputStream resultInputStream = null;
        try {
            resultInputStream = new FileInputStream(resultFile);
            outputStreamer.stream(contentType, contentLength,
                    requestTimestampContext.getCurrentRequestTimestamp(), /* Use request time, which is less accurate than it could be */
                    resultInputStream);
        }
        catch (final FileNotFoundException e) {
            throw new QtiWorksRuntimeException("Unexpected IOException", e);
        }
        catch (final IOException e) {
            /* Streamer threw Exception */
            throw e;
        }
        finally {
            ServiceUtilities.ensureClose(resultInputStream);
        }
    }

    //----------------------------------------------------
    // Access controls

    private CandidateSession lookupCandidateSession(final long xid, final String sessionToken)
            throws DomainEntityNotFoundException, CandidateException {
        Assert.notNull(sessionToken, "sessionToken");
        final CandidateSession candidateSession = candidateSessionDao.requireFindById(xid);
        if (!sessionToken.equals(candidateSession.getSessionToken())) {
            candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.SESSION_TOKEN_MISMATCH);
        }
        return candidateSession;
    }


    private void ensureSessionNotTerminated(final CandidateSession candidateSession)
            throws CandidateException {
        if (candidateSession.isTerminated()) {
            candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.SESSION_IS_TERMINATED);
        }
    }

    private CandidateEvent ensureSessionEntered(final CandidateSession candidateSession)
            throws CandidateException {
        final CandidateEvent mostRecentEvent = candidateDataService.getMostRecentEvent(candidateSession);
        if (mostRecentEvent==null) {
            candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.SESSION_NOT_ENTERED);
        }
        return mostRecentEvent;
    }

    private void ensureCallerMayAccessAuthorInfo(final CandidateSession candidateSession)
            throws CandidateException {
        if (!candidateSession.isAuthorMode()) {
            candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.AUTHOR_INFO_FORBIDDEN);
        }
    }
}
