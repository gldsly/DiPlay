package com.shilapi.xcertplay.hud

/**
 * Application ids DiPlay installs under.
 *
 * The BYD integrations only act on this app's own builds - a stock app or another project's HUD test
 * must not borrow the cluster - so they compare the running package name. Every comparison has to
 * agree with `applicationIdSuffix` in mobile/build.gradle.kts, which is why they read this object
 * instead of repeating the literal: a mismatch disables those integrations without a build error.
 */
object DiPlayPackages {
    /** Released application id. */
    const val OFFICIAL = "com.shihab.diplay"

    /** Suffix the car-test (debug) variant appends to [OFFICIAL]. Keep in step with Gradle. */
    const val CAR_TEST_SUFFIX = ".sealion_05_dmi"

    /** Car-test build, installed alongside the released app on this head unit. */
    const val CAR_TEST = OFFICIAL + CAR_TEST_SUFFIX

    /** The other CarPlay project, whose car-test build runs the same BYD HUD experiments. */
    const val HEADUNIT_REVIVED = "com.andrerinas.headunitrevived"
    const val HEADUNIT_REVIVED_CAR_TEST = "com.andrerinas.headunitrevived.bydhudtest"

    /** True for a car-test build of either project; released builds are excluded on purpose. */
    fun isCarTest(packageName: String): Boolean =
        packageName == CAR_TEST || packageName == HEADUNIT_REVIVED_CAR_TEST

    /** True for the builds allowed to drive the stock cluster receiver. */
    fun isHudCapable(packageName: String): Boolean =
        packageName == OFFICIAL || packageName == HEADUNIT_REVIVED || isCarTest(packageName)
}
