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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class RepoInitUtil {
    private RepoInitUtil() {}

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoInitUtil.class);


    static void validateRepoinit(final List<Feature> features) {

        long start = System.nanoTime();
        StringBuilder validationResult = new StringBuilder();
        try {
            validationResult = validateFeatures(features);
        } finally {
            long end = System.nanoTime();
            long durationMs = (end - start) / 1_000_000;

            validationResult.append("validateRepoinit took ");
            validationResult.append(durationMs);
            validationResult.append(" ms\n");
            LOGGER.warn(validationResult.toString());
        }
    }

    static StringBuilder validateFeatures(final List<Feature> features) {
        StringBuilder logMessage = new StringBuilder("Repoinit validation results:\n");
        for (Feature feature : features) {
            if (feature.getExtensions().getByName("repoinit") == null) {
                continue;
            }

            final Extension repoinitExtension = feature.getExtensions().getByName("repoinit");
            List<CreatePath[]> conflicts = doesRepoinitHaveConflicts(repoinitExtension);

            if (!conflicts.isEmpty()) {
                logMessage.append("Incorrect repoinit for feature ").append(feature).append("\n");
                logMessage.append("Found ").append(conflicts.size()).append(" sets of conflicting repoinit statements:\n");
                for (CreatePath[] conflict : conflicts) {
                    logMessage.append(conflict[0].asRepoInitString().stripTrailing()).append("\n");
                    logMessage.append(conflict[1].asRepoInitString().stripTrailing()).append("\n");
                    logMessage.append("\n");
                }
            }
        }

        if (logMessage.length() > 29) {
           return logMessage;
        }
        return logMessage.append("No issues found\n");
    }

    private static List<CreatePath[]> doesRepoinitHaveConflicts(Extension repoinitExtension) {
        if (repoinitExtension == null || repoinitExtension.getText() == null || repoinitExtension.getType() != ExtensionType.TEXT) {
            return Collections.emptyList();
        }

        try {
            List<Operation> operations = new RepoInitParserImpl(
                    new StringReader(repoinitExtension.getText())
            ).parse();

            List<CreatePath> createPaths = operations.stream()
                    .filter(op -> op instanceof CreatePath)
                    .map(op -> (CreatePath) op)
                    .collect(Collectors.toList());

            return hasConflicts(createPaths);
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

    private static List<CreatePath[]> hasConflicts(List<CreatePath> createPaths) {
        int size = createPaths.size();
        List<CreatePath[]> conflicts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                List<CreatePath[]> conflict = hasConflict(createPaths.get(i), createPaths.get(j));
                if (!conflict.isEmpty()) {
                    conflicts.addAll(conflict);
                }
            }
        }
        return conflicts;
    }

    private static List<CreatePath[]> hasConflict(CreatePath a, CreatePath b) {
        List<PathSegmentDefinition> aDefs = a.getDefinitions();
        List<PathSegmentDefinition> bDefs = b.getDefinitions();

        // different depth → no conflict
        if (aDefs.size() != bDefs.size()) {
            return Collections.emptyList();
        }
        List<CreatePath[]> conflicts = new ArrayList<>();
        for (int i = 0; i < aDefs.size(); i++) {
            PathSegmentDefinition aSeg = aDefs.get(i);
            PathSegmentDefinition bSeg = bDefs.get(i);

            // segments diverge → stop comparing this pair
            if (!Objects.equals(aSeg.getSegment(), bSeg.getSegment())) {
                return Collections.emptyList();
            }

            // same segment but different type → conflict
            if (!Objects.equals(aSeg.getPrimaryType(), bSeg.getPrimaryType())) {
                CreatePath[] conflict = new CreatePath[2];
                conflict[0] = a;
                conflict[1] = b;

                conflicts.add(conflict);
                break;
            }
        }
        return conflicts;
    }
}
