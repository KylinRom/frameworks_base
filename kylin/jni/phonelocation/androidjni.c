/*
 * Copyright (C) 2012 - 2015 The MoKee OpenSource Project
 * Copyright (C) 2016 The KylinRom Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Global header for android jni call
 *
 * Created by cytown martincz kylin
 * Last edited 2015.02.12
 *
 */


#include <string.h>
#include <stdio.h>
#include <jni.h>
#include <assert.h>
#include <android/log.h>
#include "androidjni.h"

//#define DEBUG

#ifdef DEBUG
#define TAG_JNI = "kylin-phoneloc-jni"
#endif

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;

    clazz = (*env)->FindClass(env, className);
    if (clazz == NULL) {
#ifdef DEBUG
        __android_log_print(ANDROID_LOG_INFO, TAG_JNI, "class not exist!");
#endif
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numMethods) < 0) {
#ifdef DEBUG
        __android_log_print(ANDROID_LOG_INFO, TAG_JNI, "method not exist!");
#endif
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 */
static int registerNatives(JNIEnv *env) {
    if (!registerNativeMethods(env, JNIREG_CLASS, gMethods,
                               sizeof(gMethods) / sizeof(gMethods[0])))
        return JNI_FALSE;

    return JNI_TRUE;
}

/*
 * Set some test stuff up.
 *
 * Returns the JNI version on success, -1 on failure.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jint result = -1;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    if (!registerNatives(env)) { return -1; }
    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

    return result;
}
