#include "SpaceCadetPinballJNI.h"
#include "../../../../SpaceCadetPinball/winmain.h"
#include "../../../../SpaceCadetPinball/pb.h"
#include "../../../../SpaceCadetPinball/options.h"
#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <string>

void SpaceCadetPinballJNI::show_error_dialog(std::string title, std::string message) {
    __android_log_print(ANDROID_LOG_ERROR, "SpaceCadetPinballJNI", "Error: %s, %s", title.c_str(), message.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dualscreenstudios_spacecadetpinball_MainActivity_initNative(JNIEnv *env, jobject thiz,
        jstring data_path) {
    // GetStringUTFChars returns a JVM-owned buffer; copy it because the game
    // keeps the BasePath pointer for the lifetime of the process.
    const char* path = env->GetStringUTFChars(data_path, nullptr);
    winmain::BasePath = strdup(path);
    env->ReleaseStringUTFChars(data_path, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_dualscreenstudios_spacecadetpinball_MainActivity_nativeSaveSettings(JNIEnv*, jobject) {
    options::SaveSettingsToDisk();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_dualscreenstudios_spacecadetpinball_MainActivity_nativeGetHighScores(JNIEnv* env, jobject) {
    std::string out;
    char buf[128];
    for (int i = 0; i < 5; ++i) {
        const auto& row = pb::highscore_table[i];
        const char* name = (row.Name[0] == 0) ? "---" : row.Name;
        int score = (row.Score == -999) ? 0 : row.Score;
        snprintf(buf, sizeof buf, "%d. %-20s %10d\n", i + 1, name, score);
        out += buf;
    }
    return env->NewStringUTF(out.c_str());
}
