/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.utils.aws;

import com.amazonaws.services.s3.AmazonS3URI;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides utility aws-related functionality.
 */
public final class AwsS3Utils {

    static final String MACRO_SITENAME = "{siteName}";

    /**
     * Returns the base key from the S3 URL, making sure to replace the {@code {siteName}} macro instances
     *
     * @param s3Url    the S3 URL
     * @param siteName the site name
     */
    public static String getS3BaseKey(AmazonS3URI s3Url, String siteName) {
        String baseKey = s3Url.getKey();
        if (StringUtils.isNotEmpty(baseKey)) {
            return baseKey.replace(MACRO_SITENAME, siteName);
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Returns the bucket from the S3 URL, making sure to replace the {@code {siteName}} macro instances
     *
     * @param s3Url    the S3 URL
     * @param siteName the site name
     */
    public static String getBucket(AmazonS3URI s3Url, String siteName) {
        String bucket = s3Url.getBucket();
        return bucket.replace(MACRO_SITENAME, siteName);
    }
}
