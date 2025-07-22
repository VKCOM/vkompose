function checkStatus() {
    if [[ $? != 0 ]]; then
      echo "Publish failed!"
      exit 1
    fi
}

./gradlew :rules:common:publishAllPublicationsToMavenCentralRepository || checkStatus
./gradlew :rules:detekt:publishAllPublicationsToMavenCentralRepository || checkStatus