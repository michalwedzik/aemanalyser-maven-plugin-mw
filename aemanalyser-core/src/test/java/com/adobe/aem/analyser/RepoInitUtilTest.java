package com.adobe.aem.analyser;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepoInitUtilTest {

    @Test
    public void shouldReturnNoIssuesWhenNoFeatures() {
        String result = RepoInitUtil.validateFeatures(List.of()).toString();

        assertTrue(result.contains("No issues found"));
    }

    @Test
    public void shouldReturnNoIssuesWhenFeatureHasNoRepoinit() {
        Feature feature = mock(Feature.class);
        when(feature.getExtensions()).thenReturn(mock(org.apache.sling.feature.Extensions.class));
        when(feature.getExtensions().getByName("repoinit")).thenReturn(null);

        String result = RepoInitUtil.validateFeatures(List.of(feature)).toString();

        assertTrue(result.contains("No issues found"));
    }

    @Test
    public void shouldReturnNoIssuesForCorrectRepoinit() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b\n" +
                        "create path (sling:Folder) /apps/a/c\n" +
                        "create path (sling:Folder) /apps/a/c/d(cq:ClientLibraryFolder)"
        );
        Feature feature = featureWithExtension(extension);

        String result = RepoInitUtil.validateFeatures(List.of(feature)).toString();

        assertTrue(result.contains("No issues found"));
    }

    @Test
    public void shouldReportConflictForSamePathDifferentType() {
        Extension extension = textExtension(
                "create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/a/b"
        );

        Feature feature = featureWithExtension(extension);

        String result = RepoInitUtil.validateFeatures(List.of(feature)).toString();

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

        String result = RepoInitUtil.validateFeatures(List.of(feature)).toString();

        assertTrue(result.contains("Found 2 sets of conflicting repoinit statements"));
    }

    @Test
    public void shouldReportConflictsForMultipleFeatures() {
        Feature feature1 = featureWithExtension(textExtension(
                "create path (sling:Folder) /apps/a/b(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/a/b"
        ));

        Feature feature2 = featureWithExtension(textExtension(
                "create path (sling:Folder) /apps/x/y(cq:ClientLibraryFolder)\n" +
                        "create path (sling:Folder) /apps/x/y"
        ));

        String result = RepoInitUtil.validateFeatures(List.of(feature1, feature2)).toString();

        assertTrue(result.contains("Incorrect repoinit for feature"));
        assertTrue(result.contains("Found 1 sets of conflicting repoinit statements"));
    }

    @Test
    public void shouldIgnoreInvalidRepoinitSyntax() {
        Extension extension = textExtension("invalid $$$");

        Feature feature = featureWithExtension(extension);

        String result = RepoInitUtil.validateFeatures(List.of(feature)).toString();

        assertTrue(result.contains("No issues found"));
    }

    private Extension textExtension(String text) {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(text);
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