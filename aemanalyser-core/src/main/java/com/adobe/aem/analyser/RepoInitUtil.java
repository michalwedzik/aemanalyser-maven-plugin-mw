/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.aem.analyser;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

class RepoInitUtil {
    private RepoInitUtil() {}

    private static final Pattern INCORRECT_PATH_PATTERN = Pattern.compile("^create path \\(sling:Folder\\)(?!.*\\(sling:).*", Pattern.MULTILINE);
    private static final Logger LOGGER = LoggerFactory.getLogger(RepoInitUtil.class);

    static void validateRepoinit(Feature feature) {
        final Extension repoinitExtension = feature.getExtensions().getByName("repoinit");
        if (isRepoinitIncorrect(repoinitExtension)) {
                LOGGER.info("Incorrect repoinit: '{}' for feature {}", repoinitExtension.getText(), feature);
        }
    }

    static boolean isRepoinitIncorrect(Extension repoinitExtension) {
        if (repoinitExtension != null && repoinitExtension.getType() == ExtensionType.TEXT) {
            String text = repoinitExtension.getText();
            return text != null && INCORRECT_PATH_PATTERN.matcher(text).find();
        }
        return false;
    }
}
