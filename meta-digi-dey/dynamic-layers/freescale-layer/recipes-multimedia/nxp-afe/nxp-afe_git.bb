# Copyright 2021 NXP

DESCRIPTION = "NXP Audio Front End (AFE) for incorporating Voice Assistants"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=7bdef19938f3503cfc4c586461f99012"

PV = "1.0+git${SRCPV}" 

SRCBRANCH = "MM_04.06.03_2110_L5.10.y"
NXPAFE_SRC ?= "git://github.com/nxp-imx/nxp-afe.git;protocol=https"
SRC_URI = " \
    ${NXPAFE_SRC};branch=${SRCBRANCH} \
"

SRCREV = "7d3295d250fe34325123cc9df94902a083308a46"

S = "${WORKDIR}/git"

DEPENDS += "alsa-lib"

TARGET_CC_ARCH += "${LDFLAGS}"

do_compile() {
        oe_runmake clean
        oe_runmake all
}

do_install() {
        install -d ${D}${libdir}/nxp-afe
        install -d ${D}/unit_tests/nxp-afe
        install -m 0644 ${WORKDIR}/deploy_afe/*.so.1.0 ${D}${libdir}/nxp-afe
        ln -sf -r ${D}${libdir}/nxp-afe/libdummyimpl.so.1.0 ${D}${libdir}/nxp-afe/libdummyimpl.so
        install -m 0755 ${WORKDIR}/deploy_afe/afe ${D}/unit_tests/nxp-afe
        install -m 0644 ${WORKDIR}/deploy_afe/asound.conf*    ${D}/unit_tests/nxp-afe
        install -m 0644 ${WORKDIR}/deploy_afe/TODO.md    ${D}/unit_tests/nxp-afe
}

FILES:${PN} += "/unit_tests"
INSANE_SKIP:${PN} += "dev-so"
