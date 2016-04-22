LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := PB_debugAdbPrj
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)
include $(CLEAR_VARS)

include $(BUILD_MULTI_PREBUILT)  

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
