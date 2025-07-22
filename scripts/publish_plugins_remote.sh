function checkStatus() {
    if [[ $? != 0 ]]; then
      echo "Publish failed!"
      exit 1
    fi
}

./gradlew :gradle-plugin:sub-plugin:composable-skippability-checker:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:applier:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:cleaner:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:drawer:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-source-information:cleaner:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:recompose:highlighter:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:recompose:logger:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :gradle-plugin:composite:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:composable-skippability-checker:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:cleaner:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:applier:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:applier:runtime:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:drawer:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:drawer:runtime:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:compose-source-information:cleaner:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:recompose:highlighter:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:recompose:highlighter:runtime:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:recompose:logger:plugin:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :compiler-plugin:recompose:logger:runtime:publishAllPublicationsToMavenCentralRepository || checkStatus
