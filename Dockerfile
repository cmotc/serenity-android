FROM bitriseio/android-ndk-alpha:v2017_04_07-03_14-b450

RUN git clone https://github.com/NineWorlds/serenity-android /home/serenity-android
RUN cd /home/serenity-android && ./gradlew clean assembleDebug
