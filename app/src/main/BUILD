# Simulate google-services gradle plugin

android_library(
    name = "demo_lib",
    srcs = glob([
        "java/**/*.java",
    ]),
    custom_package = "com.qualcomm.qti.snpe.imageclassifiers",
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = [
        "//app/libs:snpe_release_aar",
        "//third_party:android_arch_core_common",        
        "//third_party:android_arch_lifecycle_common",
        "//third_party:com_android_support_constraint_constraint_layout_solver",
        "//third_party:com_android_support_support_annotations",
        "@android_arch_lifecycle_runtime//aar",
        "@com_android_support_appcompat_v7//aar",
        "@com_android_support_cardview//aar",
        "@com_android_support_constraint_constraint_layout//aar",
        "@com_android_support_design//aar",
        "@com_android_support_recyclerview//aar",
        "@com_android_support_support_compat//aar",
        "@com_android_support_support_core_ui//aar",
        "@com_android_support_support_core_utils//aar",
        "@com_android_support_support_fragment//aar",
        "@com_android_support_support_v4//aar",
        "@com_android_support_support_vector_drawable//aar",
    ],
)

android_binary(
    name = "demo",
    custom_package = "com.qualcomm.qti.snpe.imageclassifiers",
    manifest = "AndroidManifest.xml",
    manifest_values = {
        "applicationId": "com.qualcomm.qti.snpe.imageclassifiers",
    },
    multidex = "native",
    resource_files = glob(["res/**"]),
    deps = [
        ":demo_lib",
    ],
)
