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
import org.apache.sling.repoinit.parser.impl.ParseException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserImpl;
import org.apache.sling.repoinit.parser.operations.CreatePath;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.repoinit.parser.operations.PathSegmentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class RepoInitUtil {
    private RepoInitUtil() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoInitUtil.class);

    static void validateRepoinit(Feature feature) {
        final Extension repoinitExtension = feature.getExtensions().getByName("repoinit");
        if (isRepoinitIncorrect(repoinitExtension)) {
                LOGGER.info("Incorrect repoinit: '{}' for feature {}", repoinitExtension.getText(), feature);
        }
    }

    static boolean isRepoinitIncorrect(Extension repoinitExtension) {
        if (repoinitExtension == null || repoinitExtension.getText() == null || repoinitExtension.getType() != ExtensionType.TEXT) {
            return false;
        }

        try {
            List<Operation> operations = new RepoInitParserImpl(
                    new StringReader(repoinitExtension.getText())
            ).parse();

            List<CreatePath> createPaths = operations.stream()
                    .filter(op -> op instanceof CreatePath)
                    .map(op -> (CreatePath) op)
                    .collect(Collectors.toList());

            return isCreatePathsIncorrect(createPaths);
        } catch (ParseException e) {
            return false;
        }
    }

    private static boolean isCreatePathsIncorrect(List<CreatePath> createPaths) {
        int size = createPaths.size();

        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                if (hasConflict(createPaths.get(i), createPaths.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasConflict(CreatePath a, CreatePath b) {
        List<PathSegmentDefinition> aDefs = a.getDefinitions();
        List<PathSegmentDefinition> bDefs = b.getDefinitions();

        // different depth → no conflict
        if (aDefs.size() != bDefs.size()) {
            return false;
        }

        for (int i = 0; i < aDefs.size(); i++) {
            PathSegmentDefinition aSeg = aDefs.get(i);
            PathSegmentDefinition bSeg = bDefs.get(i);

            // segments diverge → stop comparing this pair
            if (!Objects.equals(aSeg.getSegment(), bSeg.getSegment())) {
                return false;
            }

            // same segment but different type → conflict
            if (!Objects.equals(aSeg.getPrimaryType(), bSeg.getPrimaryType())) {
                return true;
            }
        }
        return false;
    }
}
