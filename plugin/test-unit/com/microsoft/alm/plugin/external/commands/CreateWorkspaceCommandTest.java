// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.exceptions.WorkspaceAlreadyExistsException;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.junit.Assert;
import org.junit.Test;

/**
 * Covers {@link CreateWorkspaceCommand#parseOutput(String, String)} recognizing the tf "workspace already exists"
 * error. The detection must be robust to prefix/wording variation between tf clients, since a miss makes the TFVC
 * checkout fail silently instead of telling the user the workspace name is taken.
 */
public class CreateWorkspaceCommandTest {

    private static CreateWorkspaceCommand command(final String workspaceName) {
        return new CreateWorkspaceCommand(null, workspaceName, null, null, null, Workspace.Location.LOCAL);
    }

    @Test(expected = WorkspaceAlreadyExistsException.class)
    public void parseOutput_fullTfMessage_throwsWorkspaceAlreadyExists() {
        command("Folder1").parseOutput(
                "",
                "An error occurred: The workspace Folder1;DOMAIN\\user already exists on computer MACHINE.");
    }

    @Test(expected = WorkspaceAlreadyExistsException.class)
    public void parseOutput_withoutErrorPrefix_stillThrows() {
        // The "An error occurred:" prefix is not guaranteed across tf versions.
        command("Folder1").parseOutput(
                "",
                "The workspace Folder1 already exists on computer BUILDBOX.");
    }

    @Test(expected = WorkspaceAlreadyExistsException.class)
    public void parseOutput_differentCasing_stillThrows() {
        command("Folder1").parseOutput(
                "",
                "The workspace already EXISTS ON COMPUTER somebox.");
    }

    @Test(expected = WorkspaceAlreadyExistsException.class)
    public void parseOutput_matchesRegardlessOfWorkspaceName() {
        // Detection keys on the stable phrase, not on the (possibly regex-special) workspace name.
        command("My.Weird+Name(1)").parseOutput(
                "",
                "The workspace My.Weird+Name(1);owner already exists on computer HOST.");
    }

    @Test
    public void parseOutput_emptyStderr_returnsEmptyAndDoesNotThrow() {
        Assert.assertEquals("", command("Folder1").parseOutput("", ""));
    }

    @Test
    public void parseOutput_unrelatedError_throwsGenericNotWorkspaceExists() {
        try {
            command("Folder1").parseOutput("", "An error occurred: Access denied connecting to TFS.");
            Assert.fail("expected an exception for a non-empty error");
        } catch (final WorkspaceAlreadyExistsException e) {
            Assert.fail("unrelated errors must not be reported as a workspace-name collision");
        } catch (final RuntimeException e) {
            // expected: the base command surfaces other errors as a generic RuntimeException
        }
    }
}
