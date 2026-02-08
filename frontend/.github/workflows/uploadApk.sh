curl https://upload.diawi.com/ \
    -F token='ns67xCHnJfEvo7xpTn1JSEHm2uJLZ8suqmi98xxiwR' \
    -F file=@./build/app/outputs/apk/release/app-armeabi-v7a-release.apk \
    -H Content-Type='multipart/form-data'


curl https://upload.diawi.com/ \
    -F token='ns67xCHnJfEvo7xpTn1JSEHm2uJLZ8suqmi98xxiwR' \
    -F file=@./build/app/outputs/apk/release/app-arm64-v8a-release.apk \
    -H Content-Type='multipart/form-data'