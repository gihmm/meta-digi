# Copyright (C) 2017, Digi International Inc.

SUMMARY = "DEY Digi APIX examples"
SECTION = "examples"
LICENSE = "ISC"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/ISC;md5=f3b90e78ea0cffb20bf5cca7947a896d"

DEPENDS = "libdigiapix"

SRCBRANCH = "dey-2.2/maint"
SRCREV = "9cf9701082d7e99f1c200c5e08d6189c023579e8"

LIBDIGIAPIX_STASH = "${DIGI_MTK_GIT}/dey/dey-examples.git;protocol=ssh"
LIBDIGIAPIX_GITHUB = "${DIGI_GITHUB_GIT}/dey-examples.git;protocol=git"

LIBDIGIAPIX_GIT_URI ?= "${@base_conditional('DIGI_INTERNAL_GIT', '1' , '${LIBDIGIAPIX_STASH}', '${LIBDIGIAPIX_GITHUB}', d)}"

SRC_URI = "${LIBDIGIAPIX_GIT_URI};nobranch=1"

S = "${WORKDIR}/git"

inherit pkgconfig

EXTRA_OEMAKE += "-f libdigiapix-examples.mk"

do_compile() {
	oe_runmake
}

do_install() {
	oe_runmake DESTDIR=${D} install
}

COMPATIBLE_MACHINE = "(ccimx6$|ccimx6ul)"
