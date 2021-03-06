# Copyright (C) 2020 The Dagger Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Description:
#   A JSR-330 compliant dependency injection system for android and java

package(default_visibility = ["//:src"])

java_library(
    name = "componentgenerator",
    srcs = glob(["*.java"]),
    plugins = ["//java/dagger/internal/codegen/bootstrap"],
    deps = [
        "//java/dagger:core",
        "//java/dagger/internal/codegen/base",
        "//java/dagger/internal/codegen/binding",
        "//java/dagger/internal/codegen/compileroption",
        "//java/dagger/internal/codegen/extension",
        "//java/dagger/internal/codegen/javapoet",
        "//java/dagger/internal/codegen/kotlin",
        "//java/dagger/internal/codegen/langmodel",
        "//java/dagger/internal/codegen/writing",
        "//java/dagger/internal/guava:base",
        "//java/dagger/internal/guava:collect",
        "//java/dagger/producers",
        "//java/dagger/spi",
        "@google_bazel_common//third_party/java/javapoet",
        "@google_bazel_common//third_party/java/jsr330_inject",
        "@maven//:com_google_auto_auto_common",
    ],
)
