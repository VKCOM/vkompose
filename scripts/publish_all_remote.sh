function checkStatus() {
    if [[ $? != 0 ]]; then
      echo "Publish failed!"
      exitWithCleanup
    fi
}

./gradlew :gradle-plugin:sub-plugin:composable-skippability-checker:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:applier:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:cleaner:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-test-tag:drawer:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:compose-source-information:cleaner:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:recompose:highlighter:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:sub-plugin:recompose:logger:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :gradle-plugin:composite:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:composable-skippability-checker:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:cleaner:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:applier:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:applier:runtime:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:drawer:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:compose-test-tag:drawer:runtime:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:compose-source-information:cleaner:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:recompose:highlighter:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:recompose:highlighter:runtime:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:recompose:logger:plugin:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :compiler-plugin:recompose:logger:runtime:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :rules:common:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :rules:detekt:publishAllPublicationsToPublicRemoteRepository || checkStatus