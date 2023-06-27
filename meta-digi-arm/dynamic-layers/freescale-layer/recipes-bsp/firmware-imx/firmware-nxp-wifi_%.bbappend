# Copyright (C) 2023 Digi International.

FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}:"

LIC_FILES_CHKSUM:ccimx93 = "file://EULA.txt;md5=add2d392714d3096ed7e0f7e2190724b"

SRCBRANCH:ccimx93 = "lf-6.1.1_1.0.0"
SRCREV:ccimx93 = "bacbeb4789c1b13d13aab12ada29217ce8c3e905"

# Use this temporal binary till it is released
SRC_URI:append:ccimx93 = " file://sduart_nw61x_v1.bin.se"

do_install:append:ccimx93() {
	install -m 444 ${WORKDIR}/sduart_nw61x_v1.bin.se ${D}${base_libdir}/firmware/nxp
}

PACKAGES:prepend:ccimx93 = "${PN}-nxpiw612 "

FILES:${PN}-nxpiw612 = " \
    ${base_libdir}/firmware/nxp/sduart_nw61x_v1.bin.se \
"
