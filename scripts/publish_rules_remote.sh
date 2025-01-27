function checkStatus() {
    if [[ $? != 0 ]]; then
      echo "Publish failed!"
      exit 1
    fi
}

./gradlew :rules:common:publishAllPublicationsToPublicRemoteRepository || checkStatus
./gradlew :rules:detekt:publishAllPublicationsToPublicRemoteRepository || checkStatus