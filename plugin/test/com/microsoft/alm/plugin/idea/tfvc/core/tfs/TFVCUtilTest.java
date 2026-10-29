// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.idea.tfvc.core.TFSVcs;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcWorkspaceLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TFVCUtilTest {

    private final Workspace workspace = new Workspace(
            "server",
            "name",
            "computer",
            "owner",
            "comment",
            Collections.singletonList(new Workspace.Mapping("serverPath", "/tmp/localPath", false)));

    @Mock
    private Project mockProject;

    @Mock
    private MockedStatic<TfvcWorkspaceLocator> tfvcWorkspaceLocatorStatic;

    @Mock
    private TFSVcs mockVcs;

    @Mock
    private ProjectLevelVcsManager mockVcsManager;

    @Mock
    private MockedStatic<ProjectLevelVcsManager> projectLevelVcsManagerStatic;

    @Before
    public void setUp() {
        tfvcWorkspaceLocatorStatic.when(() -> TfvcWorkspaceLocator.getPartialWorkspace(mockProject, false))
                .thenReturn(workspace);
    }

    @Test
    public void isFileUnderTFVCMappingShouldTests() {
        assertTrue(TFVCUtil.isFileUnderTFVCMapping(mockProject, new LocalFilePath("/tmp/localPath/1.txt", false)));
        assertTrue(TFVCUtil.isFileUnderTFVCMapping(mockProject, new LocalFilePath("/tmp/localPath", true)));
        assertFalse(TFVCUtil.isFileUnderTFVCMapping(mockProject, new LocalFilePath("/tmp/localPath1", true)));
        assertFalse(TFVCUtil.isFileUnderTFVCMapping(mockProject, new LocalFilePath("/tmp/localPath1/1.txt", false)));
    }

    @Test
    public void filterValidTFVCPathsTest() {
        List<FilePath> localFiles = Arrays.asList(
                new LocalFilePath("/tmp/localPath", true),
                new LocalFilePath("/tmp/localPath/1.txt", false));
        List<FilePath> nonLocalFiles = Arrays.asList(
                new LocalFilePath("/tmp/localPath1", true),
                new LocalFilePath("/tmp/localPath1/1.txt", false));
        List<FilePath> allPaths = Lists.newArrayList(Iterables.concat(localFiles, nonLocalFiles));
        List<String> localFilePaths = localFiles.stream().map(FilePath::getPath).collect(Collectors.toList());
        assertThat(TFVCUtil.filterValidTFVCPaths(mockProject, allPaths), is(localFilePaths));
    }

    @Test
    public void filterDollarTFVCPathsTest() {
        List<FilePath> localFiles = Arrays.asList(
                new LocalFilePath("/tmp/localPath", true),
                new LocalFilePath("/tmp/localPath/1.txt", false));
        List<FilePath> dollarFiles = Arrays.asList(
                new LocalFilePath("/tmp/localPath/$1.txt", false),
                new LocalFilePath("/tmp/localPath/$1/1.txt", false),
                new LocalFilePath("/tmp/localPath/$1", true));
        List<FilePath> allPaths = Lists.newArrayList(Iterables.concat(localFiles, dollarFiles));
        List<String> localFilePaths = localFiles.stream().map(FilePath::getPath).collect(Collectors.toList());
        assertThat(TFVCUtil.filterValidTFVCPaths(mockProject, allPaths), is(localFilePaths));
    }

    @Test
    public void isInServiceDirectoryTests() {
        assertFalse(TFVCUtil.isInServiceDirectory(new LocalFilePath("C:\\Temp", true)));
        assertFalse(TFVCUtil.isInServiceDirectory(new LocalFilePath("/tmp/_tf/xxx", true)));
        assertTrue(TFVCUtil.isInServiceDirectory(new LocalFilePath("/tmp/$tf/xxx", true)));
        assertTrue(TFVCUtil.isInServiceDirectory(new LocalFilePath("/tmp/.tf/xxx", true)));
        assertTrue(TFVCUtil.isInServiceDirectory(new LocalFilePath("/tmp/.tf", false)));
    }

    @Test
    public void isInvalidTFVCPathTests() {
        mockVcsRoots("/tmp/localPath");
        assertFalse(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/localPath", true)));
        assertFalse(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/localPath/1.txt", false)));
        assertTrue(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/localPath/$1.txt", false)));
        assertTrue(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/localPath/$1/1.txt", false)));
        assertTrue(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/localPath/$tf/1.txt", false)));
    }

    @Test
    public void isInvalidTFVCPathIgnoresDollarAboveVcsRoot() {
        mockVcsRoots("/tmp/$parent/localPath");
        assertFalse(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/$parent/localPath/1.txt", false)));
        assertTrue(TFVCUtil.isInvalidTFVCPath(mockVcs, new LocalFilePath("/tmp/$parent/localPath/$1.txt", false)));
    }

    @Test
    public void collectInvalidTFVCPathsTest() {
        mockVcsRoots("/tmp/localPath");
        List<FilePath> validPaths = Arrays.asList(
                new LocalFilePath("/tmp/localPath", true),
                new LocalFilePath("/tmp/localPath/1.txt", false));
        List<FilePath> invalidPaths = Arrays.asList(
                new LocalFilePath("/tmp/localPath/$1.txt", false),
                new LocalFilePath("/tmp/localPath/$1/1.txt", false),
                new LocalFilePath("/tmp/localPath/$tf/1.txt", false));
        List<FilePath> allPaths = Lists.newArrayList(Iterables.concat(validPaths, invalidPaths));
        assertThat(
                TFVCUtil.collectInvalidTFVCPaths(mockVcs, allPaths.stream()).collect(Collectors.toList()),
                is(invalidPaths));
    }

    private void mockVcsRoots(String... rootPaths) {
        List<VirtualFile> roots = Arrays.stream(rootPaths).map(path -> {
            VirtualFile root = mock(VirtualFile.class);
            when(root.getPath()).thenReturn(path);
            return root;
        }).collect(Collectors.toList());
        when(mockVcs.getProject()).thenReturn(mockProject);
        projectLevelVcsManagerStatic.when(() -> ProjectLevelVcsManager.getInstance(mockProject))
                .thenReturn(mockVcsManager);
        when(mockVcsManager.getRootsUnderVcsWithoutFiltering(mockVcs)).thenReturn(roots);
    }
}
