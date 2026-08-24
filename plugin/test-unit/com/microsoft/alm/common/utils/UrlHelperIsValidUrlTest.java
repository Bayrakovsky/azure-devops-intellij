// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Covers {@link UrlHelper#isValidUrl(String)} after the migration from the deprecated
 * {@code URL(String)} constructor to {@code URI.toURL()}.
 */
public class UrlHelperIsValidUrlTest {

    @Test
    public void acceptsAzureDevOpsServicesUrl() {
        Assert.assertTrue(UrlHelper.isValidUrl("https://dev.azure.com/myorganization"));
    }

    @Test
    public void acceptsVisualStudioComUrl() {
        Assert.assertTrue(UrlHelper.isValidUrl("https://myaccount.visualstudio.com"));
    }

    @Test
    public void acceptsOnPremTfsUrlWithPortAndCollection() {
        Assert.assertTrue(UrlHelper.isValidUrl("http://tfs-server:8080/tfs/DefaultCollection"));
    }

    @Test
    public void acceptsUrlWithTrailingSlash() {
        Assert.assertTrue(UrlHelper.isValidUrl("https://dev.azure.com/myorganization/"));
    }

    @Test
    public void acceptsUrlWithEncodedSpaces() {
        Assert.assertTrue(UrlHelper.isValidUrl("https://dev.azure.com/my%20organization/My%20Project"));
    }

    @Test
    public void acceptsUrlWithQueryString() {
        Assert.assertTrue(UrlHelper.isValidUrl("https://dev.azure.com/org/project?version=GBmain"));
    }

    @Test
    public void rejectsNull() {
        Assert.assertFalse(UrlHelper.isValidUrl(null));
    }

    @Test
    public void rejectsEmptyString() {
        Assert.assertFalse(UrlHelper.isValidUrl(""));
    }

    @Test
    public void rejectsPlainText() {
        Assert.assertFalse(UrlHelper.isValidUrl("not a url"));
    }

    @Test
    public void rejectsUnknownProtocol() {
        Assert.assertFalse(UrlHelper.isValidUrl("nosuchprotocol://server/path"));
    }

    @Test
    public void rejectsRelativePath() {
        Assert.assertFalse(UrlHelper.isValidUrl("/tfs/DefaultCollection"));
    }

    @Test
    public void rejectsHostRelativeUrlWithoutProtocol() {
        Assert.assertFalse(UrlHelper.isValidUrl("dev.azure.com/myorganization"));
    }
}
