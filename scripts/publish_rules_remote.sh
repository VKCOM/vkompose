function checkStatus() {
    if [[ $? != 0 ]]; then
      echo "Publish failed!"
      exitWithCleanup
    fi
}

./gradlew :rules:common:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :rules:detekt:publishAllPublicationsToPublicRemoteRepository || checkStatus