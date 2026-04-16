package com.adobe.aem.analyser.impl;

import com.adobe.aem.analyser.validators.repoinit.RepoInitValidationReport;
import com.adobe.aem.analyser.validators.repoinit.RepoInitValidator;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;

/**
 * Analyser task that validates Repoinit definitions for conflicts within a feature.
 * <p>
 * This task runs the {@link RepoInitValidator} against the provided feature and checks
 * whether there are any conflicting Repoinit statements. If conflicts are detected,
 * a warning is reported to the analyser context.
 */
public class RepoInitConflictAnalyserTask implements AnalyserTask {


    @Override
    public String getId() {
        return "repoinit-conflict-validation";
    }

    @Override
    public String getName() {
        return "Repoinit Conflict Validation";
    }

    /**
     * Executes the Repoinit conflict validation.
     * <p>
     * This method retrieves the feature from the context, validates it using
     * {@link RepoInitValidator}, and reports a warning if any conflicts are found.
     *
     * @param context analyser task context containing the feature to validate
     */
    @Override
    public void execute(final AnalyserTaskContext context) {
        Feature feature = context.getFeature();

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        if (!report.hasConflicts()) {
            return;
        }

        context.reportWarning(report.generate());
    }
}
