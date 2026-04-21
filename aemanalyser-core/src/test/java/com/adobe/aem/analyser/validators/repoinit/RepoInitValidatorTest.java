package com.adobe.aem.analyser.validators.repoinit;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepoInitValidatorTest {

    @Test
    public void shouldReturnNoIssuesWhenFeatureHasNoRepoinit() {
        Feature feature = mock(Feature.class);
        when(feature.getExtensions()).thenReturn(mock(org.apache.sling.feature.Extensions.class));
        when(feature.getExtensions().getByName("repoinit")).thenReturn(null);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    public void shouldReturnNoIssuesForCorrectRepoinit() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b\n" +
                        "create path (sling:Folder) /apps/a/c\n" +
                        "create path (sling:Folder) /apps/a/c/d(cq:ClientLibraryFolder)"
        );

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    public void shouldReportConflictForSamePathDifferentType() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/a/b"
        );

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);
        String result = report.generate();

        assertTrue(report.hasConflicts());
        assertTrue(result.contains("Incorrect repoinit for feature"));
        assertTrue(result.contains("Found 1 sets of conflicting repoinit statements"));
        assertTrue(result.contains("/apps/a/b"));
    }

    @Test
    public void shouldReportMultipleConflicts() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/a/b\n" +
                        "create path (sling:Folder) /apps/x/y(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/x/y"
        );

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);
        String result = report.generate();

        assertTrue(report.hasConflicts());
        assertTrue(result.contains("Found 2 sets of conflicting repoinit statements"));
    }

    @Test
    public void shouldIgnoreInvalidRepoinitSyntax() {
        Extension extension = textExtension("invalid $$$");

        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport report = RepoInitValidator.validateRepoinit(feature);

        assertFalse(report.hasConflicts());
        assertTrue(report.generate().contains("No issues found"));
    }

    @Test
    public void shouldNotModifyRepoinitWhenFixDisabled() {
        String original =
                "create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/a/b";
        Extension extension = textExtension(original);
        Feature feature = featureWithExtension(extension);

        RepoInitValidator.validateRepoinit(feature, false);

        assertEquals(original, extension.getText());
    }

    @Test
    public void shouldHandleMultipleConflictsForSamePath() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b\n" +
                        "create path (nt:unstructured) /apps/a/b\n" +
                        "create path (cq:ClientLibraryFolder) /apps/a/b"
        );
        Feature feature = featureWithExtension(extension);

        RepoInitValidationReport result = RepoInitValidator.validateRepoinit(feature, false);

        assertTrue(result.hasConflicts());
        assertTrue(result.generate().contains("Found 3 sets of conflicting repoinit statements"));
    }

    @Test
    public void shouldHandleEmptyRepoinit() {
        Extension extension = textExtension("");
        Feature feature = featureWithExtension(extension);

        String result = RepoInitValidator.validateRepoinit(feature, false).generate();

        assertTrue(result.contains("No issues found"));
    }

    @Test
    public void shouldRemoveConflictsInCustomerExample() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/namics/genericmultifield/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield/clientlibs/css\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield/clientlibs/js\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/css(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/js(cq:ClientLibraryFolder)"
        );

        Feature feature = featureWithExtension(extension);

        RepoInitValidator.validateRepoinit(feature, true);

        String fixed = extension.getText();

        String expectedRepoinit =
                "create path (sling:Folder) /apps/namics/genericmultifield/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/css(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/js(cq:ClientLibraryFolder)";
        assertEquals(expectedRepoinit, fixed);
    }

    @Test
    public void shouldRemoveConflictsInCustomerExampleDifferentOrder() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/css(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/js(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield/clientlibs/css\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield/clientlibs/js\n"
        );

        Feature feature = featureWithExtension(extension);

        RepoInitValidator.validateRepoinit(feature, true);

        String fixed = extension.getText();

        String expectedRepoinit =
                "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/css(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/js(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield/readonly";
        assertEquals(expectedRepoinit, fixed);
    }

    @Test
    public void shouldPreserveCommentsWhenFixEnabled() {
        String original =
                "# origin=test\n" +
                        "create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/a/b";

        Extension extension = textExtension(original);
        Feature feature = featureWithExtension(extension);

        RepoInitValidator.validateRepoinit(feature, true);

        String fixed = extension.getText();
        assertTrue(fixed.contains("# origin=test"));
    }

    @Test
    public void shouldHandleLeadingWhitespace() {
        Extension extension = textExtension(
                "   create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "   create path (sling:Folder) /apps/a/b"
        );
        Feature feature = featureWithExtension(extension);

        RepoInitValidator.validateRepoinit(feature, true);

        String fixed = extension.getText();
        assertFalse(fixed.contains("create path (sling:Folder) /apps/a/b\n"));
    }

    private Extension textExtension(String text) {
        Extension extension = mock(Extension.class);

        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(text);

        doAnswer(invocation -> {
            String newText = invocation.getArgument(0);
            when(extension.getText()).thenReturn(newText);
            return null;
        }).when(extension).setText(anyString());

        return extension;
    }

    private Feature featureWithExtension(Extension extension) {
        Feature feature = mock(Feature.class);
        org.apache.sling.feature.Extensions extensions = mock(org.apache.sling.feature.Extensions.class);

        when(feature.getExtensions()).thenReturn(extensions);
        when(extensions.getByName("repoinit")).thenReturn(extension);

        return feature;
    }
}