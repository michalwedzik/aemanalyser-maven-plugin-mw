package com.adobe.aem.analyser;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RepoInitUtilTest {

    @Test
    public void shouldReturnFalseWhenExtensionIsNull() {
        assertFalse(RepoInitUtil.isRepoinitIncorrect(null));
    }

    @Test
    public void shouldReturnFalseWhenExtensionIsNotText() {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.JSON);

        assertFalse(RepoInitUtil.isRepoinitIncorrect(extension));
    }

    @Test
    public void shouldReturnFalseWhenTextIsNull() {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(null);

        assertFalse(RepoInitUtil.isRepoinitIncorrect(extension));
    }

    @Test
    public void shouldReturnFalseForCorrectRepoinit() {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(
                "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/readonly"
        );

        assertFalse(RepoInitUtil.isRepoinitIncorrect(extension));
    }

    @Test
    public void shouldReturnTrueForIncorrectRepoinit() {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(
                "create path (sling:Folder) /apps/namics/genericmultifield/readonly"
        );

        assertTrue(RepoInitUtil.isRepoinitIncorrect(extension));
    }

    @Test
    public void shouldReturnTrueWhenMultipleLinesContainIncorrect() {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(
                "create path (sling:Folder) /apps/namics/genericmultifield/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/js"
        );

        assertTrue(RepoInitUtil.isRepoinitIncorrect(extension));
    }

    @Test
    public void shouldReturnFalseWhenMultipleLinesAllCorrect() {
        Extension extension = mock(Extension.class);
        when(extension.getType()).thenReturn(ExtensionType.TEXT);
        when(extension.getText()).thenReturn(
                "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/readonly\n" +
                        "create path (sling:Folder) /apps/namics/genericmultifield(sling:Folder)/clientlibs/js"
        );

        assertFalse(RepoInitUtil.isRepoinitIncorrect(extension));
    }
}