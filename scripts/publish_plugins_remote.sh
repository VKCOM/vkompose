function checkStatus() {
    if [[ $? != 0 ]]; then
      echo "Publish failed!"
      exit 1
    fi
}

./gradlew :gradle-plugin:sub-plugin:composable-skippability-checker:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:applier:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:cleaner:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:drawer:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-source-information:cleaner:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:sub-plugin:recompose:highlighter:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:sub-plugin:recompose:logger:publishToMavenCentral || checkStatus
./gradlew :gradle-plugin:composite:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:composable-skippability-checker:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:compose-test-tag:cleaner:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:compose-test-tag:applier:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:compose-test-tag:applier:runtime:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:compose-test-tag:drawer:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:compose-test-tag:drawer:runtime:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:compose-source-information:cleaner:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:recompose:highlighter:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:recompose:highlighter:runtime:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:recompose:logger:plugin:publishToMavenCentral || checkStatus
./gradlew :compiler-plugin:recompose:logger:runtime:publishToMavenCentral || checkStatus
