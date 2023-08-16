PROJECT_PATH="$(git rev-parse --show-toplevel)"

./gradlew :gradle-plugin:sub-plugin:composable-skippability-checker:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:sub-plugin:compose-test-tag:applier:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:sub-plugin:compose-test-tag:cleaner:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:sub-plugin:compose-test-tag:drawer:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:sub-plugin:compose-source-information:cleaner:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:sub-plugin:recompose:highlighter:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:sub-plugin:recompose:logger:publishAllPublicationsToPropertyRootRepository \
:gradle-plugin:composite:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:composable-skippability-checker:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:compose-test-tag:cleaner:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:compose-test-tag:applier:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:compose-test-tag:applier:runtime:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:compose-test-tag:drawer:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:compose-test-tag:drawer:runtime:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:compose-source-information:cleaner:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:recompose:highlighter:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:recompose:highlighter:runtime:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:recompose:logger:plugin:publishAllPublicationsToPropertyRootRepository \
:compiler-plugin:recompose:logger:runtime:publishAllPublicationsToPropertyRootRepository \
:rules:common:publishAllPublicationsToPropertyRootRepository \
:rules:detekt:publishAllPublicationsToPropertyRootRepository -PPROPERTY_MAVEN_ROOT=$PROJECT_PATH/libs/